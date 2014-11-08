package com.railway.meetro;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.railway.bean.RoomBean;
import com.railway.bean.StationApiBean;
import com.railway.controller.JoinRoom;
import com.railway.controller.SearchRoom;
import com.railway.controller.SearchRoomCallback;
import com.railway.helper.CheckGcmHelper;
import com.railway.helper.MeetroDbOpenHelper;
import com.railway.meetro.R;
import com.railway.utility.CommonConfig;
import com.railway.utility.CommonMethod;

/*
 * 自分が乗車する駅を選択する画面
 * （他者が作成した部屋に招待された場合のみ）
 */
public class SelectStationActivity extends ActionBarActivity implements SearchRoomCallback {
	String TAG = "SelectStationActivity";

	// ユーザ情報の変数
	SharedPreferences sp;
	public int userId;
	public String regid = "";
	public String userName;
	String roomId;

	ArrayAdapter<String> adapterRideSt;
	SearchRoom searchRoom;
	Spinner spinnerRideSt; // 乗車駅プルダウン
	Button btnSaveName;

	String railwayId;
	String rideStId;    // Ex) odpt:Station.TokyoMetro.Shibuya
	String rideStTitle; // Ex) 渋谷
	String rideStCode;  // Ex) G01

	HashMap<String, String> roomInfo;
	JoinRoom joinRoom;
	RoomBean room;

