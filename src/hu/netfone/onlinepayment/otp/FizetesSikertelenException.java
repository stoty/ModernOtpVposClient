package hu.netfone.onlinepayment.otp;

public class FizetesSikertelenException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String otpMessageCode;
	
	public FizetesSikertelenException(String ok){
		super(ok);
	}

	public FizetesSikertelenException(String ok, String otpMessageCode){
		super(ok);
		this.otpMessageCode = otpMessageCode;
	}
	
	public String getOtpMessageCode(){
		return otpMessageCode;
	}

	
}
