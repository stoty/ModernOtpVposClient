package hu.netfone.onlinepayment.otp;

import hu.netfone.onlinepayment.otp.TrafficLogger.TrafficType;
import hu.netfone.onlinepayment.otp.soap.StartWorkflowSynch;
import hu.netfone.onlinepayment.otp.soap.WorkflowState;

import hu.iqsys.otp.webshopfizetes.input.IsClientSignatureType;
import hu.iqsys.otp.webshopfizetes.input.WebshopFizetes;
import hu.iqsys.otp.webshopfizetes.input.WebshopFizetes.Variables;
import hu.iqsys.otp.webshopfizetes.output.WebshopFizetesOutput;
import org.xml.sax.SAXException;

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

public class HaromszereplosFizetesInditas {

	private WebshopFizetes webshopFizetes = new WebshopFizetes();
	private WebshopFizetesOutput webshopFizetesKimenet;

	private PosConfig config;

	public HaromszereplosFizetesInditas() throws Exception {
		throw new Exception("nem lehet konfig nélkül létrehozni");
	}

	public HaromszereplosFizetesInditas(PosConfig posConfig) {
		config = posConfig;
	}

	private void webshopFizetesInit() {
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

	private void webshopFizetesAlairas()
			throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException {
		String[] adatok = new String[5];

		adatok[0] = webshopFizetes.getVariables().getIsPOSID();
		adatok[1] = webshopFizetes.getVariables().getIsTransactionID();
		adatok[2] = webshopFizetes.getVariables().getIsAmount().toString();
		adatok[3] = webshopFizetes.getVariables().getIsExchange();
		adatok[4] = webshopFizetes.getVariables().getIsConsumerRegistrationID();

		if (adatok[4] == null) {
			adatok[4] = "";
		}

		OTPSigner signer = new OTPSigner(config.getKeyFileName());

		IsClientSignatureType signature = new IsClientSignatureType();
		signature.setAlgorithm("SHA512");
		signature.setValue(signer.generateSignature(adatok));

		webshopFizetes.getVariables().setIsClientSignature(signature);
	}

	public boolean fizetesInditasSimple(BigInteger osszeg, String comment, String tranzakcioId, String backURL) throws OTPInterfaceTulterheltException {
		try {
			fizetesInditas(osszeg, comment, tranzakcioId, backURL);

			String result = null;
			try {
				result = webshopFizetesKimenet.getMessagelist().getMessage().get(0);
			  // TODO log here
			} catch (Exception e) {
				// Nem tudtuk feldolgozni a választ
				// TODO log here
			}
			
			if ("SIKERESWEBSHOPFIZETESINDITAS".equals(result)) {
				return true;
			} else {
				return false;
			}
			
		} catch (InvalidKeyException
				| NoSuchAlgorithmException
				| InvalidKeySpecException
				| SignatureException
				| IOException
				| JAXBException
				| ParserConfigurationException
				| SAXException
				| ParseException e) {
			// Nem jött össze a kommunikáció
			// TODO log here
			return false;
		} catch (SOAPException soapException) {
			// TODO log here
			if (soapException.getMessage().contains("Maximum workflow number is reached")) {
				throw new OTPInterfaceTulterheltException();
			}
			// TODO log here
			return false;
		}

	}

	public void fizetesInditas(BigInteger osszeg, String comment, String tranzakcioId, String backURL)
			throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException,
			IOException, JAXBException, SOAPException, ParserConfigurationException, SAXException, ParseException {

		webshopFizetesInit();
		
		webshopFizetes.getVariables().setIsAmount(osszeg);
		webshopFizetes.getVariables().setIsShopComment(comment);
		webshopFizetes.getVariables().setIsTransactionID(tranzakcioId);
		webshopFizetes.getVariables().setIsBackURL(backURL);

		webshopFizetesAlairas();
		// Itt megvan az aláírt ojjektumunk

		JAXBContext requestJaxbContext = JAXBContext.newInstance(WebshopFizetes.class);
		Marshaller requestMarshaller = requestJaxbContext.createMarshaller();
		requestMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		StringWriter outHandler = new StringWriter();
		requestMarshaller.marshal(webshopFizetes, outHandler);

		String belsoXml = outHandler.toString();
		// Itt pedig megvan a belső XML

		config.getTrafficLogger().logTraffic(TrafficType.XML_OUT, belsoXml);

		StartWorkflowSynch workflowSynchStub = new StartWorkflowSynch();
		WorkflowState state = workflowSynchStub.call(Constants.HAROMSZEREPLOS_TEMPLATE_NEV, belsoXml, config.getTrafficLogger());

		config.getTrafficLogger().logTraffic(TrafficType.XML_IN, state.getResult());

		// Parse-oljuk fel a választ
		JAXBContext responseJaxbContext = JAXBContext.newInstance(WebshopFizetesOutput.class);
		Unmarshaller responseUnMarshaller = responseJaxbContext.createUnmarshaller();
		webshopFizetesKimenet = (WebshopFizetesOutput) responseUnMarshaller.unmarshal(new StringReader(state.getResult()));

	}

	public WebshopFizetesOutput getWebshopFizetesOutput() {
		return webshopFizetesKimenet;
	}

	public BigInteger getAmount() {
		return webshopFizetes.getVariables().getIsAmount();
	}

	public String getComment() {
		return webshopFizetes.getVariables().getIsShopComment();
	}

	public String getTranzakcioId() {
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

	public String getRedirectUrlString() throws UnsupportedEncodingException {
		return String.format(Constants.OTP_WEBSHOP_CUSTOMER_SIDE_URL, URLEncoder.encode(config.posId, "UTF-8"),
				URLEncoder.encode(webshopFizetes.getVariables().getIsTransactionID(), "UTF-8"), webshopFizetes.getVariables().getIsLanguageCode());
	}
}
