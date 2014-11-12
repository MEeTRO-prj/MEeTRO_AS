package com.railway.meetro;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

/*
 * アプリ起動時の画面
 */
public class IntroActivity extends Activity {
	String TAG = "IntroActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intro);

		// 2秒したらMainActivityを呼び出してIntroActivityを終了する
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				String intentType = getIntent().getType();
				String action = getIntent().getAction();

				// notificationからアプリを起動した時
				if (intentType != null && intentType.equals("notification")) {
					Log.v(TAG + " extras", getIntent().getExtras().toString());
					// extrasからGcmIntentService.javaでいれたデータを取り出す
					String msgContext = getIntent().getStringExtra("msgContext");
					NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					Intent intent = new Intent(IntroActivity.this, MainActivity.class);
					startActivity(intent);
					// IntroActivityを終了する（戻るボタンの抑制）
					IntroActivity.this.finish();
				} 
				// ブラウザからアプリを起動した時(パラメータ: room_id)
				else if (Intent.ACTION_VIEW.equals(action)) {
					Uri uri = getIntent().getData();
					Log.d(TAG, String.valueOf(uri));
					if (uri != null) {
						String room_id = uri.getQueryParameter("room_id");
						Intent intent = new Intent(IntroActivity.this, CreateUserActivity.class);
						System.out.println("param1: " + room_id);
						intent.putExtra("room_id", room_id);
						startActivity(intent);
						// IntroActivityを終了する（戻るボタンの抑制）
						IntroActivity.this.finish();
					}
				}
				// 普通にアプリを起動した時
				else {
					Intent intent = new Intent(IntroActivity.this, MainActivity.class);
					startActivity(intent);
					// IntroActivityを終了する（戻るボタンの抑制）
					IntroActivity.this.finish();					
				}
			}
		}, 2 * 1000); // 2000ミリ秒後（2秒後）に実行
	}
}
