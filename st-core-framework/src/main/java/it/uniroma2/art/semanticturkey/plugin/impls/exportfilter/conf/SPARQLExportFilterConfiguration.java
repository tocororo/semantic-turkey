package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.configuration.RequiredConfigurationParameter;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLExportFilterConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "SPARQL Update Export Filter";
	}
	
	@PluginConfigurationParameter(description="SPARQL Update implementing the filter")
	@RequiredConfigurationParameter
	public String filter;
	
	@PluginConfigurationParameter(description="Executes the SPARQL query on each graph separately")
	public boolean sliced = true;
}
