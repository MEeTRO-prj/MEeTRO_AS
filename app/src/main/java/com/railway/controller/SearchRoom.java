package com.railway.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.railway.bean.RoomBean;
import com.railway.utility.CommonConfig;
import com.railway.utility.ServiceHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public class SearchRoom extends AsyncTask<String, Void, RoomBean> {
	private SearchRoomCallback callbackListener = null;

	private String PHP_PATH = "room_detail.php";
	private Activity activity;
	ProgressDialog pDialog;
//	CustomAdapter adapterRoom;

	// コンストラクタ
	public SearchRoom(Activity activity, SearchRoomCallback listener) {
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
	protected RoomBean doInBackground(String... arg) {
		String roomId = arg[0];

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ROOM_ID", roomId));

		ServiceHandler serviceClient = new ServiceHandler();
		// 部屋リストのJSON
		String jsRoom = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + PHP_PATH, ServiceHandler.POST, params);
		Log.d("Create Response: ", "> " + jsRoom);

		// 駅名の日本語変換用JSON
		String jsToTitleMetro = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + "metro_stationDict.json", ServiceHandler.GET, null);

		// DB
//		MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(activity);

		RoomBean roomBean = new RoomBean();
		try {
			// 部屋リストのJSONをJSONObject->JSONArrayに格納
			JSONObject jsRoot = new JSONObject(jsRoom);
			JSONArray jsArray = jsRoot.getJSONArray("room");
			System.out.println("jsRoot.length(): " + jsRoot.length());
			System.out.println("jsArray.length(): " + jsArray.length());

			// 駅名の日本語変換用JSONをJSONObjectに格納
			JSONObject jsObjToTitleMetro = new JSONObject(jsToTitleMetro);

			// 路線名の日本語変換用DBデータ取得
//			SQLiteDatabase userDbR = userHelper.getReadableDatabase();

			for (int i = 0; i < jsArray.length(); i++) {
//				Cursor c = null;
				JSONObject jsOneRecord = jsArray.getJSONObject(i);
				roomBean.setRoomId(jsOneRecord.getInt("room_id"));
				roomBean.setOwnerId(jsOneRecord.getInt("owner_id"));
				roomBean.setRideDate(jsOneRecord.getString("ride_date"));
				roomBean.setRideTime(jsOneRecord.getString("ride_time"));
				roomBean.setTimeType(jsOneRecord.getInt("time_type"));

				String railwayId = jsOneRecord.getString("railway_id");
				roomBean.setRailwayId(railwayId); // EX) odpt.Railway:TokyoMetro.Ginza
				// DBからrailway_idと一致する路線名を取得
				// EX) odpt.Railway:TokyoMetro.Ginza -> 銀座線
//				c = userDbR.rawQuery("SELECT RAILWAY_NAME FROM RAILWAY WHERE RAILWAY_ID = ?;", new String[]{railwayId});			
//				String railway_name = null;
//				while(c.moveToNext()) {
//					railway_name = c.getString(c.getColumnIndex("RAILWAY_NAME"));
//				}
//				roomBean.setRailwayId(railway_name);

				// odpt:Station.TokyoMetro.部分を切り取る
				// EX) odpt:Station.TokyoMetro.Shibuya -> Shibuya
				String rideStTitleEng = jsOneRecord.getString("ride_st").substring(railwayId.length() + 1);
				String destStTitleEng = jsOneRecord.getString("dest_st").substring(railwayId.length() + 1);
				// EX) Shibuya -> 渋谷
				String rideStTitleJpn = jsObjToTitleMetro.getString(rideStTitleEng);
				String destStTitleJpn = jsObjToTitleMetro.getString(destStTitleEng);
				roomBean.setRideSt(rideStTitleJpn);
				roomBean.setDestSt(destStTitleJpn);
				roomBean.setEndSt(jsOneRecord.getString("end_st"));
				roomBean.setTrainType(jsOneRecord.getString("train_type"));
				roomBean.setCarNum(jsOneRecord.getInt("car_num"));
				roomBean.setTrainNumber(jsOneRecord.getString("train_number"));
				roomBean.setUseFlg(jsOneRecord.getInt("use_flg"));
//				c.close();
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return roomBean;
	}

	@Override
	protected void onPostExecute(RoomBean result) {
		super.onPostExecute(result);
		// SelectStatonActivityでオーバーライドしているonPostExecuteメソッドを呼ぶ
		if (callbackListener != null) {
			callbackListener.onPostExecute(result);
		}
		if (pDialog.isShowing())
			pDialog.dismiss();
	}
}