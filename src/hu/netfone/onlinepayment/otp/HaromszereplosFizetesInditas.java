package hu.netfone.onlinepayment.otp;

import hu.iqsys.otp.webshopfizetes.input.IsClientSignatureType;
import hu.iqsys.otp.webshopfizetes.input.WebshopFizetes;
import hu.iqsys.otp.webshopfizetes.input.WebshopFizetes.Variables;
import hu.iqsys.otp.webshopfizetes.output.WebshopFizetesOutput;
import hu.netfone.onlinepayment.otp.TrafficLogger.TrafficType;
import hu.netfone.onlinepayment.otp.soap.StartWorkflowSynch;
import hu.netfone.onlinepayment.otp.soap.WorkflowState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.xml.sax.SAXException;

public class HaromszereplosFizetesInditas {
	
	private WebshopFizetes webshopFizetes = new WebshopFizetes();
	private WebshopFizetesOutput webshopFizetesKimenet;
	
	private PosConfig config;
	
	public HaromszereplosFizetesInditas(PosConfig posConfig) {
		config = posConfig;
		webshopFizetesInit();
	}
	
	private void webshopFizetesInit(){
		webshopFizetes.setTemplateName(Constants.HAROMSZEREPLOS_TEMPLATE_NEV);
		Variables variables = new Variables();
		webshopFizetes.setVariables(variables);

		variables.setIsClientCode(Constants.CLIENT_NAME);
		variables.setIsPOSID(config.getPosId());
		variables.setIsCardPocketId("");
		variables.setIsExchange("HUF");
		variables.setIsLanguageCode("hu");
		
		variables.setIsNameNeeded(Constants.FALSE);
		variables.setIsCountryNeeded(Constants.FALSE);
		variables.setIsCountyNeeded(Constants.FALSE);
		variables.setIsSettlementNeeded(Constants.FALSE);
		variables.setIsZipcodeNeeded(Constants.FALSE);
		variables.setIsStreetNeeded(Constants.FALSE);
		variables.setIsMailAddressNeeded(Constants.FALSE);
		variables.setIsNarrationNeeded(Constants.FALSE);
		variables.setIsConsumerReceiptNeeded(Constants.FALSE);
		variables.setIsConsumerRegistrationNeeded(Constants.FALSE);
	}
	
	private void webshopFizetesAlairas() throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException{
		String[] adatok = new String[5];
		
		adatok[0] = webshopFizetes.getVariables().getIsPOSID();
		adatok[1] = webshopFizetes.getVariables().getIsTransactionID();
		adatok[2] = webshopFizetes.getVariables().getIsAmount().toString();
		adatok[3] = webshopFizetes.getVariables().getIsExchange();
		adatok[4] = webshopFizetes.getVariables().getIsConsumerRegistrationID();
		
		if(adatok[4] == null){
			adatok[4] = "";
		}
		
		OTPSigner signer = new OTPSigner(config.getKeyFileName());
		
		IsClientSignatureType signature = new IsClientSignatureType();
		signature.setAlgorithm("SHA512");
		signature.setValue(signer.generateSignature(adatok));
		
		webshopFizetes.getVariables().setIsClientSignature(signature);
	}
	
	public void fizetesElokeszites(BigInteger osszeg, String comment, String tranzakcioId, String backURL){
		webshopFizetes.getVariables().setIsAmount(osszeg);
		webshopFizetes.getVariables().setIsShopComment(comment);
		webshopFizetes.getVariables().setIsTransactionID(tranzakcioId);
		webshopFizetes.getVariables().setIsBackURL(backURL);
	}
	
	public String fizetesInditas() throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, FizetesSikertelenException{
		webshopFizetesAlairas();
		//Itt megvan az aláírt ojjektumunk
		
		JAXBContext requestJaxbContext = JAXBContext.newInstance(WebshopFizetes.class);
		Marshaller requestMarshaller = requestJaxbContext.createMarshaller();
		requestMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	
		StringWriter outHandler = new StringWriter();
		requestMarshaller.marshal(webshopFizetes, outHandler);
		
		String belsoXml = outHandler.toString();
		//Itt pedig megvan a belső XML

		config.getTrafficLogger().logTraffic(TrafficType.XML_OUT, belsoXml);
		
		StartWorkflowSynch workflowSynchStub = new StartWorkflowSynch();
		WorkflowState state = workflowSynchStub.call(Constants.HAROMSZEREPLOS_TEMPLATE_NEV, belsoXml, config.getTrafficLogger());

		config.getTrafficLogger().logTraffic(TrafficType.XML_IN, state.getResult());

		
		//Ha idáig eljutunk, akkor itt sok hiba nem lehet, de azért ellenőrizgessünk
		
		if(!state.isCompleted()){
			throw new FizetesSikertelenException("isCompleted = false");
		}
		
		JAXBContext responseJaxbContext = JAXBContext.newInstance(WebshopFizetesOutput.class);
		Unmarshaller responseUnMarshaller = responseJaxbContext.createUnmarshaller();
		webshopFizetesKimenet =  (WebshopFizetesOutput) responseUnMarshaller.unmarshal(new StringReader(state.getResult()));
		
		if(!webshopFizetesKimenet.getMessagelist().getMessage().get(0).toUpperCase().equals(Constants.SIKER)){
			throw new FizetesSikertelenException("message != SIKER", webshopFizetesKimenet.getMessagelist().getMessage().get(0));
		}
		
		if(webshopFizetesKimenet.getResultset().getRecord().get(0).getAuthorizationcode() == null || webshopFizetesKimenet.getResultset().getRecord().get(0).getAuthorizationcode().isEmpty()){	
			throw new FizetesSikertelenException("Nincs authorization code");
		}
		
		return webshopFizetesKimenet.getResultset().getRecord().get(0).getAuthorizationcode();
	}
	
	public String fizetesInditas(BigInteger osszeg, String comment, String tranzakcioId, String backURL) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, FizetesSikertelenException {
		fizetesElokeszites(osszeg, comment, tranzakcioId, backURL);
		return fizetesInditas();
	}
	
	public WebshopFizetesOutput getWebshopFizetesOutput(){
		return webshopFizetesKimenet;
	}
	
	public BigInteger getAmount(){
		return webshopFizetes.getVariables().getIsAmount();
	}
	
	public String getComment(){
		return webshopFizetes.getVariables().getIsShopComment();
	}
	
	public String getTranzakcioId(){
		return webshopFizetes.getVariables().getIsTransactionID();
	}
	
	public String getBackURL() {
		return webshopFizetes.getVariables().getIsBackURL();
	}
	
	public String getAuthorizationCode() {
		return webshopFizetesKimenet.getResultset().getRecord().get(0).getAuthorizationcode();
	}
	
	public String getMessage() {
		return webshopFizetesKimenet.getMessagelist().getMessage().get(0);
	}
	
	public String getRedirectUrlString() throws UnsupportedEncodingException{
		return String.format(Constants.OTP_WEBSHOP_CUSTOMER_SIDE_URL, URLEncoder.encode(config.posId, "UTF-8"), URLEncoder.encode(webshopFizetes.getVariables().getIsTransactionID(), "UTF-8"), webshopFizetes.getVariables().getIsLanguageCode() );
	}
}
