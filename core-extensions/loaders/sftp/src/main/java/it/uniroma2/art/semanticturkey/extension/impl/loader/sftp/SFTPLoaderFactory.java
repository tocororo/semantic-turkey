package it.uniroma2.art.semanticturkey.extension.impl.loader.sftp;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * The {@link ExtensionFactory} for the the {@link SFTPLoader}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SFTPLoaderFactory implements ExtensionFactory<SFTPLoader>,
		ConfigurableExtensionFactory<SFTPLoader, SFTPLoderConfiguration>,
		PUScopedConfigurableComponent<SFTPLoderConfiguration> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.loader.sftp.SFTPLoaderFactory";
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
	public SFTPLoader createInstance(SFTPLoderConfiguration conf) {
		return new SFTPLoader(conf);
	}

	@Override
	public Collection<SFTPLoderConfiguration> getConfigurations() {
		return Arrays.asList(new SFTPLoderConfiguration());
	}

}
