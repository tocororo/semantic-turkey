package it.uniroma2.art.semanticturkey.extension.impl.loader.sftp;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link SFTPLoader}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SFTPLoaderFactory implements ExtensionFactory<SFTPLoader>,
		ConfigurableExtensionFactory<SFTPLoader, SFTPLoderConfiguration>,
		PUScopedConfigurableComponent<SFTPLoderConfiguration> {

	@Override
	public String getName() {
		return "SFTP Loader";
	}

	@Override
	public String getDescription() {
		return "A loader that uses the SFTP Protocol";
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
