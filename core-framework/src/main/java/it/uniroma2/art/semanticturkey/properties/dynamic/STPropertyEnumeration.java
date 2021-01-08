package it.uniroma2.art.semanticturkey.properties.dynamic;

import java.util.List;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * An enumeration for an {@link STProperties} property.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertyEnumeration implements STProperties {

	@Override
	public String getShortName() {
		return "STProperty enumeration";
	}

	@STProperty(description = "")
	@Required
	public List<String> values;

	@STProperty(description = "")
	@Required
	public boolean open;
}
