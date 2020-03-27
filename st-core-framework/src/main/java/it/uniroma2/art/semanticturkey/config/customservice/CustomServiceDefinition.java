package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.Map;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A <em>custom service</em> definition supports the implementation and deployment of an ST service without
 * writing ordinary Java code.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomServiceDefinition implements Configuration {

	@Override
	public String getShortName() {
		return "Custom Service Definition";
	}

	@STProperty(description = "The name used to group all operations defined by this service", displayName = "Service name")
	@Required
	public String name;

	@STProperty(description = "A description of this custom service", displayName = "Description")
	public String description;

	@STProperty(description = "Definitions of the operations defined by this custom service", displayName = "Operations")
	public Map<String, OperationDefintion> operations;

}
