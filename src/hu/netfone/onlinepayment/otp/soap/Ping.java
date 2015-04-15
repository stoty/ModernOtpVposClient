package hu.netfone.onlinepayment.otp.soap;

import hu.netfone.onlinepayment.otp.TrafficLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Ping extends MWOperation {
	
	protected SOAPMessage generateMessage() throws SOAPException {
		SOAPMessage soapMessage = initializeEnvelope();
		SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
		
		SOAPBody soapBody = soapEnvelope.getBody();
		soapBody.addBodyElement(soapEnvelope.createName("ping", "ns1", "urn:MWAccess"));
		
		return soapMessage;
	}
	
	public boolean call(TrafficLogger trafficLogger) throws SOAPException, UnsupportedEncodingException, ParserConfigurationException, SAXException, IOException, ParseException {
		SOAPMessage request = generateMessage();
		SOAPMessage response = soapCall(request, trafficLogger);
		return parseResponse(response);
	}

	private static boolean parseResponse(SOAPMessage responseMessage) throws ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException, ParseException, SOAPException{

		SOAPBody document = responseMessage.getSOAPBody();		

		NodeList pingResponses = document.getElementsByTagNameNS("java:hu.iqsoft.otp.mw.access", "pingResponse");
		
		if(pingResponses.getLength()==1){
			return true;
		} else {
			return false;
		}
	}
}
