package com.railway.meetro;

import java.util.Calendar;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.railway.bean.DateTimeBean;
import com.railway.bean.StationApiBean;
import com.railway.helper.MeetroDbOpenHelper;
import com.railway.meetro.R;
import com.railway.utility.CommonConfig;
import com.railway.utility.CommonMethod;

public class MakeRoomActivity extends ActionBarActivity {
	private final static String TAG = "MakeRoomActivity";

	// 路線、駅設定関連
	ArrayAdapter<String> adapterRailway;
	ArrayAdapter<String> adapterStartSt;
	ArrayAdapter<String> adapterdestSt;
	Spinner spinnerStartSt; // 乗車駅プルダウン
	Spinner spinnerDestSt;  // 下車駅プルダウン
	String[] railwayIds = new String[9]; // 路線名

	// 時間設定関連
	static final int DATE_DIALOG_ID = 0;
	static final int TIME_DIALOG_ID = 1;
	private TextView mDateDisplay; 
	private TextView mTimeDisplay;
	private int mYear;  // Ex) 2014
	private int mMonth; // Ex) 10
	private int mDate;  // Ex) 23
	private int mHour;  // Ex) 21
	private int mMinute;// Ex) 20

	// 検索関連
	private String railwayId; // Ex) odpt.Railway:TokyoMetro.Ginza
	private String startStId; // Ex) odpt:Station.TokyoMetro.Shibuya
	private String destStId;  // Ex) odpt:Station.TokyoMetro.Asakusa
	private String startStTitle; // Ex) 渋谷
	private String destStTitle;  // Ex) 浅草
	private String startStCode;  // Ex) G01
	private String destStCode;   // Ex) G19
	private RadioButton radioButton; // 出発 or 到着

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.make_room);

		// Adapterの作成
		adapterRailway = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item);
		adapterRailway.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		adapterStartSt = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item);
		adapterStartSt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		adapterdestSt = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item);
		adapterdestSt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// 路線情報の取得
		MeetroDbOpenHelper meetroHelper = new MeetroDbOpenHelper(this);
		SQLiteDatabase railwayDb = meetroHelper.getReadableDatabase();
		int i = 0;
		Cursor c;
		c = railwayDb.rawQuery("SELECT * FROM RAILWAY ORDER BY _ID", null);
		if (c != null) {
			while(c.moveToNext()) {
				String railwayName = c.getString(c.getColumnIndex("RAILWAY_NAME"));
				railwayIds[i] = c.getString(c.getColumnIndex("RAILWAY_ID"));
				adapterRailway.add(railwayName);
				i++;
			};
		}

		// SpinnerにAdapterをセット
		Spinner spinnerRailway = (Spinner) findViewById(R.id.spinnerRailway);
		spinnerRailway.setAdapter(adapterRailway);
		spinnerRailway.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String item = (String) parent.getSelectedItem();
				int itemId = (int) parent.getSelectedItemId();
				railwayId = railwayIds[itemId];
				Bundle bundle = new Bundle();
				bundle.putString("format", "json");
				bundle.putString("rdftype", "odpt:Station");
				bundle.putString("odptParam", railwayIds[itemId]);
				getSupportLoaderManager().initLoader(0, bundle, callbacks);
				Toast.makeText(MakeRoomActivity.this, "路線:" + item + "が選択されました。", Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}
		});

		// 時間区分関連
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioTimeType);
		radioButton = (RadioButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
		//        RadioButton radioButton = (RadioButton) findViewById(radioGroup.getCheckedRadioButtonId());
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			// ラジオグループのチェック状態が変更された時に呼び出されます
			// チェック状態が変更されたラジオボタンのIDが渡されます
			public void onCheckedChanged(RadioGroup group, int checkedId) { 
				radioButton = (RadioButton) findViewById(checkedId);
				Toast.makeText(MakeRoomActivity.this,
                        "onCheckedChanged():" + radioButton.getText(),
                        Toast.LENGTH_SHORT).show();
			}
		});

		// 時間設定関連
		mDateDisplay = (TextView) findViewById(R.id.dateDisplay);
		mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);
		// add a click listener to the button
		mDateDisplay.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});
		mTimeDisplay.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
			}
		});
		// get the current time
		final Calendar calendar = Calendar.getInstance();
		mYear = calendar.get(Calendar.YEAR);
		mMonth = calendar.get(Calendar.MONTH);
		mDate = calendar.get(Calendar.DAY_OF_MONTH);
		mHour = calendar.get(Calendar.HOUR_OF_DAY);
		mMinute = calendar.get(Calendar.MINUTE);
		// display the current date and time
		CommonMethod.updateDisplay(mDateDisplay, mYear, mMonth, mDate);
		CommonMethod.updateDisplay(mTimeDisplay, mHour, mMinute);

		// 検索ボタン関連
		Button trainSearch = (Button) findViewById(R.id.buttonTrainSearch);
		trainSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 検索ボタンがクリックされた時に呼び出されます
				if(startStCode.equals(destStCode)) {
					Toast.makeText(MakeRoomActivity.this, "乗車駅と目的駅が同じだよ！", Toast.LENGTH_SHORT).show();
					return;
				}
				// 乗車駅、下車駅、日時をBeanにぶっこむ
				StationApiBean startStDecide = new StationApiBean();
				startStDecide.setSameAs(startStId);
				startStDecide.setTitle(startStTitle);
				startStDecide.setStationCode(startStCode);
				StationApiBean destStaDecide = new StationApiBean();
				destStaDecide.setSameAs(destStId);
				destStaDecide.setTitle(destStTitle);
				destStaDecide.setStationCode(destStCode);
				DateTimeBean dateTimeDecide = new DateTimeBean();
				dateTimeDecide.setYear(mYear);
				dateTimeDecide.setMonth(mMonth + 1);
				dateTimeDecide.setDate(mDate);
				dateTimeDecide.setHour(mHour);
				dateTimeDecide.setMinute(mMinute);
				final Intent intent = new Intent(MakeRoomActivity.this, SelectTrainActivity.class);
				// 次のActivityにBeanごと渡す
				intent.putExtra("railwayIdDecide", railwayId); // Ex) odpt.Railway:TokyoMetro.Ginza 
				intent.putExtra("startStDecide", startStDecide); // StationBean
				intent.putExtra("destStaDecide", destStaDecide); // StationBean
				// 時間区分は[出発][到着]のテキストをそのまま渡す
				intent.putExtra("timeTypeDecide", radioButton.getText()); // 出発 or 到着
				intent.putExtra("dateTimeDecide", dateTimeDecide); // DateTimeBean
				startActivity(intent);
			}
		});
	}

	// the callback received when the user "sets" the dae or time in the dialog
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this,
					mDateSetListener, mYear, mMonth, mDate);
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this,
					mTimeSetListener, mHour, mMinute, false);
		}
		return null;
	}
	// updates the date we display in the TextView
	private final DatePickerDialog.OnDateSetListener mDateSetListener = 
			new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDate = dayOfMonth;
			CommonMethod.updateDisplay(mDateDisplay, year, monthOfYear, dayOfMonth);
		}
	};
	// updates the time we display in the TextView
	private final TimePickerDialog.OnTimeSetListener mTimeSetListener =
			new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			CommonMethod.updateDisplay(mTimeDisplay, hourOfDay, minute);
		}
	};

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
			// 丸ノ内線の分岐線対応
			// 分岐線は3駅だが、中野坂上が両方の路線に存在するので-4する
			if(!value[0].getStationCode().substring(0, 1).equals("M")) {
				v_count = value.length;
			} else {
				v_count = value.length - 4;
			}
			final StationApiBean[] stations = new StationApiBean[v_count];
			System.out.println("v_count: " + v_count);
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
			adapterStartSt.clear(); // 前回の値をリセット！
			for(StationApiBean staSort: stations) {
				adapterStartSt.add(staSort.getTitle());
			}
			// Adapterの作成
			spinnerStartSt = (Spinner) findViewById(R.id.spinnerStartSt);
			spinnerStartSt.setAdapter(adapterStartSt);
			// Spinnerの選択イベントを取得
			spinnerStartSt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					int itemId = (int) parent.getSelectedItemId();
					startStId = stations[itemId].getSameAs();
					startStTitle = stations[itemId].getTitle();
					startStCode = stations[itemId].getStationCode();
				}
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
				}
			});		

			// 下車駅
			adapterdestSt.clear(); // 前回の値をリセット！
			for(StationApiBean endSort: stations) {
				adapterdestSt.add(endSort.getTitle());
			};
			// Adapterの作成
			spinnerDestSt = (Spinner) findViewById(R.id.spinnerDestSt);
			spinnerDestSt.setAdapter(adapterStartSt);
			// Spinnerの選択イベントを取得
			spinnerDestSt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					String item = (String) parent.getSelectedItem();
					int itemId = (int) parent.getSelectedItemId();
					destStId = stations[itemId].getSameAs();
					destStTitle = stations[itemId].getTitle();
					destStCode = stations[itemId].getStationCode();
				}
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
				}
			});		
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
}
