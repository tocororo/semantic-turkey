package it.uniroma2.art.semanticturkey.config.customservice;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * The definition of <em>parameter</em> of a custom service operation
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see Operation
 */
public class Parameter implements STProperties {

	@Override
	public String getShortName() {
		return "Parameter";
	}

	@STProperty(displayName = "Name", description = "The name of this parameter")
	@Required
	public String name;

	@STProperty(displayName = "Required", description = "Whether this parameter is required or not")
	@Required
	public Boolean required;

	@STProperty(displayName = "Type", description = "The type of this parameter")
	@Required
	public Type type;
}
