package hu.netfone.onlinepayment.otp;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.xml.bind.DatatypeConverter;

public class OTPSigner {

	Signature signer;
	
	public OTPSigner(String privatKulcsFileNev) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
		
		File keyFile = new File(privatKulcsFileNev);
	    byte[] keyData = new byte[(int) keyFile.length()];
	    DataInputStream dis = new DataInputStream(new FileInputStream(keyFile));
	    dis.readFully(keyData);
	    dis.close();
	    
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
	    PrivateKey key = keyFactory.generatePrivate(keySpec);
	    signer = Signature.getInstance("SHA512withRSA");
	    signer.initSign(key);
	}
	
	private String join(String join, String... strings) {
	    if (strings == null || strings.length == 0) {
	        return "";
	    } else if (strings.length == 1) {
	        return strings[0];
	    } else {
	        StringBuilder sb = new StringBuilder();
	        sb.append(strings[0]);
	        for (int i = 1; i < strings.length; i++) {
	            sb.append(join).append(strings[i]);
	        }
	        return sb.toString();
	    }
	}
	
	 public String generateSignature(String[] fields) throws SignatureException {
		String joined = join("|", fields);
		
		signer.update(joined.getBytes());
		byte[] alairas = signer.sign();
		
		return DatatypeConverter.printHexBinary(alairas);
    }
	
	
}
