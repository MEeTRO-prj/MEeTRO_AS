package com.railway.meetro;

import com.railway.bean.DateTimeBean;
import com.railway.bean.StationApiBean;
import com.railway.controller.SearchTimetableCallback;
import com.railway.controller.SearchTimetable;
import com.railway.helper.MeetroDbOpenHelper;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/*
 * 路線と乗車駅を選択し、検索を行う画面
 */
public class SelectTrainActivity extends ActionBarActivity implements SearchTimetableCallback {
	private final static String TAG = "SelectTrainActivity";

	private StationApiBean startSt;
	private StationApiBean destSt;
	private DateTimeBean dateTime;
	private String timeType;
	private int mYear;
	private String mMonth;
	private String mDate;
	private String mHour;
	private String mMinute;
	private String direction; // Ex) odpt.RailDirection:TokyoMetro.Asakusa
	private String railwayId; // Ex) odpt.Railway:TokyoMetro.Ginza 
	private String startStId; // Ex) odpt:Station.TokyoMetro.Shibuya
	private String destStId;   // Ex) odpt:Station.TokyoMetro.Asakusa
	private String startStCodeNum; // Ex) 1 
	private String destStCodeNum;   // Ex) 19
	String endStTitle;
	String decideTime;
	String trainTypeTitle;

	private SearchTimetable searchTimetable;
	private TextView viewRailwayDecide;
	private TextView viewStartStDecide;
	private TextView viewEndStDecide;
	private TextView dateDisplay;
	private TextView timeDisplay;
	private TextView timeTypeDisplay;
	private ListView viewTimetable;
	
	ArrayAdapter<Integer> adapterCarNum;
	int decideCarNum = 1;
	Button decideBtn;