	// DB
	MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(this);
	AsyncTask<Void, Void, String> informTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_station);
		System.out.println("Here is SelectStationActivity.");

		sp = PreferenceManager.getDefaultSharedPreferences(SelectStationActivity.this);
		userId = sp.getInt("userId", 0);
		userName = sp.getString("userName", null);
		Log.d(TAG, "userId: " + userId + " userName: " + userName);

		adapterRideSt = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item);
		adapterRideSt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// パラメータ: room_id
		roomId = getIntent().getStringExtra("roomId");
		Log.d(TAG, "roomId: " + roomId);
		// room_idで部屋情報を検索(Server -> DB.ROOM)
		// SearchRoomのコンストラクタ呼び出し
		searchRoom = new SearchRoom(this, SelectStationActivity.this);
		// SearchRoomの実行（検索！）
		searchRoom.execute(roomId);

		// 乗る駅を決めるボタン押下でServer -> DB.MEMBERに登録
		btnSaveName = (Button) findViewById(R.id.buttonDecideStation);
		btnSaveName.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("trainNumber" + roomInfo.get("trainNumber"));
				// JoinRoomのコンストラクタ呼び出し
				joinRoom = new JoinRoom(SelectStationActivity.this, roomInfo, SelectStationActivity.this);
				// JoinRoomの実行（検索！）
				joinRoom.execute(roomId, String.valueOf(userId), rideStId);

				// GCM通知でHttpConnectをするため非同期処理
				informTask = new AsyncTask<Void, Void, String>() {
					@Override
					protected String doInBackground(Void... params) {
						// GCMでROOMのOWNERに参加を通知する
						System.out.println("roomId: " + roomId + "roomOwnerId: " + roomInfo.get("roomOwnerId"));
						informRoomOwnerGCM(roomId, roomInfo.get("roomOwnerId"), userName);
						return null;
					}
					@Override
					protected void onPostExecute(String result) {
						informTask = null;
					}
				};
				informTask.execute(null, null, null);
			}
		});
	}

	@Override
	public void onPostExecute(RoomBean result) {
		// Ex) 2014-10-21 -> 20141021
		room = result;
		railwayId = room.getRailwayId();
		String roomRideDate = room.getRideDate().replace("-", "");
		// Ex) 20141021 -> 2014
		String mYear = roomRideDate.substring(0, 4);
		// Ex) 20141021 -> 10
		String mMonth = roomRideDate.substring(4, 6);
		// Ex) 20141021 -> 21
		String mDate = roomRideDate.substring(6, 8);
		// 曜日判定
		Calendar calen = new GregorianCalendar(
				Integer.parseInt(mYear),
				Integer.parseInt(mMonth) - 1,
				Integer.parseInt(mDate)
		);
		String day_of_week;
		switch(calen.get(Calendar.DAY_OF_WEEK)) {
			case Calendar.SUNDAY: day_of_week = "odpt:holidays"; break;
			case Calendar.SATURDAY: day_of_week = "odpt:saturdays"; break;
			default: day_of_week = "odpt:weekdays";
		}
		System.out.println(day_of_week);

		// 曜日判定
		String day_of_weekJpn = CommonMethod.changeDayOfWeek(Integer.parseInt(mYear), Integer.parseInt(mMonth), Integer.parseInt(mDate));
		String roomNumber =  roomRideDate + room.getRoomId();
		String roomRideDateJpn = mYear + "年" + mMonth + "月" + mDate + "日" + day_of_weekJpn;
		// 時間の秒数削除
		// Ex) 19:38:00 -> 19:38
		String roomRideTime = room.getRideTime().substring(0, 5);
		// 時間区分: 0->発, 1->着
		String roomTimeTypeJpn = "";
		switch(room.getTimeType()) {
			case 0: roomTimeTypeJpn = "発";break;
			case 1: roomTimeTypeJpn = "着";break;
		}

		// 路線名の日本語変換用DBデータ取得
		MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(SelectStationActivity.this);
		SQLiteDatabase userDbR = userHelper.getReadableDatabase();
		Cursor c = null;
		c = userDbR.rawQuery("SELECT RAILWAY_NAME FROM RAILWAY WHERE RAILWAY_ID = ?;", new String[]{railwayId});
		String railway_name = null;
		while(c.moveToNext()) {
			railway_name = c.getString(c.getColumnIndex("RAILWAY_NAME"));
		}
		c.close();

		// 取得した部屋情報をHasMapにぶち込んでおく
		roomInfo = new HashMap<String, String>();
		roomInfo.put("roomNumber", roomNumber);                         // Ex) 20141023102
		roomInfo.put("roomDate", roomRideDateJpn);                      // Ex) 2014年10月23日(木)
		roomInfo.put("roomTime", roomRideTime);                         // Ex) 22:48
		roomInfo.put("roomTimeType", roomTimeTypeJpn);                  // Ex) 着
		roomInfo.put("roomRailway", railway_name);                      // Ex) 銀座線
		roomInfo.put("roomRailwayId", railwayId);                       // Ex) odpt.Railway:TokyoMetro.Hanzomon
		roomInfo.put("roomDayOfWeek", day_of_week);                     // Ex) odpt:holidays
		roomInfo.put("roomStartSt", room.getRideSt());                  // Ex) 表参道
		roomInfo.put("roomDestSt", room.getDestSt());                   // Ex) 渋谷
		roomInfo.put("endStTitle", room.getEndSt());                    // Ex) 浅草
		roomInfo.put("trainTypeTitle", room.getTrainType());            // Ex) 急行
		roomInfo.put("roomCarNum", String.valueOf(room.getCarNum()));   // Ex) 3
		roomInfo.put("trainNumber", room.getTrainNumber());             // Ex) A1104
		roomInfo.put("roomOwnerId", String.valueOf(room.getOwnerId())); // Ex) 104

		// 取得した部屋情報の路線(railway)を使い、駅情報を取得しスピナーにぶち込む
		Bundle bundle = new Bundle();
		bundle.putString("format", "json");
		bundle.putString("rdftype", "odpt:Station");
		bundle.putString("odptParam", room.getRailwayId());
		getSupportLoaderManager().initLoader(0, bundle, callbacks);

	}

	// 駅情報を非同期で取得するLoaderCallbacks
	private LoaderCallbacks<StationApiBean[]> callbacks = new LoaderCallbacks<StationApiBean[]>() {
		@Override
		public Loader<StationApiBean[]> onCreateLoader(int id, Bundle bundle) {
			CustomLoader loader = new CustomLoader(getApplicationContext(), bundle);
			loader.forceLoad();
			return loader;
		}
		@Override
		public void onLoadFinished(Loader<StationApiBean[]> loader, StationApiBean[] value) {
			int v_count = 0;
			Log.d(TAG, "room.getRideSt(): " + room.getRideSt());
			Log.d(TAG, "room.getDestSt(): " + room.getDestSt());

			// 丸ノ内線の分岐線対応
			// 分岐線は3駅だが、中野坂上が両方の路線に存在するので-4する
			if(!value[0].getStationCode().substring(0, 1).equals("M")) {
				v_count = value.length;
			} else {
				v_count = value.length - 4;
			}
			final StationApiBean[] stations = new StationApiBean[v_count];
			for(StationApiBean sta: value) {
				String stCodeAlp = sta.getStationCode().substring(0, 1);
				int stCodeNum = Integer.parseInt(sta.getStationCode().substring(1, 3));
				if(stCodeAlp.equals("m")) {
					continue;
				}
				stations[stCodeNum - 1] = new StationApiBean();
				stations[stCodeNum - 1].setSameAs(sta.getSameAs());
				stations[stCodeNum - 1].setTitle(sta.getTitle());
				stations[stCodeNum - 1].setStationCode(sta.getStationCode());
			};
			// 乗車駅
			adapterRideSt.clear(); // 前回の値をリセット！
			int x = 0, y = 0;
			final ArrayList<StationApiBean> availStations = new ArrayList<StationApiBean>();
			// 乗車可能区間のみの駅を表示させる
			for(StationApiBean staSort: stations) {
				if (x == 0) {
					if (room.getRideSt().equals(staSort.getTitle()) || room.getDestSt().equals(staSort.getTitle())) {
						x++;
					}
				} else {
					if (room.getRideSt().equals(staSort.getTitle()) || room.getDestSt().equals(staSort.getTitle())) {
						break;
					}
					Log.d(TAG, "staSort.getTitle()" + staSort.getTitle());
					availStations.add(y, staSort);
					adapterRideSt.add(staSort.getTitle());
					y++;
				}
			}
			// Adapterの作成
			spinnerRideSt = (Spinner) findViewById(R.id.spinnerRideSt);
			spinnerRideSt.setAdapter(adapterRideSt);
			if (availStations.size() == 0 ) {
				adapterRideSt.add("選択可能な駅がありません。");
				spinnerRideSt.setEnabled(false);
				btnSaveName.setEnabled(false);
			} else {
				// Spinnerの選択イベントを取得
				spinnerRideSt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						int itemId = (int) parent.getSelectedItemId();
						rideStId = availStations.get(itemId).getSameAs();
						rideStTitle = availStations.get(itemId).getTitle();
						rideStCode = availStations.get(itemId).getStationCode();
						Log.d(TAG, "availStations.get(itemId).getTitle()" + availStations.get(itemId).getTitle());
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						// TODO Auto-generated method stub
					}
				});
			}
			getSupportLoaderManager().destroyLoader(loader.getId());
		}
		@Override
		public void onLoaderReset(Loader<StationApiBean[]> loader) {
		}
	};

	private static class CustomLoader extends AsyncTaskLoader<StationApiBean[]> {
		private String mFormat;
		private String rdftype;
		private String odptParam;
		public CustomLoader(Context context, Bundle bundle) {
			super(context);
			mFormat = bundle.getString("format");
			rdftype = bundle.getString("rdftype");
			odptParam = bundle.getString("odptParam");
			System.out.println("rdftype: " + rdftype + "odptParam: " + odptParam);
		}
		@Override
		public StationApiBean[] loadInBackground() {
			RestTemplate template = new RestTemplate();
			template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
			String url = CommonConfig.getAPI_URL() + "?rdf:type=" + rdftype + "&odpt:railway=" + odptParam + "&acl:consumerKey=" + CommonConfig.getACCESS_KEY();
			System.out.println(url);
			try {
				ResponseEntity<StationApiBean[]> responseEntity = template.exchange(url, HttpMethod.GET, null, StationApiBean[].class);
				return responseEntity.getBody();
			} catch (Exception e) {
				Log.d("Error", e.toString());
				return null;
			}
		}
	}

	@Override
	public void onPostExecute(String[] result) {
		System.out.println("onPostExecute - Result2" + result[0] + result[1]);
		roomInfo.put("roomStartSt", result[0]); // Ex) 渋谷
		roomInfo.put("roomTime", result[1]);    // Ex) 22:48

		// RoomTopへ遷移
		Intent intent = new Intent(SelectStationActivity.this, RoomTopActivity.class);
		intent.putExtra("roomInfo", roomInfo);
		startActivity(intent);
		// IntroActivityを終了する
		SelectStationActivity.this.finish();
	}

	// 部屋に入ったことを部屋作成者に通知する
	public boolean informRoomOwnerGCM(String roomId, String ownerId, String userName) {
		String serverUrl = CommonConfig.getSERVER_URL() + "GCM_join.php";
		System.out.println("serverUrl: " + serverUrl);
		Map<String, String> params = new HashMap<String, String>();
		params.put("ROOM_ID", roomId);
		params.put("OWNER_ID", ownerId);
		params.put("USER_NAME", userName);
		try {
			CheckGcmHelper cgh = new CheckGcmHelper(SelectStationActivity.this, SelectStationActivity.this);
			cgh.post(serverUrl, params);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
