package com.railway.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import com.railway.utility.CommonMethod;
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
public class MakeNewRoom extends AsyncTask<String, Void, String> {
	private Activity CompleteActivity;
	boolean isNewRoomCreated = false;
	private String PHP_PATH = "make_room.php";
	ProgressDialog pDialog;
	TextView roomUrl;

	public MakeNewRoom(Activity activity, TextView roomUrl) {
		CompleteActivity = activity;
		this.roomUrl = roomUrl;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pDialog = new ProgressDialog(CompleteActivity);
		pDialog.setMessage("Creating new room..");
		pDialog.setCancelable(false);
		pDialog.show();
	}

	@Override
	protected String doInBackground(String... arg) {
		String owner_id = arg[0];
		String ride_date = arg[1];
		String ride_time = arg[2];
		String time_type = arg[3];
		String railway_id = arg[4];
		String ride_st = arg[5];
		String dest_st = arg[6];
		String endStaTitle = arg[7];
		String trainTypeTitle = arg[8];
		String car_num = arg[9];
		String direction = arg[10];
		for(int i = 0; i < arg.length; i++) {
			System.out.println("MakeNewRoom: " + arg[i]);
		}
		String trainNumber = "";
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(CompleteActivity);

		String roomRideDate = ride_date.replace("-", "");
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

		// APIに渡すパラメータをparamsに格納
		List<NameValuePair> paramsTT = new ArrayList<NameValuePair>();
		paramsTT.add(new BasicNameValuePair("rdf:type", "odpt:TrainTimetable"));
		paramsTT.add(new BasicNameValuePair("odpt:railway", railway_id));
		paramsTT.add(new BasicNameValuePair("odpt:railDirection", direction));
		paramsTT.add(new BasicNameValuePair("acl:consumerKey", CommonConfig.getACCESS_KEY()));
		// TrainTimetableAPIを叩く(レスポンス->json)
		ServiceHandler serviceClient = new ServiceHandler();
		String jsonTT = serviceClient.makeServiceCall(CommonConfig.getAPI_URL(), ServiceHandler.GET, paramsTT);
		String station = null;
		switch (Integer.parseInt(time_type)) {
			case 0: station = ride_st;break;
			case 1: station = dest_st;break;
		}
		// TrainTimetableAPIの返り値のJSON
		JSONArray jsRoot2;
		try {
			jsRoot2 = new JSONArray(jsonTT);
			System.out.println("jsRoot2.length(): " + jsRoot2.length());
			for(int i = 0; i < jsRoot2.length(); i++) {
				if(!jsRoot2.getJSONObject(i).has(day_of_week)) { // 平日or土日
					continue;
				}
				String depaTime;
				String departureSta;
				JSONArray jsArray2 = jsRoot2.getJSONObject(i).getJSONArray(day_of_week);
				for (int j = 0; j < jsArray2.length(); j++) {
					JSONObject jsonOneRecord2 = jsArray2.getJSONObject(j);
					if(jsonOneRecord2.has("odpt:departureTime")) {
						depaTime = jsonOneRecord2.getString("odpt:departureTime");
						departureSta = jsonOneRecord2.getString("odpt:departureStation");
					} else {
						depaTime = jsonOneRecord2.getString("odpt:arrivalTime");
						departureSta = jsonOneRecord2.getString("odpt:arrivalStation");
					}
					if(ride_time.equals(depaTime) && station.equals(departureSta)) {
						System.out.println("depaTime: " + depaTime + " departureSta: " + departureSta);
						System.out.println(jsRoot2.getJSONObject(i).getString("odpt:trainNumber"));
						trainNumber = jsRoot2.getJSONObject(i).getString("odpt:trainNumber");
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
		params.add(new BasicNameValuePair("OWNER_ID", owner_id));
		params.add(new BasicNameValuePair("RIDE_DATE", ride_date));
		params.add(new BasicNameValuePair("RIDE_TIME", ride_time));
		params.add(new BasicNameValuePair("TIME_TYPE", time_type));
		params.add(new BasicNameValuePair("RAILWAY_ID", railway_id));
		params.add(new BasicNameValuePair("RIDE_ST", ride_st));
		params.add(new BasicNameValuePair("DEST_ST", dest_st));
		params.add(new BasicNameValuePair("END_ST", endStaTitle));
		params.add(new BasicNameValuePair("TRAIN_TYPE", trainTypeTitle));
		params.add(new BasicNameValuePair("CAR_NUM", car_num));
		params.add(new BasicNameValuePair("TRAIN_NUMBER", trainNumber));

		// サーバ上のDBに部屋を作成
		String json = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + PHP_PATH, ServiceHandler.POST, params);
		Log.d("Create Response: ", "> " + json);
		String originalUrl = "";
		String room_id = "";

		if (json != null) {
			try {
				JSONObject jsonObj = new JSONObject(json);
				boolean error = jsonObj.getBoolean("error");
				// checking for error node in json
				if (!error) {   
					// new user created successfully
					isNewRoomCreated = true;
					Log.d("Create Room Error: ", "> " + jsonObj.getString("room_id"));
					room_id = jsonObj.getString("room_id");
					// プリファレンスにroom_id入れとく
					sp.edit().putInt("roomId", Integer.parseInt(room_id)).commit();
					originalUrl = CommonConfig.getSERVER_URL() + "meetro_start.php?room_id=" + room_id;
				} else {
					Log.e("Create User Error: ", "> " + jsonObj.getString("message"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.e("JSON Data", "Didn't receive any data from server!");
		}

		// Googleのgoo.glAPIで短縮URLを生成
		String apiUri="https://www.googleapis.com/urlshortener/v1/url";
		String apiKey = CommonConfig.getGoogleApiKey();
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.path(apiUri);
		//		uriBuilder.appendQueryParameter("key", apiKey); // 推奨だけどあるとうまくいかない...
		HttpPost post = new HttpPost(Uri.decode(uriBuilder.build().toString()));
		String shortUrl = "";
		try {
			post.setEntity(new StringEntity("{\"longUrl\": \"" + originalUrl + "\"}"));
			post.setHeader("Content-Type", "application/json");

			HttpResponse response = (new DefaultHttpClient()).execute(post);
			String stringEntity = EntityUtils.toString(response.getEntity());
			Log.v("RESPONSE", stringEntity);

			JSONObject jsRoot = new JSONObject(stringEntity);
			shortUrl = jsRoot.getString("id");

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			shortUrl = "Protocol Error";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			shortUrl = "IO Error";
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return shortUrl;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		// 短縮URLをTextViewにセット
		if (isNewRoomCreated) {
			roomUrl.setText(result);
		}
		if (pDialog.isShowing())
			pDialog.dismiss();
	}
}
