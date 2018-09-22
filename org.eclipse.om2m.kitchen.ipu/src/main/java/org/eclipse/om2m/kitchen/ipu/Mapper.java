package org.eclipse.om2m.kitchen.ipu;
import org.eclipse.om2m.commons.obix.*;
import org.eclipse.om2m.commons.obix.io.ObixEncoder;
 
public class Mapper {
 
	public static String getMySensorDescriptorRep(String sclId, String appId, String ipuId, Sensor mySensor) {
		Obj obj = new Obj();
		
		obj.add(new Str("Device_ID", String.valueOf(mySensor.getDeviceId())));
		obj.add(new Str("Device", mySensor.getDeviceName()));
		obj.add(new Str("Type", mySensor.getType()));
		obj.add(new Str("Unit", mySensor.getUnit()));
		obj.add(new Str("MinValue", mySensor.getMinValue()));
		obj.add(new Str("MaxValue", mySensor.getMaxValue()));
		obj.add(new Str("Dangerous Value", mySensor.getDangerousValue()));
		
		Op opGet = new Op();
		opGet.setName("GET");
		opGet.setHref(new Uri(sclId + "/applications/" + appId + "/containers/DATA/contentInstances/latest/content"));
		opGet.setIs(new Contract("retrieve"));
		opGet.setIn(new Contract("obix:Nil"));
		opGet.setOut(new Contract("obix:Nil"));
		obj.add(opGet);
 
		Op opGetDirect = new Op();
		opGetDirect.setName("GET(Direct)");
		opGetDirect.setHref(new Uri(sclId + "/applications/" + appId + "/" + ipuId));
		opGetDirect.setIs(new Contract("retrieve"));
		opGetDirect.setIn(new Contract("obix:Nil"));
		opGetDirect.setOut(new Contract("obix:Nil"));
		obj.add(opGetDirect);
		
		Op opGetDangerous = new Op();
		opGetDangerous.setName("GET(Dangerous)");
		opGetDangerous.setHref(new Uri(sclId + "/applications/" + appId + "/" + ipuId + "/" + "dangerous"));
		opGetDangerous.setIs(new Contract("retrieve"));
		opGetDangerous.setIn(new Contract("obix:Nil"));
		opGetDangerous.setOut(new Contract("obix:Nil"));
		obj.add(opGetDangerous);
		
		Op opON = new Op();
		opON.setName("ON");
		opON.setHref(new Uri(sclId + "/applications/" + appId + "/" + ipuId + "/" +
							String.valueOf(mySensor.getDeviceId()) + "/Switch/true"));
		opON.setIs(new Contract("execute"));
		opON.setIn(new Contract("obix:Nil"));
		opON.setOut(new Contract("obix:Nil"));
		obj.add(opON);
 
		Op opOFF = new Op();
		opOFF.setName("OFF");
		opOFF.setHref(new Uri(sclId + "/applications/" + appId + "/" + ipuId + "/" + 
				String.valueOf(mySensor.getDeviceId()) + "/Switch/false"));
		opOFF.setIs(new Contract("execute"));
		opOFF.setIn(new Contract("obix:Nil"));
		opOFF.setOut(new Contract("obix:Nil"));
		obj.add(opOFF);

		return ObixEncoder.toString(obj);
	}
	
	public static String getMySensorDataRep(String value) {
		Obj obj = new Obj();
		obj.add(new Str("value", value));
		return ObixEncoder.toString(obj);
	}
	
	public static String getMySensorDangerous(String dangerous) {
		Obj obj = new Obj();
		obj.add(new Str("dangerous", dangerous));
		return ObixEncoder.toString(obj);
	}
}
