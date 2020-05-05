package it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf;

import it.uniroma2.art.coda.converters.impl.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODAURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link CODAURIGeneratorFactory} that uses the converter {@link TemplateBasedRandomIdGenerator}.
 *
 */
public class CODATemplateBasedURIGeneratorConfiguration extends CODAURIGeneratorConfiguration {
	
	@Override
	public String getShortName() {
		return "CODA-based templated URI generator";
	}

	@STProperty(description="Template for SKOS concepts")
	public String concept = "c_${rand()}";
	
	@STProperty(description="Template for SKOS eXtended Labels")
	public String xLabel = "xl_${lexicalForm.language}_${rand()}";
	
	@STProperty(description="Template for SKOS Definitions")
	public String xNote = "xNote_${rand()}";

	@STProperty(description="Fallback template for any unknown genre of resource")
	public String fallback = "${xRole}_${rand()}";

}