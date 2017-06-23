package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DeletePropertyValueExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "Delete Property Value Export Filter";
	}

	@STProperty(description = "The subject of the filtered out triple")
	@Required
	public String resource;

	@STProperty(description = "The predicate of the filtered out triple")
	@Required
	public String property;

	@STProperty(description = "The value of the triple being filtered out. If not set, "
			+ "then all triples of the form <resource, predicate, *> are deleted")
	public String value = null;
}
