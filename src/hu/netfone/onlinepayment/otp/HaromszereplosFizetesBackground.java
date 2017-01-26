package hu.netfone.onlinepayment.otp;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

public class HaromszereplosFizetesBackground {

	public String fizetes(PosConfig config, BigInteger osszeg, String comment, String tranzakcioId, String backURL, CallbackFunction cb)
			throws UnsupportedEncodingException {
		// Csináljuk meg a konfig ojjektumot
		HaromszereplosFizetesInditas fizetesInditas = new HaromszereplosFizetesInditas(config);
		fizetesInditas.fizetesElokeszites(osszeg, comment, tranzakcioId, backURL);

		String redirectURL = fizetesInditas.getRedirectUrlString();

		(new Thread(new FizetesBgThread(fizetesInditas, cb))).start();

		return redirectURL;
	}

	private class FizetesBgThread implements Runnable {

		HaromszereplosFizetesInditas fizetes;
		CallbackFunction cb;

		FizetesBgThread(HaromszereplosFizetesInditas fizetes, CallbackFunction cb) {
			this.fizetes = fizetes;
			this.cb = cb;
		}

		@Override
		public void run() {

			try {
				fizetes.fizetesInditas();
				cb.onSuccess(fizetes);
				// Innen meg kéne különböztetni, hogy egyátlaán eljutott-e az OTP-ig az XML, de ez nem felétlenűl egyértelmű.
				// Az igazi megoldás az lenne, ha csak az explicit hiba esetén tennénk hibára, és minden más esetben egy későbbi státszlekérdezést indítanánk rá.
			} catch (InvalidKeyException e1) {
				cb.onFailure(fizetes);
			} catch (NoSuchAlgorithmException e2) {
				cb.onFailure(fizetes);
			} catch (InvalidKeySpecException e3) {
				cb.onFailure(fizetes);
			} catch (SignatureException e4) {
				cb.onFailure(fizetes);
			} catch (IOException e5) {
				cb.onFailure(fizetes);
			} catch (JAXBException e6) {
				cb.onFailure(fizetes);
			} catch (ParserConfigurationException e7) {
				cb.onFailure(fizetes);
			} catch (SAXException e8) {
				cb.onFailure(fizetes);
			} catch (ParseException e9) {
				cb.onFailure(fizetes);
			} catch (FizetesSikertelenException e10) {
				cb.onFailure(fizetes);
			} catch (SOAPException e11) {
				// Ez nem biztos, hogy hiba, lehet hogy simán timeout-oltunk, hagyjuk függőben
			}

		}

	}

	public interface CallbackFunction {
		public void onSuccess(HaromszereplosFizetesInditas fizetes);

		public void onFailure(HaromszereplosFizetesInditas fizetes);
	}
}
