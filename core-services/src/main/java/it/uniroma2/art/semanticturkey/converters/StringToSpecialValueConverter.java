package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.SpecialValue;

public class StringToSpecialValueConverter implements Converter<String, SpecialValue> {
	
	@Autowired
	private ApplicationContext applicationContext;

	private ConversionService conversionService;
	private final String conversionServiceId;
	
	public StringToSpecialValueConverter(String conversionServiceId) {
		this.conversionServiceId = conversionServiceId;
	}

	@Override
	public SpecialValue convert(String source) {
		SpecialValue sv = new SpecialValue();
		conversionService = (ConversionService) applicationContext.getBean(conversionServiceId);
		/*
		 * Try to convert the String to a SpecialValue testing the converters
		 * - String -> CustomFormValue
		 * - String -> Value (rdf4j)
		 * The choice of the order is due since if both converter fail, it throws the exception about the 
		 * conversion to Value (otherwise it would throw the exception about the conversion to CustomFormValue
		 * that is not so clear)
		 */
		try { //try to convert to CustomFormValue
			CustomFormValue cfValue = conversionService.convert(source, CustomFormValue.class);
			sv.setCustomFormValue(cfValue);
		} catch (ConversionFailedException e) { //in case of conversion failed, try to convert from String to Value (rdf4j)
			Value value = conversionService.convert(source, Value.class);
			sv.setRdf4jValue(value);
			
		}
		return sv;
	}

}
