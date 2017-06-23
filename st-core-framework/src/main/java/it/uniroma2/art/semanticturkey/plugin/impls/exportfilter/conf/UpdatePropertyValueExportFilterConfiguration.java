package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "Update Property Value Export Filter";
	}

	@STProperty(description = "The subject of the filtered triple")
	@Required
	public String resource;

	@STProperty(description = "The predicate of the filtered triple")
	@Required
	public String property;

	@STProperty(description = "The new value to be set")
	@Required
	public String value = null;
	
	@STProperty(description = "if set, the triple <resource, property, oldValue> is "
			+ "replaced by <resource, property, value>. If not set, then all <resource, property, *> are "
			+ "deleted and <resource, property, value> is written")
	public String oldValue = null;

}
