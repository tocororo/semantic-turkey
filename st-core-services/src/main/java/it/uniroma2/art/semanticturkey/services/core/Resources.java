package it.uniroma2.art.semanticturkey.services.core;

import java.security.InvalidParameterException;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;


@STService
public class Resources extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Resources.class);
	
	@STServiceOperation
	@Write
	public void updateTriple(Resource resource, IRI property, Value value, Value newValue){
		logger.info("request to update a triple");
		
		if(resource instanceof BNode){
			//TODO check if this is the right exception
			throw new InvalidParameterException("the resource parameter must be a URI");
		}
		String resourceString = SPARQLHelp.toSPARQL(resource);

		String propertyString = SPARQLHelp.toSPARQL(property);
		
		if(value instanceof BNode){
			//TODO check if this is the right exception
			throw new InvalidParameterException("the value parameter must be a URI or a Literal");
		}
		String valueString = SPARQLHelp.toSPARQL(value);
		
		if(newValue instanceof BNode){
			//TODO check if this is the right exception
			throw new InvalidParameterException("the newValue parameter must be a URI or a Literal");
		}
		String newValueString = SPARQLHelp.toSPARQL(newValue);
		
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
