package test;

import hu.netfone.onlinepayment.otp.FizetesSikertelenException;
import hu.netfone.onlinepayment.otp.LekerdezesSikertelenException;
import hu.netfone.onlinepayment.otp.TranzakcioLekerdezes;

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

	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, FizetesSikertelenException, LekerdezesSikertelenException {
		String tranzakcioId="valami";
		if(args.length==1 && !args[0].isEmpty()){
			tranzakcioId = args[0];
		}
		
		
		TranzakcioLekerdezes kliens = new TranzakcioLekerdezes(TestData.getPosConfig());
		kliens.egyTranzakcioSikeres(tranzakcioId);
	}

}
