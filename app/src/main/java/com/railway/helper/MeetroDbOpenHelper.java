package com.railway.helper;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * アプリ内DBの生成を行う
 */
public class MeetroDbOpenHelper extends SQLiteOpenHelper {
	private static final String TAG = "MeetroDbOpenHelper";

	static final String DATABASE_NAME = "MEETRO";
	static final int DATABASE_VERSION = 1;

	public MeetroDbOpenHelper(Context context) {
		// データベースファイル名とバージョンを指定しSQLiteOpenHelperクラスを初期化
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d(TAG, "MeetroDbOpenHelperのコンストラクタが呼ばれました");
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		Log.d(TAG, "MeetroDbHelper.onCreateが呼ばれました");
		// USERテーブルを作成
		database.execSQL(
				"CREATE TABLE USER ("
						+ "_id INTEGER PRIMARY KEY, "
						+ "REGI_ID INTEGER NOT NULL, "
						+ "USER_NAME TEXT NOT NULL"
						+ ");"
				);
		// RAILWAYテーブルを作成
		database.execSQL(
				"CREATE TABLE RAILWAY ("
						+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "RAILWAY_ID TEXT NOT NULL, "
						+ "RAILWAY_NAME TEXT NOT NULL, "
						+ "CAR_NUM INTEGER, "
						+ "DIRECTION_1 TEXT NOT NULL, "
						+ "DIRECTION_2 TEXT NOT NULL"
						+ ");"
				);

		// デフォルトデータの挿入
		ContentValues values = new ContentValues();
		String[] idArray = {"odpt.Railway:TokyoMetro.Ginza",
				"odpt.Railway:TokyoMetro.Marunouchi",
				"odpt.Railway:TokyoMetro.Hibiya",
				"odpt.Railway:TokyoMetro.Tozai",
				"odpt.Railway:TokyoMetro.Chiyoda",
				"odpt.Railway:TokyoMetro.Yurakucho",
				"odpt.Railway:TokyoMetro.Hanzomon",
				"odpt.Railway:TokyoMetro.Namboku",
		"odpt.Railway:TokyoMetro.Fukutoshin"};
		String[] nameArray = {"銀座線","丸ノ内線","日比谷線","東西線","千代田線","有楽町線","半蔵門線","南北線","副都心線"};
		int[] carArray = {6, 6, 6, 10, 6, 10, 10, 6, 10};
		String[] dir1Array = {
				"odpt.RailDirection:TokyoMetro.Shibuya", // 銀座線
				"odpt.RailDirection:TokyoMetro.Ogikubo", // 丸ノ内線
				"odpt.RailDirection:TokyoMetro.NakaMeguro", // 日比谷線
				"odpt.RailDirection:TokyoMetro.Nakano", // 東西線
				"odpt.RailDirection:TokyoMetro.YoyogiUehara", // 千代田線
				"odpt.RailDirection:TokyoMetro.Wakoshi", // 有楽町線
				"odpt.RailDirection:TokyoMetro.Shibuya", // 半蔵門線
				"odpt.RailDirection:TokyoMetro.Meguro", // 南北線
				"odpt.RailDirection:TokyoMetro.Wakoshi" // 副都心線
				};
		String[] dir2Array = {
				"odpt.RailDirection:TokyoMetro.Asakusa", // 銀座線
				"odpt.RailDirection:TokyoMetro.Ikebukuro", // 丸ノ内線
				"odpt.RailDirection:TokyoMetro.KitaSenju", // 日比谷線
				"odpt.RailDirection:TokyoMetro.NishiFunabashi", // 東西線
				"odpt.RailDirection:TokyoMetro.Ayase", // 千代田線
				"odpt.RailDirection:TokyoMetro.ShinKiba", // 有楽町線
				"odpt.RailDirection:TokyoMetro.Oshiage", // 半蔵門線
				"odpt.RailDirection:TokyoMetro.AkabaneIwabuchi", // 南北線
				"odpt.RailDirection:TokyoMetro.Shibuya" // 副都心線
				};
		for(int i = 0; i < idArray.length; i++) {
			values.put("RAILWAY_ID", idArray[i]);
			values.put("RAILWAY_NAME", nameArray[i]);
			values.put("CAR_NUM", carArray[i]);
			values.put("DIRECTION_1", dir1Array[i]);
			values.put("DIRECTION_2", dir2Array[i]);
			database.insert("RAILWAY", null, values);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "UserDbHelper.onUpgaradeが呼ばれました");
		// USERテーブルを再定義するため現在のテーブルを削除
		db.execSQL("DROP TABLE IF EXISTS USER");
		db.execSQL("DROP TABLE IF EXISTS RAILWAY");
		onCreate(db);
	}

	public void insertLabel(String label, String table){
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put("USER_NAME", label);

		// Inserting Row
		db.insert(table, null, values);
		db.close(); // Closing database connection
	}

	public List<String> getAllLabels(String table){
		List<String> labels = new ArrayList<String>();

		// Select All Query
		String selectQuery = "SELECT  * FROM " + table;

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				labels.add(cursor.getString(1));
			} while (cursor.moveToNext());
		}

		// closing connection
		cursor.close();
		db.close();

		// returning lables
		return labels;
	}

}
