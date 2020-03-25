package it.uniroma2.art.semanticturkey.config.customservice;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A <em>parameter definition</em> of an operation of a custom service
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ParameterDefinition implements STProperties {

	@Override
	public String getShortName() {
		return "Parameter Definition";
	}
	
	@STProperty(displayName="Name", description="The name of the parameter")
	@Required
	public String name;

	@STProperty(displayName="Required", description="Whether the parameter is required or not")
	@Required
	public Boolean required;
	
	@STProperty(displayName="Schema", description="The schema of the parameter")
	public Schema schema;
}
