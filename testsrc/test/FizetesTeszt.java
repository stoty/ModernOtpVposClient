package test;

import hu.netfone.onlinepayment.otp.HaromszereplosFizetesInditas;
import hu.netfone.onlinepayment.otp.OTPInterfaceTulterheltException;

import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

public class FizetesTeszt {

	public static void main(String[] args) throws 
			InvalidKeyException, 
			FileNotFoundException, 
			NoSuchAlgorithmException, 
			InvalidKeySpecException, 
			SignatureException, 
			IOException, 
			JAXBException, 
			SOAPException, 
			ParserConfigurationException, 
			SAXException, ParseException, 
			OTPInterfaceTulterheltException {
		String tranzakcioID = "teszttranzakcioid" + new java.util.Date().getTime();
		String backURL = TestData.getBackURLPrefix() + URLEncoder.encode(tranzakcioID, "UTF-8");
		BigInteger osszeg = BigInteger.valueOf(5);
		String comment = "komment";
		
		System.out.println("tranzakcioId: " + tranzakcioID);
		System.out.println("OTP fizetőfelület URL:");
		System.out.println(String.format(hu.netfone.onlinepayment.otp.Constants.OTP_WEBSHOP_CUSTOMER_SIDE_URL, 
				URLEncoder.encode(TestData.posId, "UTF-8"), URLEncoder.encode(tranzakcioID, "UTF-8"), "hu"));
		System.out.println();

		HaromszereplosFizetesInditas fizetesInditas = new HaromszereplosFizetesInditas(TestData.getPosConfig());
		
		fizetesInditas.fizetesInditas(osszeg, comment, tranzakcioID, backURL);
		
		if (!fizetesInditas.sikeres()) {
			System.out.println("fizetés tranzakció indítási hiba. Kód:" + fizetesInditas.getResultCode());
			return;
		}
		
		System.out.println("fizetés tranzakció indítás sikeres. redirect url: " + fizetesInditas.getRedirectUrlString());

	}

}
