package org.eclipse.om2m.kitchen.ipu;

import java.net.*;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.core.service.SclService;
import org.eclipse.om2m.ipu.service.IpuService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private static Log logger = LogFactory.getLog(Activator.class);
	private ServiceTracker<Object, Object> sclServiceTracker;
	protected static boolean threadExecution = true;
	protected String localIP_IPV4 = "127.0.0.1";
 
    // Activate the plugin
	public void start(BundleContext context) throws Exception {
		logger.info("IPU started");
 
                // Register the IPU Controller service
		logger.info("Register IpuService..");
		context.registerService(IpuService.class.getName(), new Controller(), null);
		logger.info("IpuService is registered.");
 
                // Track the CORE SCL service
		sclServiceTracker = new ServiceTracker<Object, Object>(context,SclService.class.getName(), null) {
			public void removedService(ServiceReference<Object> reference, Object service) {
				logger.info("SclService removed");
			}
 
			public Object addingService(ServiceReference<Object> reference) {
				logger.info("SclService discovered");
				SclService sclService = (SclService) this.context.getService(reference);
				final Monitor IpuMonitor = new Monitor(sclService);

				new Thread() {
					public void run() {
						try {
							threadExecution = true;
							IpuMonitor.start();
						} catch (Exception e) {
							logger.error("IpuMonitor error", e);
						}
					}
				}.start();
				return sclService;
			}
		};
		sclServiceTracker.open();
	}
 
        // Deactivate the plugin
	public void stop(BundleContext context) throws Exception {
		logger.info("IPU stopped");
		threadExecution = false;
		Monitor.Device_Array.clear();
	}
	
	public static String GetLocalIP() throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while(interfaces.hasMoreElements()) {
			NetworkInterface current = interfaces.nextElement();
			
			if( !current.isUp() || current.isLoopback() || current.isVirtual() ) continue;
			
			Enumeration<InetAddress> addresses = current.getInetAddresses();
			while(addresses.hasMoreElements()) {
				InetAddress current_addr = addresses.nextElement();
				if(current_addr.isLoopbackAddress()) continue;
				if(current_addr instanceof Inet4Address) {
					return current_addr.getHostAddress();
				}
			}
		}
		return "127.0.0.1";
	}
}
