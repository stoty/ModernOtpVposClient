package test;

import hu.netfone.onlinepayment.otp.TranzakcioLekerdezes;

import hu.iqsys.otp.webshoptranzakciolekerdezes.output.Record;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

public class LekerdezesTeszt {

	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException {
		String tranzakcioId;
		if (args.length == 1 && !args[0].isEmpty()) {
			tranzakcioId = args[0];
		} else {
			System.out.println("Meg kell adni a tranzakció id-t");
			return;
		}
		
		TranzakcioLekerdezes lekerdezes = new TranzakcioLekerdezes(TestData.getPosConfig());
		
		
		lekerdezes.lekerdezesInditas(tranzakcioId);
		
		if (!lekerdezes.sikeres()) {
			System.out.println("fizetés eredmény lekérdezés hiba");
			return;
		}
		
		Record valasz = lekerdezes.getFizetesTranzakcioSingleResult();

		if (!lekerdezes.fizetesTranzakcioBefejezodott(valasz)) {
			//Nincs még válasz
			System.out.println("Nem fejeződött be a tranzakció");
			return;
		}
		
		if (lekerdezes.fizetesTranzakcioSikeres(valasz)) {
			System.out.println("Sikeres tranzakció. resultcode: " + valasz.getResponsecode() 
				+ "  auth kód:" + lekerdezes.getFizetesTranzakcioAuthorizationCode(valasz));
		} else {
			System.out.println("Sikertelen tranzakció. resultcode: " + valasz.getResponsecode());
		}
	}

}
