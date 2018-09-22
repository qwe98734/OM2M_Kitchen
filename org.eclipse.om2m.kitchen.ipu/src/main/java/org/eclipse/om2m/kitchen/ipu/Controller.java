package org.eclipse.om2m.kitchen.ipu;
 
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.*;
import org.eclipse.om2m.ipu.service.IpuService;

import java.io.*;

public class Controller implements IpuService {
	
	static String fileName = "DeviceList.txt";
	static int deviceCount = -1;
	
	public ResponseConfirm doExecute(RequestIndication requestIndication) {
		String[] parts = requestIndication.getTargetID().split("/");

		String appId;
		String deviceName;
		int deviceId;
		String ipAddress;
		int port;
		String func;
		String value;
		
		// add a new device into list
		if(parts.length == 8) {
			appId = parts[2];
			ipAddress = parts[4];
			port = Integer.parseInt(parts[5]);
			func = parts[6];
			deviceName = parts[7];  // device name
			
			if( !appId.equals("InitDevice") ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: appId " + appId + " need to be \"InitDevice\"");
			} else if( !func.equals("AddDevice") ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: func " + func + " need to be \"AddDevice\"");
			}
			return AddDevice(ipAddress, port, deviceName);
		}
		
		// methods only do control all devices
		if(parts.length == 6) {
			appId = parts[2];
			func = parts[4];
			value = parts[5];
			
			if( !appId.equals("InitDevice") ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: appId " + appId + " need to be \"InitDevice\"");
			}
			if( func.equals("Connect") && value.equals("true")) {
				return ConnectAll();
			}
			if( func.equals("Connect") && value.equals("false") ) {
				return DisConnectAll();
			}
			if( func.equals("Switch") ) {
				return SwitchAll(value);
			}
		}
		
		// methods do control one device or one sensor
		if(parts.length == 7) {
			appId = parts[2];
			deviceId = Integer.parseInt(parts[4]);
			func = parts[5];
			value = parts[6];
			
			// control one device
			if(appId.equals("InitDevice")) {
				if( func.equals("RemoveDevice") && value.equals("true") ) {
					return RemoveDevice(deviceId);
				}
				if( func.equals("Connect") && value.equals("true") ) {
					return Connect(deviceId);
				}
				if( func.equals("Connect") && value.equals("false") ) {
					return DisConnect(deviceId);
				}
				if( func.equals("Switch") ) {
					return SwitchDevice(deviceId, value);
				}
				if( func.equals("AdjustDeviceTime") ) {
					return AdjustDeviceTime(deviceId, value);
				}
			}
			// control one sensor
			else {
				// device has not been connected
				if( !Monitor.Device_Array.containsKey(deviceId) ) {
					return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: device " + String.valueOf(deviceId) + " may has not been connected");
				}
				
				Device myDevice = Monitor.Device_Array.get(deviceId);
				
				// sensor is not contained in the device
				if( !myDevice.Sensor_Array.containsKey(appId) ) {
					return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: application " + appId + " is not contained in device");
				}
				
				Sensor mySensor = myDevice.Sensor_Array.get(appId);
				
				if( func.equals("Switch")) {
					return SwitchSensor(mySensor, value);
				}
				if( func.equals("AdjustUploadTime")) {
					return AdjustUploadTime(mySensor, value);
				}
			}
		}
		return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "End");
	}
 
	public ResponseConfirm doRetrieve(RequestIndication requestIndication) {
		String[] parts = requestIndication.getTargetID().split("/");
		String appId;
		int deviceId;
		String content = "";
		
		if(parts.length == 4) {
			appId = parts[2];
			
			// get all devices in list
			if( appId.equals("InitDevice") ) {
				return GetAllDevices();
			}
			else {  // get one sensor data 
				for( Device myDevice : Monitor.Device_Array.values() ) {
					if( myDevice.Sensor_Array.containsKey(appId) ) {
						content = Mapper.getMySensorDataRep(myDevice.Sensor_Array.get(appId).getValue());
						return new ResponseConfirm(StatusCode.STATUS_OK, content);
					}
				}
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "appId " + appId + " may be wrong");
			}
		}
		
		if(parts.length == 5) {
			appId = parts[2];			
			
			// get sensors included in one device
			if( appId.equals("InitDevice") && Character.isDigit( parts[4].charAt(0) ) ) {
				deviceId = Integer.parseInt(parts[4]);
				return GetSensors(deviceId);
			}
			else if( appId.equals("InitDevice") && parts[4].equals("discovery") ) {
				try {
					Broadcast broadcast = new Broadcast();
					broadcast.start();
					
					while(broadcast.discoveryFlag) {					 
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					return new ResponseConfirm(StatusCode.STATUS_OK, "done");
				} catch(IOException e) {
					e.printStackTrace();
				}
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "error: discovery not succeed");
			}
			else if( parts[4].equals("dangerous") ) {  // get dangerous state about one sensor
				for( Device myDevice : Monitor.Device_Array.values() ) {
					if( myDevice.Sensor_Array.containsKey(appId) ) {
						content = Mapper.getMySensorDangerous(myDevice.Sensor_Array.get(appId).getDangerous());
						return new ResponseConfirm(StatusCode.STATUS_OK, content);
					}
				}
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "appId " + appId + " may be wrong");
			} 
		}
		
		if(parts.length == 6) {
			appId = parts[2];
			deviceId = Integer.parseInt(parts[4]);
			
			if( appId.equals("InitDevice") && parts[5].equals("Connect") ) {
				if(Monitor.Device_Array.containsKey(deviceId)) {
					return new ResponseConfirm(StatusCode.STATUS_OK, "true");
				}
				return new ResponseConfirm(StatusCode.STATUS_OK, "false");
			}
		}
		
		return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "End");
	}
 
	public ResponseConfirm doUpdate(RequestIndication requestIndication) {
		return new ResponseConfirm(StatusCode.STATUS_NOT_IMPLEMENTED,
				requestIndication.getMethod() + " not Implemented");
	}
 
	public ResponseConfirm doDelete(RequestIndication requestIndication) {
		String[] parts = requestIndication.getTargetID().split("/");
		String appId;
		String content = "";
		
		if(parts.length == 3) {
			appId = parts[2];
			content = appId + " has been deleted";
			
			return new ResponseConfirm(StatusCode.STATUS_OK, content);
		}
		return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "End");
	}
 
	public ResponseConfirm doCreate(RequestIndication requestIndication) {
		return new ResponseConfirm(StatusCode.STATUS_NOT_IMPLEMENTED,
			requestIndication.getMethod() + " not Implemented");
	}
	
	public String getAPOCPath(){
		return Sensor.APOCPATH;
	}
	
	public ResponseConfirm AddDevice(String ipAddress, int port, String deviceName) {
		try {
			String currentLine;
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			
			// check whether the list already has the same device
			while( (currentLine = in.readLine()) != null) {
				if(currentLine.contains(deviceName)) {
					in.close();
					return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: device name cannot be repeated in list");
				} else if(currentLine.contains(ipAddress) && currentLine.contains(String.valueOf(port))) {
					in.close();
					return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: Ip and port cannot be repeated in list");
				}
				
				// to catch the maximum deviceId in the list
				int tempId;
				if( (tempId = Integer.parseInt((currentLine.split(" "))[0])) > deviceCount ) {
					deviceCount = tempId;
				}
			}
			in.close();
			
		} catch(FileNotFoundException e) { // skip if the file does not exist
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
		
		// add the new device to the end of the list 
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
			++deviceCount;
			out.write(String.valueOf(deviceCount) + " " + deviceName + " " + ipAddress + " " + String.valueOf(port) + "\n");
			
			out.close();

			return new ResponseConfirm(StatusCode.STATUS_OK, "A new device is built");
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
	}
	
	public ResponseConfirm RemoveDevice(int deviceId) {
		String tempFileName = "DeviceList.temp";
		
		try {
			File inputFile = new File(fileName);
			File tempFile = new File(tempFileName);
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
			String currentLine;
			boolean checkHasDevice = false;
			
			while( (currentLine = in.readLine()) != null) {
				
				// find the device, do continue and do not write to the temp file
				if( (currentLine.split(" "))[0].equals(String.valueOf(deviceId)) ) {
					checkHasDevice = true;
					continue;
				}
				
				// not the device, do write to the temp file with new line symbol
				out.write(currentLine + System.getProperty("line.separator"));
			}
			
			out.close();
			in.close();
			
			// revise the tempfile name to the correct file name
			tempFile.renameTo(inputFile);
			
			// cannot find the device in list
			if( !checkHasDevice ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the specific device cannot be found in list");
			}			
			
			// the device has already been connected
			if( !Monitor.Device_Array.containsKey(deviceId) ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the specific device has already been disconnected");
			}
			
			Monitor.removeMyDevice(deviceId);
			
			return new ResponseConfirm(StatusCode.STATUS_OK, "An old deivce is removed");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the file does not exist");
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
	}
	
	public ResponseConfirm ConnectAll() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String currentLine;
			
			while( (currentLine = in.readLine()) != null ) {
				String[] parts = currentLine.split(" ");
				
				// the device has already been connected
				if(Monitor.Device_Array.containsKey(Integer.parseInt(parts[0]))) continue;
				
				// deviceId(int), deviceName(String), ipAddress(String), port(int)
				Monitor.createMyDevice(Integer.parseInt(parts[0]), parts[1], parts[2], Integer.parseInt(parts[3]));
			}
			in.close();
			
			return new ResponseConfirm(StatusCode.STATUS_OK, "All devices has been connected");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the file does not exist");
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
	}
	
	public ResponseConfirm DisConnectAll() {
		Monitor.removeAllDevice();
		return new ResponseConfirm(StatusCode.STATUS_OK, "All devices has disconnected");
	}

	public ResponseConfirm SwitchAll(String value) {
		if( !(value.equals("true") || value.equals("false")) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: value " + value + " must be true or false");
		}
		
		for( int deviceKey : Monitor.Device_Array.keySet() ) {
			Device myDevice = Monitor.Device_Array.get(deviceKey);
			for( String sensorKey : myDevice.Sensor_Array.keySet() ) {
				myDevice.Sensor_Array.get(sensorKey).setPower(Boolean.valueOf(value));
			}
		}
		
		if(value.equals("true")) {
			return new ResponseConfirm(StatusCode.STATUS_OK, "All devices turn on");
		} else {
			return new ResponseConfirm(StatusCode.STATUS_OK, "All devices turn off");
		}
	}

	public ResponseConfirm Connect(int deviceId) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String currentLine;
			boolean checkHasDevice = false;
			String deviceName = null;
			String ipAddress = null;
			int port = 0;
			
			while( (currentLine = in.readLine()) != null ) {
				String[] parts = currentLine.split(" ");
				
				// the device has been added and assign ip and port
				if(deviceId == Integer.parseInt(parts[0])) {
					checkHasDevice = true;
					deviceName = parts[1];
					ipAddress = parts[2];
					port = Integer.parseInt(parts[3]);
					break;
				}
			}
			in.close();
			
			// cannot find the device in list
			if( !checkHasDevice ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the specific device cannot be found in list");
			}
			
			// the device has already been connected
			if( Monitor.Device_Array.containsKey(deviceId) ) {
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the specific device has already been connected");
			}
			
			Monitor.createMyDevice(deviceId, deviceName, ipAddress, port);
			return new ResponseConfirm(StatusCode.STATUS_OK, "The device " + String.valueOf(deviceId) + " has been connected");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the file does not exist");
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
	}

	public ResponseConfirm DisConnect(int deviceId) {
		// the device has already been connected
		if( !Monitor.Device_Array.containsKey(deviceId) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: the specific device has already been disconnected");
		}
		
		Monitor.removeMyDevice(deviceId);
		return new ResponseConfirm(StatusCode.STATUS_OK, "The device has been disconnected");
	}

	public ResponseConfirm SwitchDevice(int deviceId, String value) {
		// value must be true or false
		if( !(value.equals("true") || value.equals("false")) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: value " + value + " must be true or false");
		}
		
		// device has not been connected
		if( !Monitor.Device_Array.containsKey(deviceId) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: device may has not been connected");
		}
		
		Device myDevice = Monitor.Device_Array.get(deviceId);
		for( String sensorKey : myDevice.Sensor_Array.keySet() ) {
			myDevice.Sensor_Array.get(sensorKey).setPower(Boolean.valueOf(value));
		}
		
		if(value.equals("true")) {
			return new ResponseConfirm(StatusCode.STATUS_OK, "The device turns on");
		} else {
			return new ResponseConfirm(StatusCode.STATUS_OK, "The device turns off");
		}
	}

	public ResponseConfirm SwitchSensor(Sensor mySensor, String value) {
		// value must be true or false
		if( !(value.equals("true") || value.equals("false")) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: value " + value + " must be true or false");
		}
		
		mySensor.setPower(Boolean.valueOf(value));
		
		if(value.equals("true")) {
			return new ResponseConfirm(StatusCode.STATUS_OK);
		} else {
			return new ResponseConfirm(StatusCode.STATUS_OK);
		}
	}

	public ResponseConfirm AdjustUploadTime(Sensor mySensor, String value) {
		mySensor.setUploadTime(Integer.parseInt(value));
		return new ResponseConfirm(StatusCode.STATUS_OK, "The sensor upload time has changed to " + value);
	}

	public ResponseConfirm AdjustDeviceTime(int deviceId, String value) {
		// device has not been connected
		if( !Monitor.Device_Array.containsKey(deviceId) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: device may has not been connected");
		}
		
		Device myDevice = Monitor.Device_Array.get(deviceId);
		
		try {
			myDevice.setDeviceResponseTime(Integer.parseInt(value));
			return new ResponseConfirm(StatusCode.STATUS_OK, "The device has been set a new time");
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: connection between device and gscl may be error");
		}
	}

	public ResponseConfirm GetAllDevices() {
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader in = new BufferedReader(fileReader);
			String currentLine;
			String content = "";
			
			while( (currentLine = in.readLine()) != null ) {
				content += currentLine + System.getProperty("line.separator");
			}
			in.close();
			
			return new ResponseConfirm(StatusCode.STATUS_OK, content);
			
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file does not exist");
		} catch(IOException e) {
			e.printStackTrace();
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file error");
		}
	}
	
	public ResponseConfirm GetSensors(int deviceId) {
		if( !Monitor.Device_Array.containsKey(deviceId) ) {
			return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: device Id does not exist");
		}
		
		String content = "";
		Device myDevice = Monitor.Device_Array.get(deviceId);
		for(String index : myDevice.Sensor_Array.keySet()) {
			content += index + ",";
		}
		content = content.substring(0, content.length()-1);
		return new ResponseConfirm(StatusCode.STATUS_OK, content);
	}
}
