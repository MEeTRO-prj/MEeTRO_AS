package com.railway.meetro;

import java.util.Random;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.railway.controller.AddNewUser;
import com.railway.controller.AddNewUserCallback;
import com.railway.controller.SearchRoomCallback;
import com.railway.helper.CheckGcmHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/*
 * ユーザ作成の画面
 */
public class CreateUserActivity extends Activity implements AddNewUserCallback  {
	final String TAG = "CreateUserActivity";
	private Context context;
	SharedPreferences sp;
	
	// ユーザ情報の変数
	public int userId;
	public String regid = "";
	public String userName;

	// GCM
	public GoogleCloudMessaging gcm;
	public CheckGcmHelper checkGcm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_user);
		context = this.getApplicationContext();

		// プリファレンスから値を取得
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		userId = sp.getInt("userId", 0);
		userName = sp.getString("userName", null);
		Log.d(TAG, "userId: " + userId + " userName: " + userName);

		// プリファレンスにユーザ情報が存在する場合
		if(userId != 0 && userName != null) {
			Intent intent = new Intent(CreateUserActivity.this, SelectStationActivity.class);
			Log.d(TAG, "roomId: " + getIntent().getStringExtra("room_id"));
			intent.putExtra("roomId", getIntent().getStringExtra("room_id"));
			startActivity(intent);
			CreateUserActivity.this.finish();
		} else {
			// プリファレンスにユーザ情報が存在しない場合
			// Play serviceが有効かチェックし、regidを取得
			checkGcm = new CheckGcmHelper(context, CreateUserActivity.this);
			if (checkGcm.checkPlayServices()) {
				gcm = GoogleCloudMessaging.getInstance(this);
				// RegistrationIDをプリファレンスから取得
				regid = checkGcm.getRegistrationId(context);
				Log.d("Registrationid", "regid: " + regid);
				// RegistrationIDがない場合は新規発行してGCMに登録
				if(regid.isEmpty()){
					regid = checkGcm.regist_id();
				}
			} else {
				Log.d(TAG, "Google Play Services are not enable");
				Toast.makeText(CreateUserActivity.this, "Google Play Services are not enable", Toast.LENGTH_LONG).show();
				return;
			}

			Button btnSaveName = (Button) findViewById(R.id.buttonSaveName);
			// ユーザ作成
			btnSaveName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// エディットテキストのテキストを取得します
					EditText editText = (EditText) findViewById(R.id.nameBox);
					userName = editText.getText().toString();
					
					// 入力してない時はランダムに決めちゃう（これもCommonMethod化するか）
					if (userName == null || userName.equals("")) {
						// サーバ上とアプリ内USERテーブルに挿入（デフォルト値からランダム）
						String[] defaultUser = {"暗黒の騎士", "薔薇の貴公子", "裸の王様"};
						Random r = new Random();
						userName = defaultUser[r.nextInt(defaultUser.length)];
					}
					// サーバ上とアプリ内USERテーブルに挿入
					regid = checkGcm.getRegistrationId(context);
					Log.d(TAG, "userName:" + userName);
					new AddNewUser(CreateUserActivity.this, CreateUserActivity.this).execute(regid, userName);
				}
			});
		}
	}

	@Override
	public void onPostExecute(String userId, String userName) {
		// プリファレンスに保存
		sp.edit().putInt("userId", Integer.parseInt(userId)).commit();
		sp.edit().putString("userName", userName).commit();

		// SelectStationActivityに遷移
		Intent intent = new Intent(CreateUserActivity.this, SelectStationActivity.class);
		intent.putExtra("roomId", getIntent().getStringExtra("room_id"));
		startActivity(intent);
		CreateUserActivity.this.finish();
		
	}
}
