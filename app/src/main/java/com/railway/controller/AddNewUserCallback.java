package com.railway.controller;

import java.util.List;

import com.railway.bean.RoomBean;
import android.widget.ArrayAdapter;

public interface AddNewUserCallback {
	 public void onPostExecute(String userId, String userName);
}
