package com.railway.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.railway.helper.MeetroDbOpenHelper;
import com.railway.meetro.R;
import com.railway.utility.CommonConfig;
import com.railway.utility.ServiceHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/*
 * 指定した条件での時刻表を検索する非同期処理
 */
public class SearchTimetable extends AsyncTask<String, Void, HashMap<String, String>> {
	private SearchTimetableCallback callbackListener = null;

	private Activity activity;
	ProgressDialog pDialog;

	private TextView viewRailwayDecide;
	private TextView viewStartStDecide;
	private TextView viewEndStDecide;
	private TextView dateDisplay;
	private TextView timeDisplay;
	private TextView timeTypeDisplay;

	CustomAdapter adapterTime;

	// コンストラクタ
	// SeachTimetableCallback引数有り
	public SearchTimetable(Activity activity, TextView railway, TextView startSt, TextView endSt, 
			TextView date, TextView time, TextView type, SearchTimetableCallback listener) {
		this.activity = activity;
		this.viewRailwayDecide = railway;
		this.viewStartStDecide = startSt;
		this.viewEndStDecide = endSt;
		this.dateDisplay = date;
		this.timeDisplay = time;
		this.timeTypeDisplay = type;
		this.callbackListener = listener;
	}

	// プログレスダイアログを生成・表示
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pDialog = new ProgressDialog(activity);
		pDialog.setMessage("Searching timetable..");
		pDialog.setCancelable(false);
		pDialog.show();
	}

	// 時刻表を検索する
	@Override
	protected HashMap<String, String> doInBackground(String... arg) {
		// DB
		MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(activity);
		String railwayId = arg[0]; // Ex) odpt.Railway:TokyoMetro.Ginza
		String direction = arg[1]; // Ex) odpt.RailDirection:TokyoMetro.Asakusa
		String startStId = arg[2]; // Ex) odpt:Station.TokyoMetro.Shibuya
		String endStId = arg[3];   // Ex) odpt:Station.TokyoMetro.Asakusa
		int mYear = Integer.parseInt(arg[4]);
		int mMonth = Integer.parseInt(arg[5]);
		int mDate = Integer.parseInt(arg[6]);
		int mHour = Integer.parseInt(arg[7]);
		int mMinute = Integer.parseInt(arg[8]);
		String timeType = arg[9];

		// APIに渡すパラメータをparamsに格納
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("rdf:type", "odpt:StationTimetable"));
		params.add(new BasicNameValuePair("odpt:station", startStId));
		params.add(new BasicNameValuePair("odpt:railDirection", direction));
		params.add(new BasicNameValuePair("acl:consumerKey", CommonConfig.getACCESS_KEY()));

		// StationTimetableAPIを叩く(レスポンス->json)
		ServiceHandler serviceClient = new ServiceHandler();
		String json = serviceClient.makeServiceCall(CommonConfig.getAPI_URL(), ServiceHandler.GET, params);
		
		HashMap<String, String> result = new HashMap<String, String>();
		result.put("json", json);
		result.put("railwayId", railwayId);
		result.put("startStId", startStId);
		result.put("endStId", endStId);
		result.put("mYear", arg[4]);
		result.put("mMonth", arg[5]);
		result.put("mDate", arg[6]);
		result.put("mHour", arg[7]);
		result.put("mMinute", arg[8]);
		result.put("timeType", timeType);

		// 端末DBから利用する路線名の日本語を取得
		// EX) odpt.Railway:TokyoMetro.Marunouchi -> 丸ノ内線
		SQLiteDatabase userDbR = userHelper.getReadableDatabase();
		Cursor c = null;
		c = userDbR.rawQuery("SELECT RAILWAY_NAME FROM RAILWAY WHERE RAILWAY_ID = ?;", new String[]{railwayId});			
		String railway_name = null;
		while(c.moveToNext()) {
			railway_name = c.getString(c.getColumnIndex("RAILWAY_NAME"));
		}
		result.put("railwayName", railway_name);
		c.close();

		// サーバ上にある駅名の日本語辞書JSONを取得
		// メトロ駅用と、他社線駅用
		String jsToTitleMetro = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + "metro_stationDict.json", ServiceHandler.GET, null);
		String jsToTitleOther = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + "other_stationDict.json", ServiceHandler.GET, null);
		result.put("jsToTitleMetro", jsToTitleMetro);
		result.put("jsToTitleOther", jsToTitleOther);
		try {
			// メトロ駅用辞書JSONを使って、駅名を変換する
			JSONObject jsRoot = new JSONObject(jsToTitleMetro);
			// odpt:Station.TokyoMetro.部分を切り取ってから日本語に変換
			// EX) odpt:Station.TokyoMetro.Shibuya -> Shibuya -> 渋谷
			result.put("staCode", jsRoot.getString(startStId.substring(railwayId.length() + 1)));
			result.put("endCode", jsRoot.getString(endStId.substring(railwayId.length() + 1)));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// 曜日判定
		Calendar calen = new GregorianCalendar(
				Integer.parseInt(result.get("mYear")),
				Integer.parseInt(result.get("mMonth")) - 1,
				Integer.parseInt(result.get("mDate"))
				);
		String day_of_week;
		switch(calen.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.SUNDAY: day_of_week = "odpt:holidays"; break;
		case Calendar.SATURDAY: day_of_week = "odpt:saturdays"; break;
		default: day_of_week = "odpt:weekdays";
		}
		result.put("day_of_week", day_of_week);
		System.out.println(day_of_week);

		return result;
	}

	@Override
	protected void onPostExecute(HashMap<String, String> result) {
		super.onPostExecute(result);
		// 各TextViewにデータを格納
		viewRailwayDecide.setText(result.get("railwayName"));
		viewStartStDecide.setText(result.get("staCode"));
		viewEndStDecide.setText(result.get("endCode"));
		dateDisplay.setText(result.get("mYear") + "年" + result.get("mMonth") + "月" + result.get("mDate") + "日");
		timeDisplay.setText(result.get("mHour") + "時" + result.get("mMinute") + "分");
		timeTypeDisplay.setText(result.get("timeType"));
		String day_of_week = result.get("day_of_week");
		List<String> list = new ArrayList<String>();

		String destStaTitle = null, depaTime, destSta, trainType, trainTypeTitle = null;
		// 日本語変換
		try {
			// StationTimetableAPIの返り値のJSON
			JSONArray jsRoot = new JSONArray(result.get("json"));
			// 指定した曜日の時刻表のJSON
			JSONArray jsArray = jsRoot.getJSONObject(0).getJSONArray(day_of_week);

			// 駅名の辞書JSON
			JSONObject jsToTitleMetro = new JSONObject(result.get("jsToTitleMetro"));
			JSONObject jsToTitleOther = new JSONObject(result.get("jsToTitleOther"));

			String[] sp;
			int tableCount = 0;
			// 指定した出発時間
			String timeDecide = result.get("mHour") + ":" + result.get("mMinute");
			int deciHour = Integer.parseInt(result.get("mHour"));
			int deciMinu = Integer.parseInt(result.get("mMinute"));
			if(deciHour == 0) deciHour = 24; // 0時は24時として扱う
			for (int i = 0; i < jsArray.length(); i++) {
				JSONObject jsonOneRecord = jsArray.getJSONObject(i);

				// 時刻表の出発時間
				depaTime = jsonOneRecord.getString("odpt:departureTime");
				int depaHour = Integer.parseInt(depaTime.substring(0,2));
				int depaMinu = Integer.parseInt(depaTime.substring(3,5));
				if(depaHour == 0) depaHour = 24; // 0時は24時として扱う

				if(deciHour > depaHour // 指定した時が大きい場合はスキップ
						|| (deciHour == depaHour && deciMinu > depaMinu)) {  // 時が同じ、指定した分が大きい場合はスキップ
					continue;
				}
				tableCount++;
				if(tableCount == 10) break; // 時刻表は10件まで

				// 行き先駅名
				destSta = jsonOneRecord.getString("odpt:destinationStation");
				sp = destSta.split("\\.");
				if(sp[sp.length - 3].equals("Station:TokyoMetro")) {
					destStaTitle = jsToTitleMetro.getString(sp[sp.length - 1]);					
				} else {
					destStaTitle = jsToTitleOther.getString(destSta);
				}

				// 列車タイプ（各停、急行など）
				// if分岐で全部やるのはナンセンスです...
				trainType = jsonOneRecord.getString("odpt:trainType");
				if(trainType.equals("odpt.TrainType:TokyoMetro.Express")) {
					trainTypeTitle = "急行";
				} else if(trainType.equals("odpt.TrainType:TokyoMetro.Rapid")) {
					trainTypeTitle = "快速";
				} else if(trainType.equals("odpt.TrainType:TokyoMetro.LimitedExpress")) {
					trainTypeTitle = "特急";
				} else if(trainType.equals("odpt.TrainType:TokyoMetro.Local")) {
					trainTypeTitle = "各停";
				}
				list.add(depaTime + " : " + destStaTitle + "行き" + "  " + trainTypeTitle);
				System.out.println(depaTime + " : " + destStaTitle + "行き" + "  " + trainTypeTitle);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		adapterTime = new CustomAdapter(activity, R.layout.timetable_row, list);
		// SelectTrainActivityでオーバーライドしているonPostExecuteメソッドを呼ぶ
		if (callbackListener != null) {
			callbackListener.onPostExecute(adapterTime);
		}
		if (pDialog.isShowing())
			pDialog.dismiss();
	}

	public class CustomAdapter extends ArrayAdapter<String> {
		private LayoutInflater mLayoutInflater;
		public CustomAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO 自動生成されたメソッド・スタブ
			String item = (String) getItem(position);
			if (null == convertView) {
				convertView = mLayoutInflater.inflate(R.layout.timetable_row, null);
			}
			TextView textView;
			textView = (TextView) convertView.findViewById(R.id.timetableList);
			textView.setText(item);
			return convertView;
		}
	}
}
