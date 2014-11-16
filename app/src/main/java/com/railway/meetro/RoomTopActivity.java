package com.railway.meetro;

import java.util.HashMap;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.railway.meetro.R;
import com.railway.utility.DrawerItemClickListener;

public class RoomTopActivity extends ActionBarActivity {
	String TAG = "RoomListActivity";
	private Context context;

	// NavigationDrawer
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room_top);
		context = this.getApplicationContext();

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
		System.out.println(roomInfo.get("roomStartSt"));
		System.out.println(roomInfo.get("roomDate"));

		//		String roomNumber = roomInfo.get("roomNumber");             // Ex) 20141024102
		//		String roomEndSt = roomInfo.get("roomDestSt");              // Ex) 表参道
		decideRailway.setText(roomInfo.get("roomRailway"));         // Ex) 銀座線
		rideStartSt.setText(roomInfo.get("roomStartSt") + "駅");    // Ex) 渋谷駅
		rideDate.setText(roomInfo.get("roomDate"));                 // Ex) 2014年10月24日(金)
		trainType.setText(roomInfo.get("trainTypeTitle"));          // Ex) 急行
		endStTitle.setText(roomInfo.get("endStTitle") + "行");      // Ex) 浅草行
		rideTime.setText(roomInfo.get("roomTime"));                 // Ex) 12:15
		timeType.setText(roomInfo.get("roomTimeType"));             // Ex) 発
		carNum.setText(roomInfo.get("roomCarNum"));                 // Ex) 3

		Button popupBtn = (Button) findViewById(R.id.popupBtn);
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
						return true;
					}
				});
			}
		});
		// DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		setupNavigationDrawer();
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
