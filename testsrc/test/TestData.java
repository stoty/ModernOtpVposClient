package test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import hu.netfone.onlinepayment.otp.PosConfig;

public class TestData {
	public static final String posId = "#02299991";
	public static final String privKeyFileName = "/home/stoty/Downloads/Webshop_4.0/teszt_kulcsok/#02299991.privKey";
	public static final String backURL = "https://127.0.0.1/otpwebshop/Callback.jsp?tranzakcioId=true&posId=%s&tranzakcioAzonosito=";

	public static PosConfig getPosConfig(){
		return new PosConfig(TestData.posId, TestData.privKeyFileName, new SysOutLogger());
	}
	
	public static String getBackURLPrefix() throws UnsupportedEncodingException{
		return String.format(backURL, URLEncoder.encode(posId, "UTF-8"));
	}
}
