package com.railway.meetro;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.railway.bean.RoomBean;
import com.railway.controller.SearchRoomList;
import com.railway.controller.SearchRoomListCallback;
import com.railway.meetro.R;

/*
 * 自分が所属する部屋の一覧画面
 */
public class RoomListActivity extends Activity implements SearchRoomListCallback {
	private final static String TAG = "RoomListActivity";

	// ユーザ情報の変数
	SharedPreferences sp;
	public int userId;
	public String regid = "";
	public String userName;

	SearchRoomList searchRoomList;
	private ListView viewRoomList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room_list);

		viewRoomList = (ListView) findViewById(R.id.listRoom);

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		userId = sp.getInt("userId", 0);
		userName = sp.getString("userName", null);
		Log.d(TAG, "userId: " + userId + "userName: " + userName);

		String scene = "";
		scene = getIntent().getStringExtra("scene");
		Log.d(TAG, "scene: " + scene);

		// MainActivityから来た場合
		if(scene != null && scene.equals("fromMain")) {
			// 自分が所属する部屋の一覧を取得し、一覧にする
			// SearchRoomListのコンストラクタ呼び出し
			searchRoomList = new SearchRoomList(this, RoomListActivity.this);
			// SearchRoomListの実行（検索！）
			searchRoomList.execute(String.valueOf(userId));

			// 部屋が複数ある場合は選択する
			viewRoomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					RoomBean rlb = (RoomBean) parent.getAdapter().getItem(position);
					view.setSelected(true);
					// 選択した項目における部屋情報を取得してTextViewに格納
					TextView roomNumber = (TextView)view.findViewById(R.id.roomNumber);
					TextView roomDateTime = (TextView)view.findViewById(R.id.roomDateTime);
					TextView roomRailway = (TextView)view.findViewById(R.id.roomRailway);
					TextView roomStartSt = (TextView)view.findViewById(R.id.roomStartSt);
					TextView roomDestSt = (TextView)view.findViewById(R.id.roomDestSt);
					Intent intent = new Intent(RoomListActivity.this, RoomTopActivity.class);
					HashMap<String, String> roomInfo = new HashMap<String, String>();
					roomInfo.put("roomNumber", (String) roomNumber.getText());                         // Ex) 20141023102
					//roomDateTime.getText() Ex) 2014年10月23日(木) 22:48着
					roomInfo.put("roomDate", (String) roomDateTime.getText().subSequence(0, 14));      // Ex) 2014年10月23日(木)
					roomInfo.put("roomTime", (String) roomDateTime.getText().subSequence(15, 20));     // Ex) 22:48
					roomInfo.put("roomTimeType", (String) roomDateTime.getText().subSequence(20, 21)); // Ex) 着
					roomInfo.put("roomRailway", (String) roomRailway.getText());                       // Ex) 銀座線
					roomInfo.put("roomStartSt", (String) roomStartSt.getText());                       // Ex) 表参道
					roomInfo.put("roomDestSt", (String) roomDestSt.getText());                         // Ex) 渋谷
					roomInfo.put("endStTitle", rlb.getEndSt());                                        // Ex) 浅草
					roomInfo.put("trainTypeTitle", rlb.getTrainType());                                // Ex) 急行
					roomInfo.put("roomCarNum", String.valueOf(rlb.getCarNum()));                       // Ex) 3
					intent.putExtra("roomInfo", roomInfo);
					intent.putExtra("scene", "selectedStation");
					startActivity(intent);
				}
			});
		}

		Button newRoomBtn = (Button) findViewById(R.id.buttonNewRoom);
		newRoomBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RoomListActivity.this, MakeRoomActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	public void onPostExecute(ArrayAdapter<RoomBean> adapterRoom) {
		// TODO Auto-generated method stub
		int roomCount = adapterRoom.getCount();
		System.out.println("adapterRoomCount is " + roomCount);
		viewRoomList.setAdapter(adapterRoom);

		// 部屋がひとつしかない場合は何もせず遷移
//		if (roomCount == 1) {
//			int roomId = adapterRoom.getItem(0).getRoomId();
//			Intent intent = new Intent(this, RoomTopActivity.class);
//			intent.putExtra("roomId", roomId);
//			startActivity(intent);
//		}
	}
}
