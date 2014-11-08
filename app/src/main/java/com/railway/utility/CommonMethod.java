package com.railway.utility;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.widget.TextView;

/*
 * いろいろと便利なメソッドの集合
 */
public class CommonMethod {
	// 数値整形
	public static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

	// 日付選択
	public static void updateDisplay(TextView view, int year, int month, int day) {
		view.setText(
				new StringBuilder()
				.append(pad(year)).append("/")
				.append(pad(month + 1)).append("/")
				.append(pad(day)));
	}
	// 時間選択
	public static void updateDisplay(TextView view, int hour, int minute) {
		view.setText(
				new StringBuilder()
				.append(pad(hour)).append(":")
				.append(pad(minute)));
	}
	
	/*
	 * dayOfWeekを(日)などに変換するメソッド
	 * params: dayOfWeek
	 * return: day_of_week
	 */
	public static String changeDayOfWeek(int dayOfWeek) {
		String day_of_week = "";
		switch(dayOfWeek) {
		case Calendar.SUNDAY: day_of_week = "(日)"; break;
		case Calendar.MONDAY: day_of_week = "(月)";break;
		case Calendar.TUESDAY: day_of_week = "(火)";break;
		case Calendar.WEDNESDAY: day_of_week = "(水)";break;
		case Calendar.THURSDAY: day_of_week = "(木)";break;
		case Calendar.FRIDAY: day_of_week = "(金)";break;
		case Calendar.SATURDAY: day_of_week = "(土)"; break;
		}
		return day_of_week;
	}

	/*
	 * yyyymmddを(日)などに変換するメソッド
	 * params: yyyy,mm,dd
	 * return: day_of_week
	 */
	public static String changeDayOfWeek(int year, int month, int date) {
		Calendar calen = new GregorianCalendar(year, month, date);
		String day_of_week = "";
		switch(calen.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.SUNDAY: day_of_week = "(日)"; break;
		case Calendar.MONDAY: day_of_week = "(月)";break;
		case Calendar.TUESDAY: day_of_week = "(火)";break;
		case Calendar.WEDNESDAY: day_of_week = "(水)";break;
		case Calendar.THURSDAY: day_of_week = "(木)";break;
		case Calendar.FRIDAY: day_of_week = "(金)";break;
		case Calendar.SATURDAY: day_of_week = "(土)"; break;
		}
		return day_of_week;
	}
}
