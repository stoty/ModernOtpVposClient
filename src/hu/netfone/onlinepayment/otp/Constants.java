package hu.netfone.onlinepayment.otp;

import java.util.Arrays;
import java.util.List;

public class Constants {
	public static final String TRUE="TRUE";
	public static final String FALSE ="FALSE";
	public static final String CLIENT_NAME="WEBSHOP";
	public static final String HAROMSZEREPLOS_TEMPLATE_NEV="WEBSHOPFIZETES";
	public static final String TRANZAKCIO_LEKERDEZES_TEMPLATE_NEV="WEBSHOPTRANZAKCIOLEKERDEZES";
	public static final String FELDOLGOZVA="FELDOLGOZVA";
	public static final String SIKER="SIKER";
	
	
	public static final String OTP_WEBSHOP_CUSTOMER_SIDE_URL="https://www.otpbankdirekt.hu/webshop/do/webShopVasarlasInditas?posId=%s&azonosito=%s&nyelvkod=%s";
    public static final List<String> successPosResponseCodes = Arrays.asList(
        	new String[]{"000", "00", "001", "002", "003", "004", 
        				 "005", "006", "007", "008", "009", "010"}
        );

}
