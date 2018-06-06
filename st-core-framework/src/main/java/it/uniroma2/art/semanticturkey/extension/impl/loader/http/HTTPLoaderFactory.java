package it.uniroma2.art.semanticturkey.extension.impl.loader.http;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * The {@link ExtensionFactory} for the the {@link HTTPLoader}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HTTPLoaderFactory implements ExtensionFactory<HTTPLoader>,
		ConfigurableExtensionFactory<HTTPLoader, HTTPLoaderConfiguration>,
		PUScopedConfigurableComponent<HTTPLoaderConfiguration> {

	@Override
	public String getName() {
		return "HTTP Loader";
	}

	@Override
	public String getDescription() {
		return "A loader that uses the HTTP Protocol";
	}

	@Override
	public HTTPLoader createInstance(HTTPLoaderConfiguration conf) {
		return new HTTPLoader(conf);
	}

	@Override
	public Collection<HTTPLoaderConfiguration> getConfigurations() {
		return Arrays.asList(new HTTPLoaderConfiguration());
	}

}
