package com.railway.bean;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

/*
 * 日時を表すBean
 * 日付・時間をまとめて扱う際に使用
 */
public class DateTimeBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private int year;   // yyyy
	private int month;  // mm
	private int date;   // dd
	private int dayOfWeek;
	private int hour;   // HH
	private int minute; // MM
	
	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getMonth() {
		return String.format("%1$02d", month);
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public String getDate() {
		return String.format("%1$02d", date);
	}

	public void setDate(int date) {
		this.date = date;
	}

	public int getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public String getHour() {
		return String.format("%1$02d", hour);
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public String getMinute() {
		return String.format("%1$02d", minute);
	}

	public void setMinute(int minute) {
		this.minute = minute;
	}

}
