package hu.netfone.onlinepayment.otp;

import hu.iqsys.otp.webshoptranzakciolekerdezes.input.IsClientSignatureType;
import hu.iqsys.otp.webshoptranzakciolekerdezes.input.WebshopTranzakcioLekerdezes;
import hu.iqsys.otp.webshoptranzakciolekerdezes.input.WebshopTranzakcioLekerdezes.Variables;
import hu.iqsys.otp.webshoptranzakciolekerdezes.output.Record;
import hu.iqsys.otp.webshoptranzakciolekerdezes.output.WebshopTranzakcioLekerdezesOutput;
import hu.netfone.onlinepayment.otp.TrafficLogger.TrafficType;
import hu.netfone.onlinepayment.otp.soap.StartWorkflowSynch;
import hu.netfone.onlinepayment.otp.soap.WorkflowState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.xml.sax.SAXException;

public class TranzakcioLekerdezes {
		
	private WebshopTranzakcioLekerdezes tranzakcioLekerdezes;
	private WebshopTranzakcioLekerdezesOutput tranzakcioLekerdezesOutput;

	private PosConfig config;
	
	private SimpleDateFormat df =  new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
	
	private void reset(){
		tranzakcioLekerdezes = new WebshopTranzakcioLekerdezes();
		tranzakcioLekerdezesOutput = new WebshopTranzakcioLekerdezesOutput();
		tranzakcioLekerdezesInit();
	}
	
	public TranzakcioLekerdezes(PosConfig posConfig) {
		config = posConfig;
	}

	private void tranzakcioLekerdezesInit() {
		tranzakcioLekerdezes.setTemplateName(Constants.TRANZAKCIO_LEKERDEZES_TEMPLATE_NEV);
		Variables variables = new Variables();
		tranzakcioLekerdezes.setVariables(variables);
		
		variables.setIsClientCode(Constants.CLIENT_NAME);
		variables.setIsPOSID(config.getPosId());
		variables.setIsMaxRecords(BigInteger.valueOf(1));		
	}
	
	private void tranzakcioLekerdezesAlairas() throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException{
		String[] adatok = new String[5];
		
		adatok[0] = tranzakcioLekerdezes.getVariables().getIsPOSID();
		if(tranzakcioLekerdezes.getVariables().getIsTransactionID() != null){
			adatok[1] = tranzakcioLekerdezes.getVariables().getIsTransactionID();
		}
		if(tranzakcioLekerdezes.getVariables().getIsMaxRecords() != null){
			//Bár az XSD szerint ez nem lehet üres, a doksi példában az
			adatok[2] = tranzakcioLekerdezes.getVariables().getIsMaxRecords().toString();
		}
		if(tranzakcioLekerdezes.getVariables().getIsStartDate() != null){
			adatok[3] = df.format(tranzakcioLekerdezes.getVariables().getIsStartDate());
		}
		if(tranzakcioLekerdezes.getVariables().getIsEndDate() != null){
			adatok[4] = df.format(tranzakcioLekerdezes.getVariables().getIsEndDate());
		}
		
		OTPSigner signer = new OTPSigner(config.getKeyFileName());
		
		IsClientSignatureType signature = new IsClientSignatureType();
		signature.setAlgorithm("SHA512");
		signature.setValue(signer.generateSignature(adatok));

		tranzakcioLekerdezes.getVariables().setIsClientSignature(signature);
	}
	
	public WebshopTranzakcioLekerdezesOutput lekerdezesInditas(Date startDate, Date endDate, BigInteger maxRecords) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, LekerdezesSikertelenException{
		reset();
		
		tranzakcioLekerdezes.getVariables().setIsStartDate(df.format(startDate));
		tranzakcioLekerdezes.getVariables().setIsEndDate(df.format(endDate));
		tranzakcioLekerdezes.getVariables().setIsMaxRecords(maxRecords);
		
		lekerdezesKozos();
		
		return tranzakcioLekerdezesOutput;
	}
	
