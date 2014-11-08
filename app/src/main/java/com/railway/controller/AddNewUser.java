package com.railway.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.railway.helper.MeetroDbOpenHelper;
import com.railway.meetro.R;
import com.railway.utility.CommonConfig;
import com.railway.utility.ServiceHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

/*
 * ユーザを新規登録する非同期処理
 */
public class AddNewUser extends AsyncTask<String, Void, List<NameValuePair>> {
	private AddNewUserCallback callbackListener = null;
	private Activity activity;
	boolean isNewUserCreated = false;
	private String PHP_PATH = "new_user.php";
	ProgressDialog pDialog;
	SharedPreferences sp;
	
	public AddNewUser(Activity activity) {
		this.activity = activity;
	}
	// コンストラクタ
	public AddNewUser(Activity activity, AddNewUserCallback listener) {
		this.activity = activity;
		this.callbackListener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pDialog = new ProgressDialog(activity);
		pDialog.setMessage("Creating new user..");
		pDialog.setCancelable(false);
		pDialog.show();
	}

	@Override
	protected List<NameValuePair> doInBackground(String... arg) {
		String regid = arg[0];
		String user_name = arg[1];
		Log.d("AddNewUser", "regi_id: " + regid);
		Log.d("AddNewUser", "user_name: " + user_name);

		sp = PreferenceManager.getDefaultSharedPreferences(activity);

		// Preparing post params
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("REGI_ID", regid));
		params.add(new BasicNameValuePair("USER_NAME", user_name));

		// サーバ上のDBに登録
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
					isNewUserCreated = true;
					Log.d("Create User Error: ", "> " + jsonObj.getString("user_id"));
					params.add(new BasicNameValuePair("_ID", jsonObj.getString("user_id")));
				} else {
					Log.e("Create User Error: ", "> " + jsonObj.getString("message"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.e("JSON Data", "Didn't receive any data from server!");
		}
		return params;
	}

	@Override
	protected void onPostExecute(List<NameValuePair> result) {
		super.onPostExecute(result);
		if (isNewUserCreated) {
			// アプリ内DBのUSERに挿入
			MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(activity);
			SQLiteDatabase userDbW = userHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("REGI_ID", result.get(0).getValue());
			values.put("USER_NAME", result.get(1).getValue());
			values.put("_id", result.get(2).getValue());
			
			long _id = userDbW.insert("USER", null, values);
			Log.d("onPostExecute", "insert data is " + _id);

			// CreateUserActivityでオーバーライドしているonPostExecuteメソッドを呼ぶ
			if (callbackListener != null) {
				callbackListener.onPostExecute(result.get(2).getValue(), result.get(1).getValue());
			}

		}
		if (pDialog.isShowing())
			pDialog.dismiss();
	}
}
