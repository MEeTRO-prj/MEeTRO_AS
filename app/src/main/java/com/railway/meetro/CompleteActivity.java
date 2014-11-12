package com.railway.meetro;

import java.util.HashMap;

import com.railway.bean.StationApiBean;
import com.railway.controller.MakeNewRoom;
import com.railway.utility.CommonMethod;
import com.railway.utility.DrawerItemClickListener;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.ClipboardManager;

/*
 * 部屋を作るフローの最終画面
 */
public class CompleteActivity extends ActionBarActivity {
	private final static String TAG = "CompleteActivity";
	private Context context;

	SharedPreferences sp;

	public int userId;
	public String userName;

	private String railwayId;
	private String railway;
	private StationApiBean startSt;
	private StationApiBean destSt;
	private String endStTitle;
	private String trainTypeTitle;
	private int timeType;
	private String rideDate;
	private String decideTime;
	private int carNum;
	private String direction;

	// NavigationDrawer
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.complete);
		context = this.getApplicationContext();

		final TextView roomUrl = (TextView) findViewById(R.id.roomUrl);
		Button copyBtn = (Button) findViewById(R.id.copyUrl);
		Button lineBtn = (Button) findViewById(R.id.sendByLine);
		Button enterBtn = (Button) findViewById(R.id.enterRoom);

		// DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		setupNavigationDrawer();

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		userId = sp.getInt("userId", 0);
		userName = sp.getString("userName", null);

		// intentから指定キーのカスタムクラスを取得する
		railwayId = getIntent().getStringExtra("railwayId");                    // Ex) odpt:Railway.TokyoMetro.Ginza
		railway = getIntent().getStringExtra("railway");                        // Ex) 銀座線
		startSt = (StationApiBean) getIntent().getSerializableExtra("startSt"); // StationBean
		destSt = (StationApiBean) getIntent().getSerializableExtra("destSt");   // StationBean
		endStTitle = getIntent().getStringExtra("endStTitle");                  // Ex) 浅草
		trainTypeTitle = getIntent().getStringExtra("trainTypeTitle");          // Ex) 急行
		rideDate = getIntent().getStringExtra("rideDate");                      // Ex) 2014-10-23
		decideTime = getIntent().getStringExtra("decideTime");                  // Ex) 23:15
		timeType = getIntent().getIntExtra("timeType", 0);                      // 0 or 1
		carNum = getIntent().getIntExtra("carNum", 0);                          // Ex) 5
		direction = getIntent().getStringExtra("direction");                    // Ex) odpt.RailDirection:TokyoMetro.Asakusa

		// Server上にそれぞれの情報を送り、PHPでDBにROOMとMEMBERを作る（非同期タスク）
		new MakeNewRoom(this, roomUrl).execute(
				String.valueOf(userId), rideDate, decideTime, 
				String.valueOf(timeType), railwayId, startSt.getSameAs(), 
				destSt.getSameAs(), endStTitle, trainTypeTitle, String.valueOf(carNum), direction
				);

		// URLクリックでブラウザ起動してみる
		roomUrl.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Uri uri = Uri.parse(roomUrl.getText().toString());
				Intent intent = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(intent);
			}
		});

		// クリップボードにコピー
		copyBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String copyUrl = roomUrl.getText().toString();

				ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData.Item item = new ClipData.Item(copyUrl);
				String[] mimeTypes = new String[] {
						ClipDescription.MIMETYPE_TEXT_PLAIN
				};
				ClipData clip = new ClipData("data", mimeTypes, item);
				clipboardManager.setPrimaryClip(clip);
				Toast.makeText(CompleteActivity.this, "URL: " + copyUrl + "をコピーしました。", Toast.LENGTH_SHORT).show();
			}
		});

		// LINEで送る
		lineBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String lineUrl = "http://line.me/R/msg/text/?";
				Uri uri = Uri.parse(lineUrl + roomUrl.getText().toString());
				Intent intent = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(intent);
			}
		});

		// 部屋に入る
		enterBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final Intent intent = new Intent(CompleteActivity.this, RoomTopActivity.class);
				intent.putExtra("scene", "fromComplete");
				int roomId = sp.getInt("roomId", 0);
				intent.putExtra("room_id", roomId); // Ex) 110

				// 作成した部屋の情報をintentにputしたい
				HashMap<String, String> roomInfo = new HashMap<String, String>();
				String roomNumber = (rideDate + roomId).replace("-", ""); // Ex) 2014-10-24110 -> 20141024110
				roomInfo.put("roomNumber", roomNumber);                   // Ex) 20141024110
				// 曜日を取得 -> Ex) (木)
				String dayOfWeek = CommonMethod.changeDayOfWeek(
						Integer.parseInt(rideDate.substring(0, 4))
						, Integer.parseInt(rideDate.substring(5, 7))
						, Integer.parseInt(rideDate.substring(8, 10)));
				// Ex) 2014-10-23 -> 2014年10月23日(木)
				String roomDate = rideDate.substring(0, 4)
						+ "年" + rideDate.substring(5, 7)
						+ "月" + rideDate.substring(8, 10)
						+ "日" + dayOfWeek;
				roomInfo.put("roomDate", roomDate);                 // Ex) 2014年10月23日(木)
				roomInfo.put("roomTime", decideTime);               // Ex) 09:10
				String roomTimeType = "";
				if(timeType == 0){
					roomTimeType = "発";
				} else {
					roomTimeType = "着";
				}
				roomInfo.put("roomTimeType", roomTimeType);         // Ex) 発, 着
				roomInfo.put("roomRailway", railway);               // Ex) 銀座線
				roomInfo.put("roomStartSt", startSt.getTitle());    // Ex) 渋谷
				roomInfo.put("roomDestSt", destSt.getTitle());      // Ex) 青山一丁目
				roomInfo.put("endStTitle", endStTitle);             // Ex) 浅草
				roomInfo.put("trainTypeTitle", trainTypeTitle);     // Ex) 急行
				roomInfo.put("roomCarNum", String.valueOf(carNum)); // Ex) 3
				intent.putExtra("roomInfo", roomInfo);
				startActivity(intent);
				CompleteActivity.this.finish();
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
