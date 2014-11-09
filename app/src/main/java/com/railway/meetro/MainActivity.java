package com.railway.meetro;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.railway.helper.CheckGcmHelper;
import com.railway.helper.MeetroDbOpenHelper;
import com.railway.controller.AddNewUser;
import com.railway.controller.UpdateUser;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.res.Configuration;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class MainActivity extends ActionBarActivity {
	private static final String TAG = "MainActivity";
	private Context context;

	// ユーザ情報の変数
	SharedPreferences sp;
	public int userId;
	public String regid = "";
	public String userName;

	// GCM
	public GoogleCloudMessaging gcm;
	public CheckGcmHelper checkGcm;

	// DB
	MeetroDbOpenHelper userHelper = new MeetroDbOpenHelper(this);

	// NavigationDrawer
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this.getApplicationContext();
		
		// Play serviceが有効かチェックし、regidを取得
		checkGcm = new CheckGcmHelper(context, MainActivity.this);
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
			Toast.makeText(MainActivity.this, "Google Play Services are not enable", Toast.LENGTH_LONG).show();
			return;
		}

		// アプリ内DBでUSER検索を行う
		SQLiteDatabase userDbR = userHelper.getReadableDatabase();
		Cursor c = null;
		c = userDbR.rawQuery("SELECT * FROM USER ORDER BY _id DESC LIMIT 1", null);

		// getSharedPreferencesもあるけど、今回はDefaultを使うことにします。
		// SharedPreferences pref = getSharedPreferences(PREF_NAME, MainActivity.this.MODE_PRIVATE);
		// SharedPreferencesを経由して保存したデータはXMLファイルで保存され、保存先フォルダはdata/data/アプリのパッケージ名/shared_prefs/
		// PreferenceManager.getDefaultSharedPreferencesを使った場合、「アプリのパッケージ名_preferences.xml」というファイル名で保存される
		// getSharedPreferencesを使った場合、「引数に指定したPREF_NAME.xml」をファイル名として保存される
		sp = PreferenceManager.getDefaultSharedPreferences(this);

		if (c.getCount() == 0) {
			Log.d(TAG, "USER is null");
			//テキスト入力を受け付けるビューを作成する
			final EditText editView = new EditText(this);
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("ユーザ名を設定して下さい。")
			.setView(editView)
			// Android4からButtonの位置が逆になったらしい...違和感!!
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					regid = checkGcm.getRegistrationId(context);
					if (editView.getText().toString() != null) {
						// サーバ上とアプリ内USERテーブルに挿入
						new AddNewUser(MainActivity.this).execute(regid, editView.getText().toString());
						userName = editView.getText().toString();
					} else {
						// ユーザ名入力しなかったらどうする？
						// サーバ上とアプリ内USERテーブルに挿入（デフォルト値からランダム）
						String[] defaultUser = {"暗黒の騎士", "薔薇の貴公子", "裸の王様"};
						Random r = new Random();
						userName = defaultUser[r.nextInt(defaultUser.length)];
						new AddNewUser(MainActivity.this).execute(regid, userName);
					}
				}
			})
			.setPositiveButton("登録しないではじめる", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					regid = checkGcm.getRegistrationId(context);
					// サーバ上とアプリ内USERテーブルに挿入（デフォルト値からランダム）
					String[] defaultUser = {"暗黒の騎士", "薔薇の貴公子", "裸の王様"};
					Random r = new Random();
					userName = defaultUser[r.nextInt(defaultUser.length)];
					new AddNewUser(MainActivity.this).execute(regid, userName);
				}
			})
			.show();
			Log.d("After AddNewUser", userId + ":" + regid + ":" + userName);
		} else {
			Log.d(TAG, "USER is exsiting");
			// プリファレンスにあればそこから取得
			userId = sp.getInt("userId", 0);
			userName = sp.getString("userName", null);
			if ( userId == 0 && userName == null) {
				Log.d(TAG, "But Preference userName is null");				
				// なければDBから
				while(c.moveToNext()) {
					userId = c.getInt(c.getColumnIndex("_id"));
					userName = c.getString(c.getColumnIndex("USER_NAME"));
				}
			}
			SQLiteDatabase userDbW = userHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("REGI_ID", regid);
			// アプリ内DBのRegistrationIDを更新
			long _id = userDbW.update("USER", values, "_ID = " + userId, null);
			Log.d("onPostExecute", "update data is " + _id);

			// サーバ上のDBのUSER.REGI_IDも更新
			new UpdateUser(MainActivity.this).execute(String.valueOf(userId), regid);
			Log.d("After UpdateUser", userId + ":" + regid + ":" + userName);
		}
		// ひと通り終わったらDBのカーソル閉じる
		c.close();

		// DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		setupNavigationDrawer();
	}

	@Override
	public void onStart() {
		super.onStart();
		// 改めてプリファレンスか、アプリ内DBからユーザ取得
		userId = sp.getInt("userId", 0);
		userName = sp.getString("userName", null);
		Log.d("onStart: Preference", userId + ":" + userName);
		if ( userId == 0 && userName == null) {
			// 新規ユーザ作成時にUSER_ID(_id)が取れないんだもん...
			// -> コールバックでなんとかできそうだけど、一旦これで対処
			SQLiteDatabase userDbR = userHelper.getReadableDatabase();
			Cursor c2 = null;
			c2 = userDbR.rawQuery("SELECT * FROM USER ORDER BY _id DESC LIMIT 1", null);
			while(c2.moveToNext()) {
				userId = c2.getInt(c2.getColumnIndex("_id"));
				userName = c2.getString(c2.getColumnIndex("USER_NAME"));
				Log.d("onStart: DB", userId + ":" + userName);
				sp.edit().putInt("userId", userId).commit();
				sp.edit().putString("userName", userName).commit();
			}
		}

        Button makeRoom = (Button) findViewById(R.id.makeRoom);
        Button enterRoom   = (Button) findViewById(R.id.enterRoom);

		makeRoom.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, MakeRoomActivity.class);
				startActivity(intent);
			}
		});

		enterRoom.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, RoomListActivity.class);
				intent.putExtra("scene", "fromMain");
				startActivity(intent);
			}
		});
	}

	// NavigationDrawerの設定
	private void setupNavigationDrawer() {		
		// 前に戻るボタン->onOptionsItemSelected()と、左上のアイコンの有効化
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// DrawerListを開く/閉じるトグルボタン
		mDrawerToggle = new ActionBarDrawerToggle(
				this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// アダプターの生成
		ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(
				this, R.array.menuList, android.R.layout.simple_list_item_1);
		mDrawerList.setAdapter(adapter);

		// ナビゲーションドロワーがクリックされた時の挙動
		mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				ListView listView = (ListView) adapterView;			
				// クリックされたアイテムを取得します
				String item = (String) listView.getItemAtPosition(position);
				Intent intent;
				switch((int) listView.getItemIdAtPosition(position)) {
				case 0: // ホーム
					mDrawerLayout.closeDrawer(mDrawerList);
					break;
				case 1: // 部屋を作る
					intent = new Intent(MainActivity.this, MakeRoomActivity.class);
					startActivity(intent);
					break;
				case 2: // 部屋に入る
					intent = new Intent(MainActivity.this, RoomListActivity.class);
					intent.putExtra("scene", "fromMain");
					startActivity(intent);
					break;
				case 3: // チュートリアル
					mDrawerLayout.closeDrawer(mDrawerList);
					break;
				case 4: // 使い方
					mDrawerLayout.closeDrawer(mDrawerList);
					break;
				case 5: // ご意見・ご慕容
					mDrawerLayout.closeDrawer(mDrawerList);
					break;
				case 6: // 開発者について
					mDrawerLayout.closeDrawer(mDrawerList);
					break;
				default:
					// DrawerLayoutを閉じる
					mDrawerLayout.closeDrawer(mDrawerList);
				}
			}
		});
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// DrawerToggleの状態を同期する
		mDrawerToggle.syncState();
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// DrawerToggleの状態を同期する
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}