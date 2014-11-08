package com.railway.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.railway.helper.MeetroDbOpenHelper;
import com.railway.utility.CommonConfig;
import com.railway.utility.ServiceHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

/*
 * ユーザ情報を更新する非同期処理
 */
public class UpdateUser extends AsyncTask<String, Void, String> {
	private Activity activity;
	boolean isNewUserCreated = false;
	private String PHP_PATH = "update_user.php";
	ProgressDialog pDialog;
	
	public UpdateUser(Activity activity) {
		this.activity = activity;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pDialog = new ProgressDialog(activity);
		pDialog.setMessage("Updating user..");
		pDialog.setCancelable(false);
		pDialog.show();
	}

	@Override
	protected String doInBackground(String... arg) {
		String _id = arg[0];
		String regid = arg[1];
		Log.d("UpdateUser", "_id: " + _id);
		Log.d("UpdateUser", "regi_id: " + regid);

		// Preparing post params
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("USER_ID", _id));
		params.add(new BasicNameValuePair("REGI_ID", regid));

		// サーバ上のDBを更新
		ServiceHandler serviceClient = new ServiceHandler();
		String json = serviceClient.makeServiceCall(CommonConfig.getSERVER_URL() + PHP_PATH, ServiceHandler.POST, params);
		Log.d("Create Response: ", "> " + json);

		if (json != null) {
			try {
				JSONObject jsonObj = new JSONObject(json);
				boolean error = jsonObj.getBoolean("error");
				// checking for error node in json
				if (!error) {   
					// new user created successfully
					Log.d("Update User Error: ", "> " + jsonObj.getString("message"));
				} else {
					Log.e("Update User Error: ", "> " + jsonObj.getString("message"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.e("JSON Data", "Didn't receive any data from server!");
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if (pDialog.isShowing())
			pDialog.dismiss();
	}
}
