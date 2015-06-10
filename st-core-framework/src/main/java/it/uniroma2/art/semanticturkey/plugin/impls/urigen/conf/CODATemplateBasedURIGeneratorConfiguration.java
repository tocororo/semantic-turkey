package it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf;

import it.uniroma2.art.coda.converters.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODAURIGeneratorFactory;

/**
 * Configuration class for {@link CODAURIGeneratorFactory} that uses the converter {@link TemplateBasedRandomIdGenerator}.
 *
 */
public class CODATemplateBasedURIGeneratorConfiguration extends CODAURIGeneratorConfiguration {
	
	@Override
	public String getShortName() {
		return "CODA-based templated URI generator";
	}

	@PluginConfigurationParameter(description="Template for SKOS concepts")
	public String concept = "c_${rand()}";
	
	@PluginConfigurationParameter(description="Template for SKOS eXtended Labels")
	public String xLabel = "xl_${lang}_${rand()}";
	
	@PluginConfigurationParameter(description="Template for SKOS Defintions")
	public String xDefinition = "xDef_${rand()}";

	@PluginConfigurationParameter(description="Fallback template for any unknown genre of resource")
	public String fallback = "${xRole}_${rand()}";

}