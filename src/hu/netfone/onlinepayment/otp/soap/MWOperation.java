package hu.netfone.onlinepayment.otp.soap;

import hu.netfone.onlinepayment.otp.TrafficLogger;
import hu.netfone.onlinepayment.otp.TrafficLogger.TrafficType;

import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.ParseException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

public abstract class MWOperation {

	protected static final Integer timeoutMs = 10 * 60 * 1000;

	protected SOAPMessage initializeEnvelope() throws SOAPException {
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage message = messageFactory.createMessage();
		message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
		SOAPPart soapPart = message.getSOAPPart();
		SOAPEnvelope soapEnvelope = soapPart.getEnvelope();

		soapEnvelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
		soapEnvelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");

		return message;
	}

	public SOAPMessage soapCall(SOAPMessage soapRequestMessage, TrafficLogger trafficLogger)
			throws SOAPException, UnsupportedEncodingException, ParserConfigurationException, SAXException, IOException, ParseException {
		// Logging
		ByteArrayOutputStream requestLogStream = new ByteArrayOutputStream();
		soapRequestMessage.writeTo(requestLogStream);
		trafficLogger.logTraffic(TrafficType.SOAP_OUT, requestLogStream.toString("UTF-8"));

		QName mwServiceName = new QName("java:hu.iqsoft.otp.mw.access", "MWAccessPublic");
		QName mwPortName = new QName("java:hu.iqsoft.otp.mw.access", "MWAccessPublicPort");

		 //FIXME: Ez lenne a jó kód, ha a ratyi Axis2 nem csapta volna felül a JAX-RS Implementációnkat
		 String wsdl = "https://www.otpbankdirekt.hu/mwaccesspublic/mwaccess?WSDL";
		 URL url = new URL(wsdl);
		 Service mwService = Service.create(url, mwServiceName);
		 Dispatch<SOAPMessage> mwDispatch = mwService.createDispatch(mwPortName, SOAPMessage.class, Service.Mode.MESSAGE);

		//// FIXME: Ez meg a ratyi Axis2-vel is működő kód
		//Service mwService = Service.create(mwServiceName);
		//mwService.addPort(mwPortName, SOAPBinding.SOAP11HTTP_BINDING, "https://www.otpbankdirekt.hu:443/mwaccesspublic/mwaccess");
		//Dispatch<SOAPMessage> mwDispatch = mwService.createDispatch(mwPortName, SOAPMessage.class, Service.Mode.MESSAGE);

		// Éljenek az implementáció specifikus paraméterek...
		//mwDispatch.getRequestContext().put(org.apache.axis2.transport.http.HTTPConstants.SO_TIMEOUT, timeoutMs);
		mwDispatch.getRequestContext().put("javax.xml.ws.client.connectionTimeout", timeoutMs);
		mwDispatch.getRequestContext().put("javax.xml.ws.client.receiveTimeout", timeoutMs);
		mwDispatch.getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", timeoutMs);
		mwDispatch.getRequestContext().put("com.sun.xml.internal.ws.request.timeout", timeoutMs);


		// Innen standard
		SOAPMessage soapResponseMessage = mwDispatch.invoke(soapRequestMessage);

		// Logging
		ByteArrayOutputStream responseLogStream = new ByteArrayOutputStream();
		soapResponseMessage.writeTo(responseLogStream);
		trafficLogger.logTraffic(TrafficType.SOAP_IN, responseLogStream.toString("UTF-8"));

		return soapResponseMessage;
	}
}