	// DB
	MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_train);

		viewRailwayDecide = (TextView)findViewById(R.id.viewRailwayDecide);
		viewStartStDecide = (TextView)findViewById(R.id.viewStartStDecide);
		viewEndStDecide = (TextView)findViewById(R.id.viewEndStDecide);
		dateDisplay = (TextView)findViewById(R.id.dateDisplay);
		timeDisplay = (TextView)findViewById(R.id.timeDisplay);
		timeTypeDisplay = (TextView)findViewById(R.id.timeTypeDisplay);
		viewTimetable = (ListView)findViewById(R.id.listTimetable);

		// intentから指定キーのカスタムクラスを取得する
		railwayId = getIntent().getStringExtra("railwayIdDecide");
		startSt = (StationApiBean) getIntent().getSerializableExtra("startStDecide");
		destSt = (StationApiBean) getIntent().getSerializableExtra("destStaDecide");
		dateTime = (DateTimeBean) getIntent().getSerializableExtra("dateTimeDecide");
		timeType = getIntent().getStringExtra("timeTypeDecide"); // 出発 or 到着

		startStId = startSt.getSameAs();
		destStId = destSt.getSameAs();
		startStCodeNum = startSt.getStationCode().substring(1, 3);
		destStCodeNum = destSt.getStationCode().substring(1, 3);
		mYear = dateTime.getYear();
		mMonth = dateTime.getMonth();
		mDate = dateTime.getDate();
		mHour = dateTime.getHour();
		mMinute = dateTime.getMinute();
		System.out.println(railwayId + ":" +
				startStCodeNum + ":" + startStId + "/" +
				destStCodeNum + ":" + destStId + 
				timeType + ":" +
				mYear + "/" + mMonth + "/"+ mDate + " " + mHour + ":" + mMinute);

		SQLiteDatabase userDbR = userHelper.getReadableDatabase();
		Cursor c = null;
		c = userDbR.rawQuery("SELECT DIRECTION_1, DIRECTION_2, CAR_NUM FROM RAILWAY WHERE RAILWAY_ID = ?;", new String[]{railwayId});			
		String direction_1 = null;
		String direction_2 = null;
		int car_num_max = 1;
		while(c.moveToNext()) {
			direction_1 = c.getString(c.getColumnIndex("DIRECTION_1"));
			direction_2 = c.getString(c.getColumnIndex("DIRECTION_2"));
			car_num_max = c.getInt(c.getColumnIndex("CAR_NUM"));
		}
		c.close();

		int directionNum = Integer.parseInt(startStCodeNum) - Integer.parseInt(destStCodeNum);
		if(directionNum > 0) {
			System.out.println(direction_1 + "方面へ行きます");
			direction = direction_1;
		} else if (directionNum < 0) {
			System.out.println(direction_2 + "方面へ行きます");
			direction = direction_2;
		}

		// SearchTimetableのコンストラクタ呼び出し
		searchTimetable = new SearchTimetable(this, 
				viewRailwayDecide, viewStartStDecide, viewEndStDecide, 
				dateDisplay, timeDisplay, timeTypeDisplay, SelectTrainActivity.this);
		// SearchTimetableの実行（検索！）
		searchTimetable.execute(
				railwayId, direction, startStId, destStId, 
				String.valueOf(mYear), String.valueOf(mMonth), String.valueOf(mDate), 
				String.valueOf(mHour), String.valueOf(mMinute), timeType
				);

		// 車両番号選択
		// SpinnerにAdapterをセット
		Spinner spinnerCarNum = (Spinner) findViewById(R.id.spinnerCarNum);
		adapterCarNum = new ArrayAdapter<Integer>(
				this, android.R.layout.simple_spinner_item);
		adapterCarNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for(int i = 1; i <= car_num_max; i++) {
			adapterCarNum.add(i);			
		}
		spinnerCarNum.setAdapter(adapterCarNum);
		spinnerCarNum.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				decideCarNum = (Integer) parent.getSelectedItem();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
		});

		decideBtn = (Button) findViewById(R.id.buttonDecideTime);
		// 時刻表のアイテムがタップされた時に呼び出されるコールバックリスナーを登録
		// 直接タッチしたり、トラックボールなどを押し込んだときに発生
		viewTimetable.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView) parent;
				view.setSelected(true);
				// クリックされたアイテムを取得
				String timeDestTrainType = (String) listView.getItemAtPosition(position);       // Ex) 23:15 浅草行き 急行
				decideTime = timeDestTrainType.substring(0, 5);                                 // Ex) 23:15
				int indexOfDestSt = timeDestTrainType.indexOf(" : ");
				int indexOfTrainType = timeDestTrainType.indexOf("行き");
				endStTitle = timeDestTrainType.substring(indexOfDestSt + 3, indexOfTrainType); // Ex) 浅草
				trainTypeTitle = timeDestTrainType.substring(indexOfTrainType + 3);             // Ex) 急行
				System.out.println(decideTime + " " + endStTitle + " " + trainTypeTitle);
				decideBtn.setEnabled(true);
			}
		});

		decideBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(SelectTrainActivity.this, CompleteActivity.class);
				// 次のActivityに渡す
				intent.putExtra("railwayId", railwayId); // Ex) odpt:Railway.TokyoMetro.Ginza
				intent.putExtra("railway", viewRailwayDecide.getText());         // Ex) 銀座線
				intent.putExtra("startSt", startSt);                             // StationBean
				intent.putExtra("destSt", destSt);                               // StationBean
				intent.putExtra("endStTitle", endStTitle);                       // Ex) 浅草
				intent.putExtra("trainTypeTitle", trainTypeTitle);               // Ex) 急行
				intent.putExtra("rideDate", mYear + "-" + mMonth + "-" + mDate); // Ex) 2014-10-23
				intent.putExtra("decideTime", decideTime);                       // Ex) 23:15
				if(timeType.equals("出発")) {
					intent.putExtra("timeType", 0);                              // 出発 -> 0
				} else {
					intent.putExtra("timeType", 1);                              // 到着 -> 1
				}
				intent.putExtra("carNum", decideCarNum);                         // 3
				intent.putExtra("direction", direction);                         // Ex) odpt.RailDirection:TokyoMetro.Asakusa
				startActivity(intent);				
			}
		});
	}
	// SearchTimetableCallback のオーバーライド
	@Override
	public void onPostExecute(ArrayAdapter<String> adapterTime) {
		viewTimetable.setAdapter(adapterTime);
	}
}
