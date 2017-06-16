/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is ART Ontology API.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * ART Ontology API was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about the ART Ontology API can be obtained at 
 * http//art.uniroma2.it/owlart
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;


/*
 * TODO we could create a subclass of ARTURIResource for datatypes, which contains itself the method for
 * recognizing if a given value is valid for that datatype
 * */

/**
 * Vocabulary file for the XML language specification
 * 
 * @author Armando Stellato
 * 
 */
public class XmlSchemaUtils {


	//TODO fare tre array statici: primitiveDatatypes e derivedDatatypes
	// TODO fare isXMLPrimitiveDatatype e isXMLDerivedDatatype e  isXMLDatatype
	
	// UTILITY METHODS
	
	/**
	 * as for {@link #formatDateTime(int, int, int, int, int, int, String)} with last argument equal to "Z"
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @return
	 * @throws ParseException 
	 */
	public static String formatDateTime(int year, int month, int day, int hour, int minute, int second) throws ParseException {
		return formatDateTime(year, month, day, hour, minute, second, "Z");
	}

	/**
	 * formats a dateTime value following ISO-8601 standard
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @param offset
	 * @return
	 * @throws ParseException 
	 */
	public static String formatDateTime(int year, int month, int day, int hour, int minute, int second,
			String offset) throws ParseException {
		String dateString = year + "-" + month + "-" + day + "T" + hour + ":" + minute + ":" + second + offset;
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("yyyy-MM-dd'T'hh:mm:ssXXX");
		Date date = sdf.parse(dateString);
		return sdf.format(date);
	}

	private static SimpleDateFormat iso8601DateTimeLocalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	private static SimpleDateFormat iso8601DateTimeUTCFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	public static String formatDateTime(Date date) {
		return iso8601DateTimeUTCFormat.format(date);
	}

	public static String formatCurrentLocalDateTime() {
		return iso8601DateTimeLocalFormat.format(new Date());
	}

	public static String formatCurrentUTCDateTime() {
		return iso8601DateTimeUTCFormat.format(new Date());
	}
	
	static {
		iso8601DateTimeUTCFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public static String formatDate(int year, int month, int day) throws ParseException{
		String dateString = year + "-" + month + "-" + day;
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("yyyy-MM-dd");
		Date date = sdf.parse(dateString);
		return sdf.format(date);
	}
	
	public static String formatTime(int hour, int minute, int second) throws ParseException{
		String timeString = hour + ":" + minute + ":" + second;
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HH:mm:ss");
		Date date = sdf.parse(timeString);
		return sdf.format(date);
	}
	
	public static String formatDuration(boolean isPositive, int year, int month, int day,
			int hour, int minute, int second) throws ParseException{
		String sign = "";
		if (!isPositive)
			sign = "-";
		return sign + "P" + year + "Y" + month + "M" + day + "DT" + hour + "H" + minute + "M" + second + "S";
	}
	
}
