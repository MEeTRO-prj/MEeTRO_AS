package com.railway.meetro;

import java.util.HashMap;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.railway.meetro.R;

public class RoomTopActivity extends Activity {
	String TAG = "RoomListActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room_top);		
		System.out.println("Here is RoomTopActivity.");

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
	}
}