	public WebshopTranzakcioLekerdezesOutput lekerdezesInditas(String tranzakcioAzonosito) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, LekerdezesSikertelenException{
		reset();
		
		tranzakcioLekerdezes.getVariables().setIsTransactionID(tranzakcioAzonosito);
		tranzakcioLekerdezes.getVariables().setIsMaxRecords(BigInteger.ONE);
		
		lekerdezesKozos();
		
		return tranzakcioLekerdezesOutput;
	}
	
	private void lekerdezesKozos() throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, LekerdezesSikertelenException{
		tranzakcioLekerdezesAlairas();
		
		JAXBContext requestJaxbContext = JAXBContext.newInstance(WebshopTranzakcioLekerdezes.class);
		Marshaller requestMarshaller = requestJaxbContext.createMarshaller();
		requestMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	
		StringWriter outHandler = new StringWriter();
		requestMarshaller.marshal(tranzakcioLekerdezes, outHandler);
	
		String belsoXml = outHandler.toString();
		//Itt pedig megvan a belső XML
		config.getTrafficLogger().logTraffic(TrafficType.XML_OUT, belsoXml);
		
		StartWorkflowSynch workflowSynchStub = new StartWorkflowSynch();
		WorkflowState state = workflowSynchStub.call(Constants.TRANZAKCIO_LEKERDEZES_TEMPLATE_NEV, belsoXml, config.getTrafficLogger());

		config.getTrafficLogger().logTraffic(TrafficType.XML_IN, state.getResult());

		
		//Ha idáig eljutunk, akkor itt sok hiba nem lehet, de azért ellenőrizgessünk
		
		if(!state.isCompleted()){
			throw new LekerdezesSikertelenException("isCompleted = false");
		}
		
		JAXBContext responseJaxbContext = JAXBContext.newInstance(WebshopTranzakcioLekerdezesOutput.class);
		Unmarshaller responseUnMarshaller = responseJaxbContext.createUnmarshaller();
		tranzakcioLekerdezesOutput =  (WebshopTranzakcioLekerdezesOutput) responseUnMarshaller.unmarshal(new StringReader(state.getResult()));
		
		if(!tranzakcioLekerdezesOutput.getMessagelist().getMessage().get(0).toUpperCase().equals(Constants.SIKER)){
			throw new LekerdezesSikertelenException("message != SIKER");
		}
				
	}
	
	//Innen kényelmi funkciók
	public boolean egyTranzakcioSikeres(String tranzakcioAzonosito) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException, LekerdezesSikertelenException{
		lekerdezesInditas(tranzakcioAzonosito);
		
		if(tranzakcioLekerdezesOutput.getResultset().getRecord().size() != 1){
			throw new LekerdezesSikertelenException("nem egy rekord van a resultset-ben");
		}
		
		Record eredmeny = tranzakcioLekerdezesOutput.getResultset().getRecord().get(0);
		if(!eredmeny.getTransactionid().equals(tranzakcioAzonosito)){
			throw new LekerdezesSikertelenException("Nem a lekérdezett tranzakcióra kaptam választ");
		}

		if(!eredmeny.getState().equals(Constants.FELDOLGOZVA)){
			return false;
		}
		
		if(eredmeny.getParams().getOutput().getAuthorizationcode() == null || eredmeny.getParams().getOutput().getAuthorizationcode().equals("")){
			return false;
		}
		
		if(Constants.successPosResponseCodes.contains(eredmeny.getResponsecode())){
			return true;
		}
		
		return false;
	}
	
	public String getResponseCode(){
		return tranzakcioLekerdezesOutput.getResultset().getRecord().get(0).getResponsecode();
	}
		
	public String getAuthorizationcode(){
		return tranzakcioLekerdezesOutput.getResultset().getRecord().get(0).getParams().getOutput().getAuthorizationcode();
	}
}
