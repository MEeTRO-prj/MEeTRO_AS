package com.railway.controller;

import java.util.List;

import com.railway.bean.RoomBean;
import android.widget.ArrayAdapter;

public interface SearchRoomCallback {
	 public void onPostExecute(RoomBean result);
	 public void onPostExecute(String[] result);
}
