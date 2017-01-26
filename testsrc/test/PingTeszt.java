package test;

import hu.netfone.onlinepayment.otp.FizetesSikertelenException;
import hu.netfone.onlinepayment.otp.soap.Ping;

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

public class PingTeszt {

	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, FizetesSikertelenException {
		
		Ping kliens = new Ping();
		System.out.println(kliens.call(TestData.getPosConfig().getTrafficLogger()));

	}

}
