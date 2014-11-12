package com.railway.utility;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.railway.meetro.MainActivity;
import com.railway.meetro.MakeRoomActivity;
import com.railway.meetro.RoomListActivity;

public class DrawerItemClickListener implements ListView.OnItemClickListener {
	private Context context;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;

	public DrawerItemClickListener(Context context, DrawerLayout mDrawerLayout, ListView mDrawerList) {
		this.context = context;
		this.mDrawerLayout = mDrawerLayout;
		this.mDrawerList = mDrawerList;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		ListView listView = (ListView) adapterView;
		// クリックされたアイテムを取得します
		String item = (String) listView.getItemAtPosition(position);
		Intent intent;
		switch((int) listView.getItemIdAtPosition(position)) {
			case 0: // ホーム
				intent = new Intent(context, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
				break;
			case 1: // 部屋を作る
				intent = new Intent(context, MakeRoomActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
				break;
			case 2: // 部屋に入る
				intent = new Intent(context, RoomListActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("scene", "fromMain");
				context.startActivity(intent);
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
}