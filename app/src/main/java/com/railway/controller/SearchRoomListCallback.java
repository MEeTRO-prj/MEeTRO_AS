package com.railway.controller;

import com.railway.bean.RoomBean;
import android.widget.ArrayAdapter;

public interface SearchRoomListCallback {
	 public void onPostExecute(ArrayAdapter<RoomBean> result);
}
