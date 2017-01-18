package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.configuration.RequiredConfigurationParameter;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "Update Property Value Export Filter";
	}

	@PluginConfigurationParameter(description = "The subject of the filtered triple")
	@RequiredConfigurationParameter
	public String resource;

	@PluginConfigurationParameter(description = "The predicate of the filtered triple")
	@RequiredConfigurationParameter
	public String property;

	@PluginConfigurationParameter(description = "The new value to be set")
	@RequiredConfigurationParameter
	public String value = null;
	
	@PluginConfigurationParameter(description = "if set, the triple <resource, property, oldValue> is "
			+ "replaced by <resource, property, value>. If not set, then all <resource, property, *> are "
			+ "deleted and <resource, property, value> is written")
	public String oldValue = null;

}
