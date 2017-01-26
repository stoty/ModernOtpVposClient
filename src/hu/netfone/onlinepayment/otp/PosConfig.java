package hu.netfone.onlinepayment.otp;

public class PosConfig {

	protected String posId;
	protected String keyFileName;
	protected TrafficLogger trafficLogger;

	public PosConfig(String posId, String keyFileName, TrafficLogger trafficLogger) {
		this.posId = posId;
		this.keyFileName = keyFileName;
		this.trafficLogger = trafficLogger;
	}

	public String getPosId() {
		return posId;
	}

	public String getKeyFileName() {
		return keyFileName;
	}

	public TrafficLogger getTrafficLogger() {
		return trafficLogger;
	}

}
