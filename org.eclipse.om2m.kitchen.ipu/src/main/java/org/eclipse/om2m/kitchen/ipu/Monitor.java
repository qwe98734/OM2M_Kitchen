package org.eclipse.om2m.kitchen.ipu;
 
import org.eclipse.om2m.commons.resource.*;
import org.eclipse.om2m.commons.rest.*;
import org.eclipse.om2m.core.service.SclService;

import java.lang.Thread;
import java.util.Map;
import java.util.HashMap;
 
public class Monitor {
	static SclService core;
	static String sclId = System.getProperty("org.eclipse.om2m.sclBaseId", "");
	static String reqEntity = System.getProperty("org.eclipse.om2m.adminRequestingEntity", "");
	
	/*********************My Code*********************/
	
	static Map<Integer, Device> Device_Array;  // deviceId match to device
	
	
	/*************************************************/
 
	public Monitor(SclService sclService) {
		core = sclService;
		Device_Array = new HashMap<Integer, Device>();
	}
 
	public void start() {
		createInitDeviceResources("InitDevice", Sensor.APOCPATH);
	}
	
	public void createInitDeviceResources(String appId, String aPoCPath) {
		String targetId;
 
        // Create the InitDevice application
		targetId = sclId + "/applications";
		core.doRequest(new RequestIndication("CREATE", targetId, reqEntity, new Application(appId, aPoCPath)));
	}
	
	public static void createMyDevice(int deviceId, String deviceName, String ipAddress, int port) {
		
		// create a new device and put into HashMap
		Device myDevice = new Device(deviceId, deviceName, ipAddress, port, false);
		
		// create a device for debugging
		//Device myDevice = new Device(deviceId, deviceName, ipAddress, port, true);
		
		Device_Array.put(deviceId, myDevice);
		
		// Use loop to limit if it doesn't load any sensor
		while(myDevice.getAllSensorSize()==0) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// create every sensor resources 
		int myDeviceId = myDevice.getDeviceId();  // used to check device is existed
		for(String mysensorId : myDevice.Sensor_Array.keySet()) {
			Sensor mySensor = myDevice.Sensor_Array.get(mysensorId);
    		createMySensorResources(mysensorId, Sensor.APOCPATH, mySensor);
    		listenToMySensor(mysensorId, mySensor, myDeviceId);
		}
	}

	public static void removeMyDevice(int deviceId) {
		Device myDevice = Device_Array.remove(deviceId);
		if(myDevice != null) {
			myDevice.stop();
		}
	}
	
	public static void removeAllDevice() {
		for(int key : Device_Array.keySet()) {
			Device myDevice = Device_Array.remove(key);
			myDevice.stop();
		}
	}
	
	public static void createMySensorResources(String appId, String aPoCPath, Sensor mySensor) {
		String targetId, content;
 
                // Create the SENSOR application
		targetId = sclId + "/applications";
		ResponseConfirm response = core.doRequest(new RequestIndication("CREATE", targetId, reqEntity, new Application(appId, aPoCPath)));
		
		if (response.getStatusCode().equals(StatusCode.STATUS_CREATED)) {                        
			// Create the "DESCRIPTOR" container
			targetId = sclId + "/applications/" + appId + "/containers";
			core.doRequest(new RequestIndication("CREATE", targetId, reqEntity, new Container("DESCRIPTOR")));
 
                         // Create the "DATA" container
			core.doRequest(new RequestIndication("CREATE", targetId, reqEntity, new Container("DATA")));
 
                         // Create the description contentInstance
			content = Mapper.getMySensorDescriptorRep(sclId, appId, aPoCPath, mySensor);
			targetId = sclId + "/applications/" + appId + "/containers/DESCRIPTOR/contentInstances";
			core.doRequest(new RequestIndication("CREATE", targetId, reqEntity, new ContentInstance(content.getBytes())));
		}
	}
	
	public static void listenToMySensor(final String mysensorId, final Sensor mySensor, final int myDeviceId) {
		new Thread() {
			public void run() {
				while (Activator.threadExecution && Monitor.Device_Array.containsKey(myDeviceId)) {
					// whether switch is on or off which is controlled in Controller.java
					while( !(mySensor.getPower()) ) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
                    // Create a data contentInstance
					String content = Mapper.getMySensorDataRep(mySensor.getValue());
					String targetID = sclId + "/applications/" + mysensorId + "/containers/DATA/contentInstances";
					core.doRequest(new RequestIndication("CREATE", targetID, reqEntity, new ContentInstance(content.getBytes())));
                                        
					// Wait for uploadTime then loop
					try {
						Thread.sleep(mySensor.getUploadTime() * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}
