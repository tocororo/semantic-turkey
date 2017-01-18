package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.configuration.RequiredConfigurationParameter;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DeletePropertyValueExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "Delete Property Value Export Filter";
	}

	@PluginConfigurationParameter(description = "The subject of the filtered out triple")
	@RequiredConfigurationParameter
	public String resource;

	@PluginConfigurationParameter(description = "The predicate of the filtered out triple")
	@RequiredConfigurationParameter
	public String property;

	@PluginConfigurationParameter(description = "The value of the triple being filtered out. If not set, "
			+ "then all triples of the form <resource, predicate, *> are deleted")
	public String value = null;
}
