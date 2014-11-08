package com.railway.bean;

import java.io.Serializable;

/*
 * 部屋を表すBean
 * DBのROOMテーブルに相当
 */
public class RoomBean implements Serializable {
	                            // --Example--
	private int roomId;         // 12
	private int ownerId;        // 86
	private String rideDate;    // 2014-10-20
	private String rideTime;    // 08:21:00
	private int timeType;       // 1(0:出発, 1:到着)
	private String railwayId;   // odpt.Railway:TokyoMetro.Hanzomon
	private String rideSt;      // odpt.Station:TokyoMetro.Hanzomon.Nagatacho
	private String destSt;      // odpt.Station:TokyoMetro.Hanzomon.KiyosumiShirakawa
	private String endSt;       // 浅草
	private String trainType;   // 急行
	private int carNum;         // 1
	private String trainNumber; // A1104
	private int useFlg;         // 0
	
	public int getRoomId() {
		return roomId;
	}
	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}
	public int getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}
	public String getRideDate() {
		return rideDate;
	}
	public void setRideDate(String rideDate) {
		this.rideDate = rideDate;
	}
	public String getRideTime() {
		return rideTime;
	}
	public void setRideTime(String rideTime) {
		this.rideTime = rideTime;
	}
	public int getTimeType() {
		return timeType;
	}
	public void setTimeType(int timeType) {
		this.timeType = timeType;
	}
	public String getRailwayId() {
		return railwayId;
	}
	public void setRailwayId(String railwayId) {
		this.railwayId = railwayId;
	}
	public String getRideSt() {
		return rideSt;
	}
	public void setRideSt(String rideSt) {
		this.rideSt = rideSt;
	}
	public String getDestSt() {
		return destSt;
	}
	public void setDestSt(String destSt) {
		this.destSt = destSt;
	}
	public String getEndSt() {
		return endSt;
	}
	public void setEndSt(String endSt) {
		this.endSt = endSt;
	}
	public String getTrainType() {
		return trainType;
	}
	public void setTrainType(String trainType) {
		this.trainType = trainType;
	}
	public int getCarNum() {
		return carNum;
	}
	public void setCarNum(int carNum) {
		this.carNum = carNum;
	}
	public String getTrainNumber() {
		return trainNumber;
	}
	public void setTrainNumber(String trainNumber) {
		this.trainNumber = trainNumber;
	}
	public int getUseFlg() {
		return useFlg;
	}
	public void setUseFlg(int useFlg) {
		this.useFlg = useFlg;
	}
}
