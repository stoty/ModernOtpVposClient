package hu.netfone.onlinepayment.otp;

public interface TrafficLogger {
	
	public enum TrafficType {
		SOAP_OUT,
		SOAP_IN,
		XML_OUT,
		XML_IN
	}
	
	public void logTraffic(TrafficType trafficType, String content);
}
