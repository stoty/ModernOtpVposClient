package test;

import hu.netfone.onlinepayment.otp.TrafficLogger;

public class SysOutLogger implements TrafficLogger {

	@Override
	public void logTraffic(TrafficType trafficType, String content) {
		System.out.println("LOGGING:" + trafficType);
		System.out.println("LOGGING:" + content);
		System.out.println();
	}

}
