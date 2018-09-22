package org.eclipse.om2m.kitchen.ipu;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.lang.Thread;

public class Device {
	
	Map<String, Sensor> Sensor_Array = new HashMap<String, Sensor>();
	private Connect client;
	private int deviceId;
	private String deviceName;
	private String ipAddress;
	private int port;
	private int debugDeviceCatchTime = 5;
	
	boolean debugflag = false;

	// constructor without parameter using for debug
	public Device(int _deviceId, String _deviceName, String _ipAddress, int _port, boolean _debugflag) {
		deviceId = _deviceId;
		deviceName = _deviceName;
		ipAddress = _ipAddress;
		port = _port;
		debugflag = _debugflag;
		
		if(debugflag) {
			DebugTest();
		} else {
			try {
				client = new Connect(ipAddress, port, this);
				Thread ConnectThread = new Thread(client);
				ConnectThread.start();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	/************** Debug code **************/

	void DebugTest() {
		new Thread() {
			public void run() {
				firsthandle(TestDataFirst());
				while(Activator.threadExecution && Monitor.Device_Array.containsKey(deviceId)) {
					handle(TestData());
					try {
						Thread.sleep(debugDeviceCatchTime * 1000);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	String TestDataFirst() {
		String line;
		if(deviceId == 0) {
			line = "Temperature celsius 24 0 100 "
				+ "humidity percent 90 0 100";
		} else if(deviceId == 1) {
			line = "CO ppm 35 0 100 "
				+ "Gas ppm 3 0 100";
		} else {
			line = "Smoke hsu 3 0 100";
		}
		return line;
	}

	String TestData() {
		String line;
		if(deviceId == 0) {
			line = "Temperature " + String.valueOf(24 + Math.random()) 
				+ " humidity " + String.valueOf(60 + Math.random()*5);
		} else if(deviceId == 1) {
			line = "CO " + String.valueOf(30 + Math.random()*10)
				+ " Gas " + String.valueOf(Math.random()*2);
		} else {
			line = "Smoke " + String.valueOf(Math.random()*4);
		}
		return line;
	}

	/****************************************/

	public void firsthandle(String line) {
		String[] lineArray = line.split(" ");
		for(int i=0; i<lineArray.length; i=i+5) {
			Sensor_Array.put(lineArray[i], new Sensor(lineArray[i],lineArray[i+1],lineArray[i+2], lineArray[i+3], lineArray[i+4], deviceId, deviceName));
		}
	}

	public void handle(String line) {
		String[] lineArray = line.split(" ");
		for(int i=0; i<lineArray.length; i=i+2) {
			Sensor_Array.get(lineArray[i]).setValue(lineArray[i+1]);
		}
	}
	
	public void setDeviceResponseTime(int time) throws IOException {
		client.adjustDeliverTime(time);
	}
	
	public int getAllSensorSize() {
		return Sensor_Array.size();
	}
	
	public int getDeviceId() {
		return deviceId;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public int getPort() {
		return port;
	}
	
	// called when Monitor.removeMyDevice is invoked
	public void stop() {
		try {
			Sensor_Array.clear();
			if( !debugflag ) {
				client.stop();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
