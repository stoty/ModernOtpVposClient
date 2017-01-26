package hu.netfone.onlinepayment.otp;

public class LekerdezesSikertelenException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LekerdezesSikertelenException(String ok) {
		super(ok);
	}
}
