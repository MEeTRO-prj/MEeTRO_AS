package com.railway.meetro;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.railway.helper.CheckGcmHelper;
import com.railway.meetro.R;
import com.railway.utility.CommonConfig;
import com.railway.utility.DrawerItemClickListener;

public class RoomTopActivity extends ActionBarActivity {
	String TAG = "RoomListActivity";
	private Context context;

	// NavigationDrawer
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	AsyncTask<Void, Void, String> informTask = null;
	String roomNumber;

	SharedPreferences sp;
	public int userId;
	public String userName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room_top);
		context = this.getApplicationContext();

		sp = PreferenceManager.getDefaultSharedPreferences(RoomTopActivity.this);
		userId = sp.getInt("userId", 0);
		userName = sp.getString("userName", null);

		// 遷移元がどこであっても同じインタフェースで部屋情報を取得する
		Intent intentHash = getIntent();
		HashMap<String, String> roomInfo = (HashMap<String, String>)intentHash.getSerializableExtra("roomInfo");

		TextView decideRailway = (TextView) findViewById(R.id.decideRailway);
		TextView rideStartSt = (TextView) findViewById(R.id.rideStartSt);
		TextView rideDate = (TextView) findViewById(R.id.rideDate);
		TextView trainType = (TextView) findViewById(R.id.trainTypeTitle);
		TextView endStTitle = (TextView) findViewById(R.id.endStTitle);
		TextView rideTime = (TextView) findViewById(R.id.rideTime);
		TextView timeType = (TextView) findViewById(R.id.timeType);
		TextView carNum = (TextView) findViewById(R.id.carNum);

		roomNumber = roomInfo.get("roomNumber");             // Ex) 20141024102
		System.out.println("roomNumber: " + roomNumber);
		//		String roomEndSt = roomInfo.get("roomDestSt");              // Ex) 表参道
		decideRailway.setText(roomInfo.get("roomRailway"));         // Ex) 銀座線
		rideStartSt.setText(roomInfo.get("roomStartSt") + "駅");    // Ex) 渋谷駅
		rideDate.setText(roomInfo.get("roomDate"));                 // Ex) 2014年10月24日(金)
		trainType.setText(roomInfo.get("trainTypeTitle"));          // Ex) 急行
		endStTitle.setText(roomInfo.get("endStTitle") + "行");      // Ex) 浅草行
		rideTime.setText(roomInfo.get("roomTime"));                 // Ex) 12:15
		timeType.setText(roomInfo.get("roomTimeType"));             // Ex) 発
		carNum.setText(roomInfo.get("roomCarNum"));                 // Ex) 3

		ImageButton popupBtn = (ImageButton) findViewById(R.id.popupBtn);
		popupBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PopupMenu popup = new PopupMenu(RoomTopActivity.this, v);
				// res/menu/popup.xmlで設定した項目をポップアップに割り当てる
				popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
				// ポップアップメニューを表示
				popup.show();
				// ポップアップメニューの項目を押下した時
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						Toast.makeText(RoomTopActivity.this, "Sorry, it is constructing now.", Toast.LENGTH_LONG).show();
						return true;
					}
				});
			}
		});
		final Button sorryBtn = (Button) findViewById(R.id.buttonSorryLate);
		sorryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// GCM通知でHttpConnectをするため非同期処理
				informTask = new AsyncTask<Void, Void, String>() {
					@Override
					protected String doInBackground(Void... params) {
						String roomId = roomNumber.substring(8);
						System.out.println("roomId: " + roomId);
						// GCMでROOMのOWNERに遅れを通知する
						informRoomOwnerGCM(roomId, userId, userName);
						return null;
					}
					@Override
					protected void onPostExecute(String result) {
						informTask = null;
					}
				};
				sorryBtn.setEnabled(false);
				Toast.makeText(RoomTopActivity.this, "遅延通知を送信しました。", Toast.LENGTH_LONG).show();
				informTask.execute(null, null, null);
			}
		});

		// DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		setupNavigationDrawer();
	}

	// 集合に遅れることを部屋参加者に通知する
	public boolean informRoomOwnerGCM(String roomId, int userId, String userName) {
		String serverUrl = CommonConfig.getSERVER_URL() + "GCM_late.php";
		System.out.println("serverUrl: " + serverUrl);
		Map<String, String> params = new HashMap<String, String>();
		params.put("ROOM_ID", roomId);
		params.put("USER_ID", String.valueOf(userId));
		params.put("USER_NAME", userName);
		try {
			CheckGcmHelper cgh = new CheckGcmHelper(RoomTopActivity.this, RoomTopActivity.this);
			cgh.post(serverUrl, params);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
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
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener(context, mDrawerLayout, mDrawerList));
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
