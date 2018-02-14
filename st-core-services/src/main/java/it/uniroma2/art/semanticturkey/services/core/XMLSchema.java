package it.uniroma2.art.semanticturkey.services.core;

import java.text.ParseException;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.xmlschema.FormattedValue;
import it.uniroma2.art.semanticturkey.utilities.XmlSchemaUtils;
import it.uniroma2.art.semanticturkey.validators.XSDDatatypeValidator;

@STService
public class XMLSchema extends STServiceAdapter {

	@STServiceOperation
	public FormattedValue formatDateTime(int year, int month, int day, int hour, int minute, int second,
			@Optional(defaultValue = "Z") String offset) throws ParseException {
		String formatted = XmlSchemaUtils.formatDateTime(year, month, day, hour, minute, second, offset);
		return new FormattedValue("dateTime", formatted, XSDDatatypeValidator.isValidDateTime(formatted));
	}

	@STServiceOperation
	public FormattedValue formatDate(int year, int month, int day) throws ParseException {
		String formatted = XmlSchemaUtils.formatDate(year, month, day);
		return new FormattedValue("date", formatted, XSDDatatypeValidator.isValidDate(formatted));
	}

	@STServiceOperation
	public FormattedValue formatTime(int hour, int minute, int second) throws ParseException {
		String formatted = XmlSchemaUtils.formatTime(hour, minute, second);
		return new FormattedValue("time", formatted, XSDDatatypeValidator.isValidTime(formatted));
	}

	@STServiceOperation
	public FormattedValue formatDuration(@Optional(defaultValue = "true") boolean isPositive,
			@Optional(defaultValue = "0") int year, @Optional(defaultValue = "0") int month,
			@Optional(defaultValue = "0") int day, @Optional(defaultValue = "0") int hour,
			@Optional(defaultValue = "0") int minute, @Optional(defaultValue = "0") int second)
			throws ParseException {
		String formatted = XmlSchemaUtils.formatDuration(isPositive, year, month, day, hour, minute, second);
		return new FormattedValue("duration", formatted, XSDDatatypeValidator.isValidDuration(formatted));
	}

	@STServiceOperation
	public FormattedValue formatCurrentLocalDateTime() {
		String formatted = XmlSchemaUtils.formatCurrentLocalDateTime();
		return new FormattedValue("dateTime", formatted, XSDDatatypeValidator.isValidDateTime(formatted));
	}

	@STServiceOperation
	public FormattedValue formatCurrentUTCDateTime() {
		String formatted = XmlSchemaUtils.formatCurrentUTCDateTime();
		return new FormattedValue("dateTime", formatted, XSDDatatypeValidator.isValidDateTime(formatted));
	}

}
