package com.railway.helper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.railway.meetro.CreateUserActivity;
import com.railway.meetro.MainActivity;
import com.railway.utility.CommonConfig;

/*
 * GCMと通信を行い、RegistrationIdの取得・登録を行う
 */
public class CheckGcmHelper {
	private static final String TAG = "CheckGcmHelper";
	private Context context;
	private Activity activity;

	public GoogleCloudMessaging gcm;
	public String regid;
	public static CommonConfig common;

	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	
	public CheckGcmHelper(Context context, Activity activity) {
		this.context = context;
		this.activity = activity;
	}

	// Play serviceが有効かチェック
	public boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.d(TAG, "Play Service not support");
			}
			return false;
		}
		return true;
	}

	// RegistrationIDをプリファレンスから読み込む
	public String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.d("getRegistrationId", "Registration not found.");
			return "";
		}
		/// アプリのバージョンが変わったらRegistrationIDの再取得が必要
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.d("getRegistrationId", "App version changed.");
			return "";
		}
		// プリファレンスから読み込んだRegistrationIDを返す
		return registrationId;
	}
	private SharedPreferences getGCMPreferences(Context context) {
		return context.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
	}
	// アプリのバージョンを取得
	private int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			throw new RuntimeException("package not found : " + e);
		}
	}

	// RegistrationIDをGCMに登録する
	public String regist_id(){ new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					//GCMサーバーへ登録
					regid = gcm.register(CommonConfig.getPROJECT_ID());
					Log.d("regist_id", "doInBackground: " + regid);
					// レジストレーションIDを端末に保存
					storeRegistrationId(context, regid);
				} catch (IOException e) {
					Log.d("regist_id", "Error :" + e.getMessage());
				}
				return regid;
			}
			@Override
			protected void onPostExecute(String result) {
				Log.d("regist_id", "onPostExecute: " + result);
			}
		}.execute(null, null, null);
		return regid;
	}

	// RegistrationIDをプリファレンスに保存する
	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGCMPreferences(context);
		int appVersion = getAppVersion(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}
	
	// 通知を送るpostメソッド
	public void post(String endpoint, Map<String, String> params)
			throws IOException {
		URL url;
		try {
			url = new URL(endpoint);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url: " + endpoint);
		}
		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, String> param = iterator.next();
			bodyBuilder.append(param.getKey()).append('=').append(param.getValue());
			if (iterator.hasNext()) {
				bodyBuilder.append('&');
			}
		}
		String body = bodyBuilder.toString();
		System.out.println("post body is " + body);
		byte[] bytes = body.getBytes();
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setFixedLengthStreamingMode(bytes.length);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
			OutputStream out = conn.getOutputStream();
			out.write(bytes);
			out.close();
			int status = conn.getResponseCode();
			if (status != 200) {
				throw new IOException("Post failed with error code " + status);
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
}
