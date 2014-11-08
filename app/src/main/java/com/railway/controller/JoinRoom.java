package com.railway.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.railway.utility.CommonConfig;
import com.railway.utility.ServiceHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

/*
 * 選択した時刻表から新しい部屋を作成する非同期処理
 */
public class JoinRoom extends AsyncTask<String, Void, String[]> {
	private Activity activity;
	private SearchRoomCallback callbackListener = null;
	boolean isNewRoomCreated = false;
	private String PHP_PATH = "join_room.php";
	ProgressDialog pDialog;
	HashMap<String, String> roomInfo;

	// コンストラクタ
	public JoinRoom(Activity activity, HashMap<String, String> roomInfo, SearchRoomCallback listener) {
		this.activity = activity;
		this.roomInfo = roomInfo;
		this.callbackListener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		//		pDialog = new ProgressDialog(activity);
		//		pDialog.setMessage("Joining room..");
		//		pDialog.setCancelable(false);
		//		pDialog.show();
	}

	@Override
	protected String[] doInBackground(String... arg) {
		String room_id = arg[0]; // Ex) 167
		String user_id = arg[1]; // Ex) 104
		String ride_st = arg[2]; // Ex) odpt:Station.TokyoMetro.Omotesando
		for(int i = 0; i < arg.length; i++) {
			System.out.println("JoinRoom: " + arg[i]);
		}
		System.out.println("JoinRoom['trainNumber']: " + roomInfo.get("trainNumber")); // Ex) A1115
		String day_of_week = roomInfo.get("roomDayOfWeek");

		// APIに渡すパラメータをparamsに格納
		List<NameValuePair> paramsTT = new ArrayList<NameValuePair>();
		paramsTT.add(new BasicNameValuePair("rdf:type", "odpt:TrainTimetable"));
		paramsTT.add(new BasicNameValuePair("odpt:railway", roomInfo.get("roomRailwayId")));
		paramsTT.add(new BasicNameValuePair("odpt:trainNumber", roomInfo.get("trainNumber")));
		paramsTT.add(new BasicNameValuePair("acl:consumerKey", CommonConfig.getACCESS_KEY()));

		// TrainTimetableAPIを叩く(レスポンス->json)
		ServiceHandler serviceClient = new ServiceHandler();
		String jsonTT = serviceClient.makeServiceCall(CommonConfig.getAPI_URL(), ServiceHandler.GET, paramsTT);
		// TrainTimetableAPIの返り値のJSON
		JSONArray jsRoot2;
		String depaTime = null;
		try {
			jsRoot2 = new JSONArray(jsonTT);
			System.out.println("jsRoot2.length(): " + jsRoot2.length());
			System.out.println("day_of_week: " + day_of_week);
			for(int i = 0; i < jsRoot2.length(); i++) {
				if(!jsRoot2.getJSONObject(i).has(day_of_week)) { // 平日or土日
					continue;
				}
				String departureSta;
				JSONArray jsArray2 = jsRoot2.getJSONObject(i).getJSONArray(day_of_week);
				System.out.println("jsArray2.length(): " + jsArray2.length());
				for (int j = 0; j < jsArray2.length(); j++) {
					JSONObject jsonOneRecord2 = jsArray2.getJSONObject(j);
					if(jsonOneRecord2.has("odpt:departureTime")) {
						depaTime = jsonOneRecord2.getString("odpt:departureTime");
						departureSta = jsonOneRecord2.getString("odpt:departureStation");
					} else {
						depaTime = jsonOneRecord2.getString("odpt:arrivalTime");
						departureSta = jsonOneRecord2.getString("odpt:arrivalStation");
					}
					if(ride_st.equals(departureSta)) {
						System.out.println("depaTime: " + depaTime + " departureSta: " + departureSta);
						break;
					}
				}
			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Preparing post params
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ROOM_ID", room_id));
		params.add(new BasicNameValuePair("USER_ID", user_id));
		params.add(new BasicNameValuePair("RIDE_ST", ride_st)); // Ex) odpt:Station.TokyoMetro.Omotesando
		params.add(new BasicNameValuePair("RIDE_TIME", depaTime));

		// サーバ上のDBに部屋を作成
		String json = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + PHP_PATH, ServiceHandler.POST, params);
		Log.d("Create Response: ", "> " + json);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
		// プリファレンスにroom_id入れとく
		sp.edit().putInt("roomId", Integer.parseInt(room_id)).commit();

		if (json != null) {
			try {
				JSONObject jsonObj = new JSONObject(json);
				boolean error = jsonObj.getBoolean("error");
				// checking for error node in json
				if (!error) {
					// join room successfully
					Log.d("Update User Error: ", "> " + jsonObj.getString("message"));
				} else {
					Log.e("Create User Error: ", "> " + jsonObj.getString("message"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.e("JSON Data", "Didn't receive any data from server!");
		}

		// 駅名の日本語変換用JSON
		String jsToTitleMetro = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + "metro_stationDict.json", ServiceHandler.GET, null);
		String rideStTitleJpn = null;
		try {
			// 駅名の日本語変換用JSONをJSONObjectに格納
			JSONObject jsObjToTitleMetro = new JSONObject(jsToTitleMetro);
			// odpt:Station.TokyoMetro.部分を切り取る
			// EX) odpt:Station.TokyoMetro.Shibuya -> Shibuya
			String rideStTitleEng = ride_st.substring(ride_st.lastIndexOf(".") + 1);
			// EX) Shibuya -> 渋谷
			rideStTitleJpn = jsObjToTitleMetro.getString(rideStTitleEng);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String[] result = { rideStTitleJpn , depaTime };

		return result;
	}

	@Override
	protected void onPostExecute(String[] result) {
		super.onPostExecute(result);
		// SelectStationActivityでオーバーライドしているonPostExecuteメソッドを呼ぶ
		if (callbackListener != null) {
			callbackListener.onPostExecute(result);
		}
		//		if (pDialog.isShowing())
		//			pDialog.dismiss();
	}
}
