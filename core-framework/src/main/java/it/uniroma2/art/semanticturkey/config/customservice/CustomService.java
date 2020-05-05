package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.List;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * The definition of a <em>custom service</em>, which supports the implementation and deployment of an ST
 * service without the need to write, build and deploy Java code.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomService implements Configuration {

	@Override
	public String getShortName() {
		return "Custom Service";
	}

	@STProperty(description = "The name used to group all operations defined by this custom service", displayName = "Service name")
	@Required
	public String name;

	@STProperty(description = "A description of this custom service", displayName = "Description")
	public String description;

	@STProperty(description = "The operations provided by this custom service", displayName = "Operations")
	public List<Operation> operations;

}
