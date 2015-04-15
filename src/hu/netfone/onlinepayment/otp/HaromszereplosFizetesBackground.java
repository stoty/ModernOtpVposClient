package hu.netfone.onlinepayment.otp;

import hu.netfone.onlinepayment.otp.HaromszereplosFizetesInditas;
import hu.netfone.onlinepayment.otp.PosConfig;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public class HaromszereplosFizetesBackground {
	
	public String fizetes(PosConfig config, BigInteger osszeg, String comment, String tranzakcioId, String backURL, CallbackFunction cb) throws UnsupportedEncodingException{
		//Csináljuk meg a konfig ojjektumot		
		HaromszereplosFizetesInditas fizetesInditas = new HaromszereplosFizetesInditas(config);
		fizetesInditas.fizetesElokeszites(osszeg, comment, tranzakcioId, backURL);
		
		String redirectURL = fizetesInditas.getRedirectUrlString();
		
		(new Thread(new FizetesBgThread(fizetesInditas, cb))).start();
		
		return redirectURL;
	}
	
	private class FizetesBgThread implements Runnable {
		
		HaromszereplosFizetesInditas fizetes;
		CallbackFunction cb;
		
		FizetesBgThread(HaromszereplosFizetesInditas fizetes, CallbackFunction cb){
			this.fizetes = fizetes;
			this.cb = cb;
		}
		
		

		@Override
		public void run() {
			try {
				fizetes.fizetesInditas();
				cb.onSuccess(fizetes);
			} catch (Exception e) {
				cb.onFailure(fizetes);
			} 
			
		}
		
	}
	
	public interface CallbackFunction {
		public void onSuccess(HaromszereplosFizetesInditas fizetes);
		
		public void onFailure(HaromszereplosFizetesInditas fizetes);
	}
}
