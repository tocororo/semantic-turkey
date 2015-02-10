package it.uniroma2.art.semanticturkey.services.core;

import java.text.ParseException;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.vocabulary.XmlSchema;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;
import it.uniroma2.art.semanticturkey.validators.XSDDatatypeValidator;

@GenerateSTServiceController
@Validated
@Component
public class XMLSchema extends STServiceAdapter{
	
	@GenerateSTServiceController
	public Response formatDateTime(int year, int month, int day, int hour, int minute, int second, @Optional(defaultValue="Z") String offset) throws ParseException {
		String formatted = XmlSchema.formatDateTime(year, month, day, hour, minute, second, offset);
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("formatDateTime",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element dateTimeElement = XMLHelp.newElement(dataElement, "dateTime");
		dateTimeElement.setTextContent(formatted);
		dateTimeElement.setAttribute("validated", XSDDatatypeValidator.isValidDateTime(formatted)+"");
		return response;
	}
	
	@GenerateSTServiceController
	public Response formatDate(int year, int month, int day) throws ParseException {
		String formatted = XmlSchema.formatDate(year, month, day);
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("formatDate",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element dateElement = XMLHelp.newElement(dataElement, "date");
		dateElement.setTextContent(formatted);
		dateElement.setAttribute("validated", XSDDatatypeValidator.isValidDate(formatted)+"");
		return response;
	}
	
	@GenerateSTServiceController
	public Response formatTime(int hour, int minute, int second) throws ParseException {
		String formatted = XmlSchema.formatTime(hour, minute, second);
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("formatTime",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element timeElement = XMLHelp.newElement(dataElement, "time");
		timeElement.setTextContent(formatted);
		timeElement.setAttribute("validated", XSDDatatypeValidator.isValidTime(formatted)+"");
		return response;
	}
	
	@GenerateSTServiceController
	public Response formatDuration(@Optional(defaultValue="") String sign, @Optional(defaultValue="0") int year,
			@Optional(defaultValue="0") int month, @Optional(defaultValue="0") int day, 
			@Optional(defaultValue="0") int hour, @Optional(defaultValue="0") int minute,
			@Optional(defaultValue="0") int second) {
		String formatted = sign + "P" + year + "Y" + month + "M" + day + "DT" + hour + "H" + minute + "M" + second + "S";
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("formatDuration",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element durationElement = XMLHelp.newElement(dataElement, "duration");
		durationElement.setTextContent(formatted);
		durationElement.setAttribute("validated", XSDDatatypeValidator.isValidDuration(formatted)+"");
		return response;
	}
	
	@GenerateSTServiceController
	public Response formatCurrentLocalDateTime(){
		String formatted = XmlSchema.formatCurrentLocalDateTime();
		System.out.println("valid local datetime: " + XSDDatatypeValidator.isValidDateTime(formatted));
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("formatCurrentLocalDateTime",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element dateTimeElement = XMLHelp.newElement(dataElement, "dateTime");
		dateTimeElement.setTextContent(formatted);
		dateTimeElement.setAttribute("validated", XSDDatatypeValidator.isValidDateTime(formatted)+"");
		return response;
	}
	
	@GenerateSTServiceController
	public Response formatCurrentUTCDateTime(){
		String formatted = XmlSchema.formatCurrentUTCDateTime();
		System.out.println("valid local datetime: " + XSDDatatypeValidator.isValidDateTime(formatted));
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("formatCurrentUTCDateTime",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element dateTimeElement = XMLHelp.newElement(dataElement, "dateTime");
		dateTimeElement.setTextContent(formatted);
		dateTimeElement.setAttribute("validated", XSDDatatypeValidator.isValidDateTime(formatted)+"");
		return response;
	}
}
