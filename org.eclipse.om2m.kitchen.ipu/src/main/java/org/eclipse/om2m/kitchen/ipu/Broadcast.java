package org.eclipse.om2m.kitchen.ipu;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Broadcast extends Thread {
	/** SclBase ip address. */
	protected final String SCL_IP = Activator.GetLocalIP();
	protected DatagramSocket socket = null;
	protected Map<String, String> connectData = new HashMap<String, String>();
	protected boolean discoveryFlag = true;
	protected int discoveryTime = 2;
	protected int devicePort = 50000;
	
	public Broadcast() throws IOException {
		this("BroadcastThread");
    }

    public Broadcast(String name) throws IOException {
        super(name);
        socket = new DatagramSocket(49153);
        
        try {
        	socket.setBroadcast(true);
        } catch(SocketException e) {
        	e.printStackTrace();
        }
    }
    
    public void run() {
    	
    	// thread used for receiving packet from recipients 
    	new Thread() {
    		public void run() {
    			CleanList();
    			
    			byte[] buf = new byte[1000];
    			DatagramPacket p = new DatagramPacket(buf, buf.length);
    			while(discoveryFlag) {  // discovery till finisihing discovery and waiting 30s
    				try {
    					socket.receive(p);
    					
    					String senderContent = new String(p.getData(), StandardCharsets.UTF_8).trim();
    					String senderIp = p.getAddress().getHostAddress();
    					String senderPort = String.valueOf(p.getPort());
    					AddDevice(senderIp, senderPort, senderContent);
    					//connectData.put(senderIp, senderPort);
    				} catch(SocketException e) {
    					System.out.println("Socket closed, stop receieving");
    				} catch(Exception e) {
    					e.printStackTrace();
    				}
    			}
    		}
    	}.start();
    	
    	// start broadcasting depending on serverIp
        try {
        	byte[] buf = new byte[256];
        	InetAddress group;
        	String[] partsIP = SCL_IP.split("\\.");  // split IP by "."
        	System.out.println("Server IP : " + SCL_IP);
        	
		    // make packet content
		    String dString = "AC";
		    buf = dString.getBytes();
		    
		    String address;
		    if(partsIP[0].equals("192") && partsIP[1].equals("168")) {
			    for(int i=1; i<255; ++i) {
			    	address = partsIP[0] + "." + partsIP[1] + "." + partsIP[2]+ "." + String.valueOf(i);
			    	group = InetAddress.getByName(address);
					DatagramPacket packet = new DatagramPacket(buf, buf.length, group, devicePort);
					socket.send(packet);
			    }
		    }
		    else if(partsIP[0].equals("172")) {
			    for(int i=1; i<255; ++i) {
			    	for(int j=1; j<255; ++j) {
				    	address = partsIP[0] + "." + partsIP[1] + "." +String.valueOf(i)+ "." + String.valueOf(j);
				    	group = InetAddress.getByName(address);
						DatagramPacket packet = new DatagramPacket(buf, buf.length, group, devicePort);
						socket.send(packet);
			    	}
			    }
		    }
		    else if(partsIP[0].equals("10")) {
			    for(int i=1; i<255; ++i) {
			    	for(int j=1; j<255; ++j) {
			    		for(int k=1; k<255; ++k) {
					    	address = partsIP[0] + "." + String.valueOf(i) + "." + String.valueOf(j)+ "." + String.valueOf(k);
					    	group = InetAddress.getByName(address);
							DatagramPacket packet = new DatagramPacket(buf, buf.length, group, devicePort);
							socket.send(packet);
			    		}
			    	}
			    }
		    }
				
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        // sleep for discoveryTime and stop receiving packet from recipients
        try {
	    	sleep(discoveryTime * 1000);
	    	discoveryFlag = false;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		}
        socket.close();
    }
    
    public void AddDevice(String ipAddress, String port, String deviceName) {
		try {
			String currentLine;
			BufferedReader in = new BufferedReader(new FileReader(Controller.fileName));
			
			// check whether the list already has the same device
			while( (currentLine = in.readLine()) != null) {
				if(currentLine.contains(deviceName)) {
					in.close();
					return;
					//return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: device name cannot be repeated in list");
				} else if(currentLine.contains(ipAddress) && currentLine.contains(port)) {
					in.close();
					return;
					//return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: Ip and port cannot be repeated in list");
				}
			}
			in.close();
			
		} catch(FileNotFoundException e) { // skip if the file does not exist
		} catch(IOException e) {
			e.printStackTrace();
			return;
			//return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
		
		// add the new device to the end of the list 
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(Controller.fileName, true));
			++Controller.deviceCount;
			out.write(String.valueOf(Controller.deviceCount) + " " + deviceName + " " + ipAddress + " " + port + "\n");
			
			out.close();
			
			return;
			//return new ResponseConfirm(StatusCode.STATUS_OK, "A new device is built");
		} catch(IOException e) {
			e.printStackTrace();
			return;
			//return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Error: file IO error");
		}
	}
    
    public void CleanList() {
    	try {
    		Controller.deviceCount = -1;
    		BufferedWriter out = new BufferedWriter(new FileWriter(Controller.fileName, false));
    		for( int deviceId : Monitor.Device_Array.keySet() ) {
    			Device myDevice = Monitor.Device_Array.get(deviceId);    			
    			out.write( String.valueOf(deviceId) + " " + myDevice.getDeviceName() + " " 
    			+ myDevice.getIpAddress() + " " + String.valueOf(myDevice.getPort()) + "\n" );
    			
    			if(deviceId > Controller.deviceCount) {
    				Controller.deviceCount = deviceId;
    			}
    		}
    		out.close();
    	} catch(IOException e) {
    		e.printStackTrace();
    		return;
    	}
    }
}
