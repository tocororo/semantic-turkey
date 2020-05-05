package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link SPARQLRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLRDFTransformerFactory
		implements ConfigurableExtensionFactory<SPARQLRDFTransformer, SPARQLRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<SPARQLRDFTransformerConfiguration> {

	@Override
	public String getName() {
		return "SPARQL RDF Transformer";
	}

	@Override
	public String getDescription() {
		return "An RDF transformer that can be configured through a SPARQL Update";
	}

	@Override
	public SPARQLRDFTransformer createInstance(SPARQLRDFTransformerConfiguration conf) {
		return new SPARQLRDFTransformer(conf);
	}

	@Override
	public Collection<SPARQLRDFTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new SPARQLRDFTransformerConfiguration());
	}

}
