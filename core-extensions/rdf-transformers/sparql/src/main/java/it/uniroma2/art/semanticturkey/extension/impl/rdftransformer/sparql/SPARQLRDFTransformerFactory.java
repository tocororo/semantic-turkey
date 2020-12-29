package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link SPARQLRDFTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLRDFTransformerFactory
		implements ConfigurableExtensionFactory<SPARQLRDFTransformer, SPARQLRDFTransformerConfiguration>,
		PUScopedConfigurableComponent<SPARQLRDFTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql.SPARQLRDFTransformerFactory";
		private static final String name = keyBase + ".name";
		private static final String description = keyBase + ".description";
	}

	@Override
	public String getName() {
		return STMessageSource.getMessage(MessageKeys.name);
	}

	@Override
	public String getDescription() {
		return STMessageSource.getMessage(MessageKeys.description);
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
