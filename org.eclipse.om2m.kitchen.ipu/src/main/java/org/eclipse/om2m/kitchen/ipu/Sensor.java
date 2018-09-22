package org.eclipse.om2m.kitchen.ipu;

public class Sensor {
	
	public final static String APOCPATH = "sensors";
	public final static String TYPE = "Sensor";
	private String type;
	private String value;
	private String unit;
	private String dangerousValue;
	private String deviceName;
	private String dangerous;
	private String minValue;
	private String maxValue;
	private int deviceId;
	private boolean power;
	private int uploadTime;  // time(s)

	public Sensor(String _type, String _unit, String _dangerousValue, String _minValue, String _maxValue,int _deviceId, String _deviceName) {
		type = _type;
		dangerousValue = _dangerousValue;
		unit = _unit;
		dangerous = "false";
		minValue = _minValue;
		maxValue = _maxValue;
		deviceId = _deviceId;
		deviceName = _deviceName;
		power = true;
		uploadTime = 5*60;
	}

	public String getType() {
		return type;
	}
	
	public void setType(String _type) {
		type = _type;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String _value) {
		value = _value;
		checkDangerous();
	}
	
	public String getUnit() {
		return unit;
	}
	
	public void setUnit(String _unit) {
		unit = _unit;
	}
	
	public String getMinValue() {
		return minValue;
	}
	
	public void setMinValue(String _minValue) {
		minValue = _minValue;
	}
	
	public String getMaxValue() {
		return maxValue;
	}
	
	public void setMaxValue(String _maxValue) {
		maxValue = _maxValue;
	}

	public void setDangerousValue(String _dangerousValue) {
		dangerousValue = _dangerousValue;
	}
	
	public String getDangerousValue() {
		return dangerousValue;
	}

	
	
	public String getDangerous() {
		return dangerous;
	}
	
	public void checkDangerous() {
		if(Double.parseDouble(this.value) > Double.parseDouble(this.dangerousValue))
			this.dangerous = "true";
		else
			this.dangerous = "false";
	}
	
	public boolean getPower() {
		return power;
	}
	
	public void setPower(boolean _power) {
		power = _power;
	}
	
	public int getDeviceId() {
		return deviceId;
	}
	
	public void setDeviceId(int _deviceId) {
		deviceId = _deviceId;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public void setDeviceName(String _deviceName) {
		deviceName = _deviceName;
	}
	
	public int getUploadTime() {
		return uploadTime;
	}
	
	public void setUploadTime(int _uploadTime) {
		uploadTime = _uploadTime;
	}
	
}
