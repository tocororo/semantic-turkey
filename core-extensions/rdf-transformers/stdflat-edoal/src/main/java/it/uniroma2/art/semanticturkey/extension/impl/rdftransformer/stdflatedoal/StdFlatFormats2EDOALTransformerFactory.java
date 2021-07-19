package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.stdflatedoal;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link StdFlatFormats2EDOALTransformer}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class StdFlatFormats2EDOALTransformerFactory implements
		ConfigurableExtensionFactory<StdFlatFormats2EDOALTransformer, StdFlatFormats2EDOALTransformerConfiguration>,
		PUScopedConfigurableComponent<StdFlatFormats2EDOALTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.stdflatedoal.StdFlatFormats2EDOALTransformerFactory";
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
	public StdFlatFormats2EDOALTransformer createInstance(
			StdFlatFormats2EDOALTransformerConfiguration conf) {
		return new StdFlatFormats2EDOALTransformer(conf);
	}

	@Override
	public Collection<StdFlatFormats2EDOALTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new StdFlatFormats2EDOALTransformerConfiguration());
	}

}
