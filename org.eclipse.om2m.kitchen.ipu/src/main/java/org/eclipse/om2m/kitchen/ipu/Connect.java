package org.eclipse.om2m.kitchen.ipu;

import java.io.*;
import java.net.*;

public class Connect implements Runnable {
	private Socket socket = null;
	private BufferedReader streamIn = null;
	private DataOutputStream streamOut = null;
	public Device device = null ;

	public Connect(String serverName, int serverPort, Device _device) throws IOException {
		socket = new Socket(serverName, serverPort);
		device = _device;
		start();
	}

	public void start() throws IOException {
		streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		device.firsthandle(streamIn.readLine());
	}

	public void run() {		
		while( (socket != null) && (Monitor.Device_Array.containsKey(device.getDeviceId())) )
		{
			try {
				device.handle(streamIn.readLine());
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void adjustDeliverTime(int t) throws IOException {
		streamOut.write(t);
		streamOut.flush();
	}

	public void stop() throws IOException {
		streamOut.writeInt(0);
		streamOut.flush();
		if(streamIn != null)  streamIn.close();
		if(streamOut != null)  streamOut.close();
		if(socket != null) socket.close();
	}
}
