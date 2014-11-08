package com.railway.utility;

import java.util.Iterator;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.railway.meetro.IntroActivity;
import com.railway.meetro.MainActivity;
import com.railway.meetro.R;
import com.railway.meetro.R.drawable;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {
	private static final String TAG = GcmIntentService.class.getSimpleName();
	private Notification notification;
	private NotificationManager nm;
	NotificationCompat.Builder builder;
	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(TAG, "onHandleIntent");
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				Log.d("LOG","messageType(error): " + messageType + ",body:" + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				Log.d("LOG","messageType(deleted): " + messageType + ",body:" + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				Log.d("LOG","messageType(message): " + messageType + ",body:" + extras.toString());
			}
		}

		// extrasに、サーバからのdataが入ってる
		Log.v(TAG, extras.toString());
		String notificationMsg = extras.getString("notificationMsg");
		String msgTitle = extras.getString("msgTitle");
		String msgContext = extras.getString("msgContext");

		// 通知を開くとIntroActivityが表示されるようにする
		Intent i = new Intent(getApplicationContext(), IntroActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setType("notification");
		i.putExtra("msgContext", msgContext);

		// 第4引数を0にすると、Intentのextrasだけ更新されず、通知のテキストは違うのにextrasだけ同じなため、
		// Intent先のMainActivityでextrasを取得すると同じ値しかとれなくなる事態に
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
		.setContentIntent(pi)
		.setSmallIcon(R.drawable.ic_launcher)
		.setTicker(notificationMsg)  // 通知が来た時にステータスバーに表示されるテキスト
		.setContentTitle(msgTitle)  // 通知バーを開いた時に表示されるタイトル
		.setContentText(msgContext) // タイトルの下に表示されるテキスト
		;

		notification = notificationBuilder.build();
		notification.flags = Notification.FLAG_AUTO_CANCEL; // 通知を開いたら自動的に消える

		// NotificationManagerのインスタンス取得
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(R.string.app_name, notification); // 設定したNotificationを通知する
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}