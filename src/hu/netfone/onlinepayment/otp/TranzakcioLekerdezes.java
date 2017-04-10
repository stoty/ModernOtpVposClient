package hu.netfone.onlinepayment.otp;

import hu.netfone.onlinepayment.otp.TrafficLogger.TrafficType;
import hu.netfone.onlinepayment.otp.soap.StartWorkflowSynch;
import hu.netfone.onlinepayment.otp.soap.WorkflowState;

import hu.iqsys.otp.webshoptranzakciolekerdezes.input.IsClientSignatureType;
import hu.iqsys.otp.webshoptranzakciolekerdezes.input.WebshopTranzakcioLekerdezes;
import hu.iqsys.otp.webshoptranzakciolekerdezes.input.WebshopTranzakcioLekerdezes.Variables;
import hu.iqsys.otp.webshoptranzakciolekerdezes.output.Output;
import hu.iqsys.otp.webshoptranzakciolekerdezes.output.Record;
import hu.iqsys.otp.webshoptranzakciolekerdezes.output.WebshopTranzakcioLekerdezesOutput;
import org.xml.sax.SAXException;

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
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

public class TranzakcioLekerdezes {

	private WebshopTranzakcioLekerdezes tranzakcioLekerdezes;
	private WebshopTranzakcioLekerdezesOutput tranzakcioLekerdezesOutput;
	
	private WorkflowState state;

	private PosConfig config;

	private SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");

	private void init() {
		tranzakcioLekerdezes = new WebshopTranzakcioLekerdezes();
		tranzakcioLekerdezesOutput = new WebshopTranzakcioLekerdezesOutput();
		state = null;
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

	private void tranzakcioLekerdezesAlairas()
			throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
		String[] adatok = new String[]{"","","","",""};

		adatok[0] = tranzakcioLekerdezes.getVariables().getIsPOSID();
		if (tranzakcioLekerdezes.getVariables().getIsTransactionID() != null) {
			adatok[1] = tranzakcioLekerdezes.getVariables().getIsTransactionID();
		}
		if (tranzakcioLekerdezes.getVariables().getIsMaxRecords() != null) {
			// Bár az XSD szerint ez nem lehet üres, a doksi példában az
			adatok[2] = tranzakcioLekerdezes.getVariables().getIsMaxRecords().toString();
		}
		if (tranzakcioLekerdezes.getVariables().getIsStartDate() != null) {
			adatok[3] = df.format(tranzakcioLekerdezes.getVariables().getIsStartDate());
		}
		if (tranzakcioLekerdezes.getVariables().getIsEndDate() != null) {
			adatok[4] = df.format(tranzakcioLekerdezes.getVariables().getIsEndDate());
		}

		OTPSigner signer = new OTPSigner(config.getKeyFileName());

		IsClientSignatureType signature = new IsClientSignatureType();
		signature.setAlgorithm("SHA512");
		signature.setValue(signer.generateSignature(adatok));

		tranzakcioLekerdezes.getVariables().setIsClientSignature(signature);
	}

	public void lekerdezesInditas(Date startDate, Date endDate, BigInteger maxRecords)
			throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException,
			SOAPException, ParserConfigurationException, SAXException, ParseException {
		init();

		tranzakcioLekerdezes.getVariables().setIsStartDate(df.format(startDate));
		tranzakcioLekerdezes.getVariables().setIsEndDate(df.format(endDate));
		tranzakcioLekerdezes.getVariables().setIsMaxRecords(maxRecords);

		lekerdezesKozos();

	}

	public void lekerdezesInditas(String tranzakcioAzonosito)
			throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, JAXBException,
			SOAPException, ParserConfigurationException, SAXException, ParseException {
		init();

		tranzakcioLekerdezes.getVariables().setIsTransactionID(tranzakcioAzonosito);
		tranzakcioLekerdezes.getVariables().setIsMaxRecords(BigInteger.ONE);

		lekerdezesKozos();

	}

	private void lekerdezesKozos() throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException,
			IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException {
		tranzakcioLekerdezesAlairas();

		JAXBContext requestJaxbContext = JAXBContext.newInstance(WebshopTranzakcioLekerdezes.class);
		Marshaller requestMarshaller = requestJaxbContext.createMarshaller();
		requestMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		StringWriter outHandler = new StringWriter();
		requestMarshaller.marshal(tranzakcioLekerdezes, outHandler);

		String belsoXml = outHandler.toString();
		// Itt pedig megvan a belső XML
		config.getTrafficLogger().logTraffic(TrafficType.XML_OUT, belsoXml);

		StartWorkflowSynch workflowSynchStub = new StartWorkflowSynch();
		state = workflowSynchStub.call(Constants.TRANZAKCIO_LEKERDEZES_TEMPLATE_NEV, belsoXml, config.getTrafficLogger());

		config.getTrafficLogger().logTraffic(TrafficType.XML_IN, state.getResult());

		JAXBContext responseJaxbContext = JAXBContext.newInstance(WebshopTranzakcioLekerdezesOutput.class);
		Unmarshaller responseUnMarshaller = responseJaxbContext.createUnmarshaller();
		tranzakcioLekerdezesOutput = (WebshopTranzakcioLekerdezesOutput) responseUnMarshaller.unmarshal(new StringReader(state.getResult()));

	}
	
		
	public WebshopTranzakcioLekerdezes getTranzakcioLekerdezes() {
		return tranzakcioLekerdezes;
	}

	public WebshopTranzakcioLekerdezesOutput getTranzakcioLekerdezesOutput() {
		return tranzakcioLekerdezesOutput;
	}

	public WorkflowState getState() {
		return state;
	}

	// Innen kényelmi funkciók

	//Ez csak a lekérdezés sikeressége, nem a lekérdezett tranzakcióé!
	public boolean sikeres() {
		return state.isCompleted() && tranzakcioLekerdezesOutput.getMessagelist().getMessage().get(0).toUpperCase().equals(Constants.SIKER);
	}
	
	public List<Record> getFizetesTranzakcioResultLista() {
		return tranzakcioLekerdezesOutput.getResultset().getRecord();
	}
	
	public Record getFizetesTranzakcioSingleResult() {
		return getFizetesTranzakcioResultLista().get(0);
	}
	
	public Output getFizetesTranzakcioOutput(Record record) {
		return record.getParams().getOutput();
	}
	
	public String getFizetesTranzakcioAuthorizationCode(Record record) {
		return getFizetesTranzakcioOutput(record).getAuthorizationcode();
	}
	
	public boolean fizetesTranzakcioBefejezodott(Record record) {
		String responseCode = record.getResponsecode();
		return responseCode != null && !responseCode.isEmpty();
	}
	
	public boolean fizetesTranzakcioSikeres(Record record) {
		String authorizationCode = getFizetesTranzakcioAuthorizationCode(record);
		return Constants.successPosResponseCodes.contains(record.getResponsecode()) 
				&& authorizationCode != null 
				&& !authorizationCode.isEmpty();
	}
	
}
