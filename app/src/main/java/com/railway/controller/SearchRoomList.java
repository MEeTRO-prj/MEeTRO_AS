package com.railway.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.railway.bean.RoomBean;
import com.railway.helper.MeetroDbOpenHelper;
import com.railway.meetro.R;
import com.railway.utility.CommonConfig;
import com.railway.utility.CommonMethod;
import com.railway.utility.ServiceHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchRoomList extends AsyncTask<String, Void, List<RoomBean>> {
	private SearchRoomListCallback callbackListener = null;

	private String PHP_PATH = "room_list.php";
	private Activity activity;
	ProgressDialog pDialog;
	CustomAdapter adapterRoom;

	// コンストラクタ
	public SearchRoomList(Activity activity, SearchRoomListCallback listener) {
		this.activity = activity;
		this.callbackListener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pDialog = new ProgressDialog(activity);
		pDialog.setMessage("Searching roomlist..");
		pDialog.setCancelable(false);
		pDialog.show();
	}

	@Override
	protected List<RoomBean> doInBackground(String... arg) {
		String userId = arg[0];

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("USER_ID", userId));

		ServiceHandler serviceClient = new ServiceHandler();
		// 部屋リストのJSON
		String jsRoomList = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + PHP_PATH, ServiceHandler.POST, params);
		Log.d("Create Response: ", "> " + jsRoomList);

		// 駅名の日本語変換用JSON
		String jsToTitleMetro = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + "metro_stationDict.json", ServiceHandler.GET, null);

		// DB
		MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(activity);

		List<RoomBean> roomList = new ArrayList<RoomBean>();
		try {
			// 部屋リストのJSONをJSONObject->JSONArrayに格納
			JSONObject jsRoot = new JSONObject(jsRoomList);
			JSONArray jsArray = jsRoot.getJSONArray("room");
			// System.out.println("jsRoot.length(): " + jsRoot.length());
			// System.out.println("jsArray.length(): " + jsArray.length());

			// 駅名の日本語変換用JSONをJSONObjectに格納
			JSONObject jsObjToTitleMetro = new JSONObject(jsToTitleMetro);

			// 路線名の日本語変換用DBデータ取得
			SQLiteDatabase userDbR = userHelper.getReadableDatabase();

			for (int i = 0; i < jsArray.length(); i++) {
				Cursor c = null;
				JSONObject jsOneRecord = jsArray.getJSONObject(i);
				RoomBean rlb = new RoomBean();
				rlb.setRoomId(jsOneRecord.getInt("room_id"));
				rlb.setOwnerId(jsOneRecord.getInt("owner_id"));
				rlb.setRideDate(jsOneRecord.getString("ride_date"));
				rlb.setRideTime(jsOneRecord.getString("ride_time"));
				rlb.setTimeType(jsOneRecord.getInt("time_type"));

				String railwayId = jsOneRecord.getString("railway_id");
				// DBからrailway_idと一致する路線名を取得
				// EX) odpt.Railway:TokyoMetro.Ginza -> 銀座線
				c = userDbR.rawQuery("SELECT RAILWAY_NAME FROM RAILWAY WHERE RAILWAY_ID = ?;", new String[]{railwayId});			
				String railway_name = null;
				while(c.moveToNext()) {
					railway_name = c.getString(c.getColumnIndex("RAILWAY_NAME"));
				}
				rlb.setRailwayId(railway_name);

				// odpt:Station.TokyoMetro.部分を切り取る
				// EX) odpt:Station.TokyoMetro.Shibuya -> Shibuya
				String rideStTitleEng = jsOneRecord.getString("ride_st").substring(railwayId.length() + 1);
				String destStTitleEng = jsOneRecord.getString("dest_st").substring(railwayId.length() + 1);
				// EX) Shibuya -> 渋谷
				String rideStTitleJpn = jsObjToTitleMetro.getString(rideStTitleEng);
				String destStTitleJpn = jsObjToTitleMetro.getString(destStTitleEng);
				rlb.setRideSt(rideStTitleJpn);
				rlb.setDestSt(destStTitleJpn);
				rlb.setEndSt(jsOneRecord.getString("end_st"));
				rlb.setTrainType(jsOneRecord.getString("train_type"));
				rlb.setCarNum(jsOneRecord.getInt("car_num"));
				rlb.setUseFlg(jsOneRecord.getInt("use_flg"));
				roomList.add(rlb);
				c.close();
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return roomList;
	}

	@Override
	protected void onPostExecute(List<RoomBean> result) {
		super.onPostExecute(result);
		adapterRoom = new CustomAdapter(activity, R.layout.room_list_row, result);
		
		// RoomListActivityでオーバーライドしているonPostExecuteメソッドを呼ぶ
		if (callbackListener != null) {
			callbackListener.onPostExecute(adapterRoom);
		}
		if (pDialog.isShowing())
			pDialog.dismiss();
	}

	public class CustomAdapter extends ArrayAdapter<RoomBean> {
		private LayoutInflater mLayoutInflater;
		private List<RoomBean> rlb;
		public CustomAdapter(Context context, int textViewResourceId, List<RoomBean> objects) {
			super(context, textViewResourceId, objects);
			rlb = objects;
			mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			RoomBean room = rlb.get(position);
			if (null == convertView) {
				convertView = mLayoutInflater.inflate(R.layout.room_list_row, null);
			}
			// Ex) 2014-10-21 -> 20141021
			String roomRideDate = room.getRideDate().replace("-", "");
			// Ex) 20141021 -> 2014
			String mYear = roomRideDate.substring(0, 4);
			// Ex) 20141021 -> 10
			String mMonth = roomRideDate.substring(4, 6);
			// Ex) 20141021 -> 21
			String mDate = roomRideDate.substring(6, 8);
			// 曜日判定
			String day_of_week = CommonMethod.changeDayOfWeek(Integer.parseInt(mYear), Integer.parseInt(mMonth), Integer.parseInt(mDate));
			String roomNumber =  roomRideDate + room.getRoomId();
			String roomRideDateJpn = mYear + "年" + mMonth + "月" + mDate + "日" + day_of_week;
			// 時間の秒数削除
			// Ex) 19:38:00 -> 19:38
			String roomRideTime = room.getRideTime().substring(0, 5);

			// 時間区分: 0->発, 1->着
			String roomTimeTypeJpn = "";
			switch(room.getTimeType()) {
			case 0: roomTimeTypeJpn = "発";break;
			case 1: roomTimeTypeJpn = "着";break;
			}
			((TextView) convertView.findViewById(R.id.roomNumber)).setText(roomNumber);
			((TextView) convertView.findViewById(R.id.roomDateTime)).setText(roomRideDateJpn + " " + roomRideTime + roomTimeTypeJpn);
			((TextView) convertView.findViewById(R.id.roomRailway)).setText(room.getRailwayId());
			((TextView) convertView.findViewById(R.id.roomStartSt)).setText(room.getRideSt());
			((TextView) convertView.findViewById(R.id.roomDestSt)).setText(room.getDestSt());
			return convertView;
		}
	}
}