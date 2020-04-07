package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.List;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * The definition of an <em>operation</em> provided by a <em>custom service</em>.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see CustomService
 *
 */
public class Operation implements Configuration {

	@Override
	public String getShortName() {
		return "Custom Service Operation";
	}

	@STProperty(description = "The name of this operation", displayName = "Name")
	@Required
	public String name;
	
	@STProperty(description = "The parameters of this operation", displayName = "Parameters")
	public List<Parameter> parameters;

	@STProperty(description = "The type of the value returned by this operations", displayName = "Return type")
	public Type returns;
	
	@STProperty(description = "The capabilities required for executing this operation", displayName = "Authorization")
	public String authorization;

}
