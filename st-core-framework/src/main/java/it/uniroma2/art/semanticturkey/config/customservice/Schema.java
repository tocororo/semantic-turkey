package it.uniroma2.art.semanticturkey.config.customservice;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A schema
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class Schema implements STProperties {

	@Override
	public String getShortName() {
		return "Schema";
	}

	@STProperty(displayName = "Type", description = "The base type associated with this schema")
	public String type;

}
