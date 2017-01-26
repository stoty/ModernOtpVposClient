package hu.netfone.onlinepayment.otp.soap;

import hu.netfone.onlinepayment.otp.TrafficLogger;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

public class StartWorkflowSynch extends MWOperation {

	public WorkflowState call(String templateName, String innerXML, TrafficLogger trafficLogger)
			throws SOAPException, UnsupportedEncodingException, ParserConfigurationException, SAXException, IOException, ParseException {
		SOAPMessage request = generateMessage(templateName, innerXML);
		SOAPMessage response = soapCall(request, trafficLogger);
		return parseResponse(response);
	}

	protected SOAPMessage generateMessage(String templateName, String innerXml) throws SOAPException {
		SOAPMessage soapMessage = initializeEnvelope();
		SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();

		SOAPBody soapBody = soapEnvelope.getBody();
		SOAPElement startWF = soapBody.addBodyElement(soapEnvelope.createName("startWorkflowSynch", "ns1", "urn:MWAccess"));
		startWF.addChildElement("arg0").addTextNode(templateName);
		startWF.addChildElement("arg1").addTextNode(innerXml);

		return soapMessage;
	}

	private static WorkflowState parseResponse(SOAPMessage responseMessage)
			throws ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException, ParseException, SOAPException {

		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");

		WorkflowState response = new WorkflowState();

		SOAPBody document = responseMessage.getSOAPBody();

		String rawResult = document.getElementsByTagName("result").item(0).getTextContent();
		byte[] resultDecodedBytes = DatatypeConverter.parseBase64Binary(rawResult);
		String resultXml = new String(resultDecodedBytes);
		response.setResult(resultXml);

		// Itt biztos ban valami tool, ami automatán bekonvertálná, csak tartok tőle, hogy Axis1-nek hívják.

		String rawTemplateName = document.getElementsByTagName("templateName").item(0).getTextContent();
		response.setTemplateName(rawTemplateName);

		String rawEndTime = document.getElementsByTagName("endTime").item(0).getTextContent();
		response.setEndTime(df.parse(rawEndTime));

		String rawCompleted = document.getElementsByTagName("completed").item(0).getTextContent();
		response.setCompleted(rawCompleted.toLowerCase().equals("true"));

		String rawStartTime = document.getElementsByTagName("startTime").item(0).getTextContent();
		response.setStartTime(df.parse(rawStartTime));

		String rawInstanceId = document.getElementsByTagName("instanceId").item(0).getTextContent();
		response.setInstanceId(Long.parseLong(rawInstanceId));

		String rawTimeout = document.getElementsByTagName("timeout").item(0).getTextContent();
		response.setTimeout(rawTimeout.toLowerCase().equals("true"));

		return response;
	}
}
