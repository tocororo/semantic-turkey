package it.uniroma2.art.semanticturkey.services.core;

import java.security.InvalidParameterException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
//import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;


@STService
public class Resources extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Resources.class);
	
	@STServiceOperation
	@Write
	public void updateTriple(IRI resource, IRI property, Value value, Value newValue){
		logger.info("request to update a triple");
		
		String resourceString = "<"+resource.stringValue()+">";
		String propertyString = "<"+property.stringValue()+">";
		
		String valueString = null;
		if(value instanceof IRI){
			valueString = "<"+value.stringValue()+">";
		} else if(value instanceof Literal){
			Literal valueLiteral = (Literal) value;
			if(valueLiteral.getLanguage().isPresent()){
				valueString += "@"+valueLiteral.getLanguage().get();
			} else if(valueLiteral.getDatatype()!= null){
				valueString += "^^<"+valueLiteral.getDatatype().stringValue()+">";
			}
		} else{
			//TODO check if this is the right exception
			throw new InvalidParameterException("the value parameter must be a URI or a Literal");
		}
		
		String newValueString = null;
		if(newValue instanceof IRI){
			newValueString = "<"+newValue.stringValue()+">";
		} else if(value instanceof Literal){
			Literal newValueLiteral = (Literal) newValue;
			if(newValueLiteral.getLanguage().isPresent()){
				newValueString += "@"+newValueLiteral.getLanguage().get();
			} else if(newValueLiteral.getDatatype()!= null){
				newValueString += "^^<"+newValueLiteral.getDatatype().stringValue()+">";
			}
		} else{
			//TODO check if this is the right exception
			throw new InvalidParameterException("the newValue parameter must be a URI or a Literal");
		}
		
		// @formatter:off
		String updateQuery = 
				//remove
				"DELETE DATA { \n"+
				resourceString+" "+propertyString+" "+valueString+"\n" +		
				"}; \n" +
				//add
				"INSERT DATA {\n" +
				resourceString+" "+propertyString+" "+newValueString+"\n" +		
				"}";
		// @formatter:on
		
		Update update = getManagedConnection().prepareUpdate(updateQuery);
		update.execute();
	}

}
