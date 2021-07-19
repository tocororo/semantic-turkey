package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalstdflat;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link EDOAL2StdFlatFormatsTransformer}.
 * 
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class EDOAL2StdFlatFormatsTransformerFactory implements
		ConfigurableExtensionFactory<EDOAL2StdFlatFormatsTransformer, EDOAL2StdFlatFormatsTransformerConfiguration>,
		PUScopedConfigurableComponent<EDOAL2StdFlatFormatsTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalstdflat.EDOAL2StdFlatFormatsTransformerFactory";
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
	public EDOAL2StdFlatFormatsTransformer createInstance(
			EDOAL2StdFlatFormatsTransformerConfiguration conf) {
		return new EDOAL2StdFlatFormatsTransformer(conf);
	}

	@Override
	public Collection<EDOAL2StdFlatFormatsTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new EDOAL2StdFlatFormatsTransformerConfiguration());
	}

}
