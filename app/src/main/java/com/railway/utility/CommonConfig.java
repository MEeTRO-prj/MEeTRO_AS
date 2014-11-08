package com.railway.utility;

public class CommonConfig {
	private final static String PROJECT_ID = "1087834224512";
	private final static String API_URL = "https://api.tokyometroapp.jp/api/v2/datapoints";
	private final static String ACCESS_KEY = "73741d6e16e6fe8e134199b0dfd60d1124889c3c79e768230db72992ec545fee";
	private final static String SERVER_URL = "http://kt-kiyoshi.com/MEeTRO_server/";
	private final static String GoogleApiKey = "AIzaSyAT3tjCuqdrxfsE9yrDLkzqncZhhEsSvVc";

	public static String getPROJECT_ID() {
		return PROJECT_ID;
	}
	public static String getACCESS_KEY() {
		return ACCESS_KEY;
	}
	public static String getAPI_URL() {
		return API_URL;
	}
	public static String getSERVER_URL() {
		return SERVER_URL;
	}
	public static String getGoogleApiKey() {
		return GoogleApiKey;
	}
}
