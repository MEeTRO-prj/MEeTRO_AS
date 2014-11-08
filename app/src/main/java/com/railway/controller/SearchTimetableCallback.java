package com.railway.controller;

import android.widget.ArrayAdapter;

public interface SearchTimetableCallback {
	 public void onPostExecute(ArrayAdapter<String> adapterTime);
}
