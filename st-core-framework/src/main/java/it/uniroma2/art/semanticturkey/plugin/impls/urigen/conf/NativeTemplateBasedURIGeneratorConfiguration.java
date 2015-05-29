package it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;

/**
 * Configuration class for {@link NativeTemplateBasedURIGeneratorFactory}.
 */
public class NativeTemplateBasedURIGeneratorConfiguration extends AbstractPluginConfiguration {

	/**
	 * Contract URL for random ID generation.
	 */
	public static final String CODA_RANDOM_ID_GENERATOR_CONTRACT = "http://art.uniroma2.it/coda/contracts/randIdGen";

	@Override
	public String getShortName() {
		return "Native template-based";
	}

	@PluginConfigurationParameter(description="Template for SKOS concepts")
	public String concept = "c_${rand()}";
	
	@PluginConfigurationParameter(description="Template for SKOS eXtended Labels")
	public String xLabel = "xl_${lang}_${rand()}";
	
	@PluginConfigurationParameter(description="Template for SKOS Defintions")
	public String xDefinition = "xDef_${rand()}";

	@PluginConfigurationParameter(description="Fallback template for any unknown genre of resource")
	//public String fallback = "res_${rand()}";
	public String fallback = "${xRole}_${rand()}";
}
