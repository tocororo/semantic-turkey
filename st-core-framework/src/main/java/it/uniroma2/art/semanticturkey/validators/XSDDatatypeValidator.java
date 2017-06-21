package it.uniroma2.art.semanticturkey.validators;


import org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import org.apache.xerces.impl.dv.xs.AnyURIDV;
import org.apache.xerces.impl.dv.xs.Base64BinaryDV;
import org.apache.xerces.impl.dv.xs.BooleanDV;
import org.apache.xerces.impl.dv.xs.DateDV;
import org.apache.xerces.impl.dv.xs.DateTimeDV;
import org.apache.xerces.impl.dv.xs.DayDV;
import org.apache.xerces.impl.dv.xs.DecimalDV;
import org.apache.xerces.impl.dv.xs.DoubleDV;
import org.apache.xerces.impl.dv.xs.DurationDV;
import org.apache.xerces.impl.dv.xs.FloatDV;
import org.apache.xerces.impl.dv.xs.IntegerDV;
import org.apache.xerces.impl.dv.xs.MonthDV;
import org.apache.xerces.impl.dv.xs.MonthDayDV;
import org.apache.xerces.impl.dv.xs.QNameDV;
import org.apache.xerces.impl.dv.xs.TimeDV;
import org.apache.xerces.impl.dv.xs.YearDV;
import org.apache.xerces.impl.dv.xs.YearMonthDV;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

public class XSDDatatypeValidator {
	
	/**
	 * Checks if a <code>value</code> is valid for the given <code>type</code>.
	 * This method simply invoke <code>isValid&ltSomeType&gt(String value)</code> associated to the
	 * <code>type</code>
	 * @param value
	 * @param type
	 * @return
	 */
	public static boolean isValid(String value, String type){
		if (type.equals(XMLSchema.STRING.stringValue())){
			return true;
		} else if (type.equals(XMLSchema.ANYURI.stringValue())){
			return isValidAnyURI(value);
		} else if (type.equals(XMLSchema.BASE64BINARY.stringValue())){
			return isValidBase64Binary(value);
		} else if (type.equals(XMLSchema.BOOLEAN.stringValue())){
			return isValidBoolean(value);
		} else if (type.equals(XMLSchema.DATE.stringValue())){
			return isValidDate(value);
		} else if (type.equals(XMLSchema.DATETIME.stringValue())){
			return isValidDateTime(value);
		} else if (type.equals(XMLSchema.DECIMAL.stringValue())){
			return isValidDecimal(value);
		} else if (type.equals(XMLSchema.DOUBLE.stringValue())){
			return isValidDouble(value);
		} else if (type.equals(XMLSchema.DURATION.stringValue())){
			return isValidDuration(value);
		} else if (type.equals(XMLSchema.FLOAT.stringValue())){
			return isValidFloat(value);
		} else if (type.equals(XMLSchema.GDAY.stringValue())){
			return isValidGDay(value);
		} else if (type.equals(XMLSchema.GMONTH.stringValue())){
			return isValidGMonth(value);
		} else if (type.equals(XMLSchema.GMONTHDAY.stringValue())){
			return isValidGMonthDay(value);
		} else if (type.equals(XMLSchema.GYEAR.stringValue())){
			return isValidGYear(value);
		} else if (type.equals(XMLSchema.GYEARMONTH.stringValue())){
			return isValidGYearMonth(value);
		} else if (type.equals(XMLSchema.HEXBINARY.stringValue())){
			return isValidHexBinary(value);
		} else if (type.equals(XMLSchema.INTEGER.stringValue())){
			return isValidInteger(value);
		} else if (type.equals(XMLSchema.QNAME.stringValue())){
			return isValidQName(value);
		} else if (type.equals(XMLSchema.TIME.stringValue())){
			return isValidTime(value);
		}
		return true;
	}
	
	public static boolean isValidAnyURI(String anyURI){
		try {
			new AnyURIDV().getActualValue(anyURI, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidBase64Binary(String base64binary){
		try {
			new Base64BinaryDV().getActualValue(base64binary, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidBoolean(String vBoolean){
		try {
			new BooleanDV().getActualValue(vBoolean, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidDate(String date){
		try {
			new DateDV().getActualValue(date, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidDateTime(String datetime){
		try {
			new DateTimeDV().getActualValue(datetime, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidDecimal(String decimal){
		try {
			new DecimalDV().getActualValue(decimal, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidDouble(String vDouble){
		try {
			new DoubleDV().getActualValue(vDouble, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}

	public static boolean isValidDuration(String duration){
		try {
			new DurationDV().getActualValue(duration, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidFloat(String vFloat){
		try {
			new FloatDV().getActualValue(vFloat, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidGDay(String day){
		try {
			new DayDV().getActualValue(day, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidGMonth(String month){
		try {
			new MonthDV().getActualValue(month, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidGMonthDay(String monthDay){
		try {
			new MonthDayDV().getActualValue(monthDay, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidGYear(String year){
		try {
			new YearDV().getActualValue(year, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidGYearMonth(String yearMonth){
		try {
			new YearMonthDV().getActualValue(yearMonth, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidHexBinary(String hexBinary){
		try {
			new DateDV().getActualValue(hexBinary, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidInteger(String integer){
		try {
			new IntegerDV().getActualValue(integer, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidTime(String time){
		try {
			new TimeDV().getActualValue(time, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	
	public static boolean isValidQName(String qName){
		try {
			new QNameDV().getActualValue(qName, null);
			return true;
		} catch (InvalidDatatypeValueException e) {
			return false;
		}
	}
	

}
