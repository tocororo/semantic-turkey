package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.pmki;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;

/**
 * Factory for the instantiation of {@link PMKIConnector}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PMKIConnectorFactory
		implements ConfigurableExtensionFactory<PMKIConnector, PMKIConnectorConfiguration>,
		NonConfigurableExtensionFactory<PMKIConnector> {

	@Override
	public String getName() {
		return "Public Multilingual Knowledge Infrastructure (PMKI)";
	}

	@Override
	public String getDescription() {
		return "A connector for the Public Multilingual Knowledge Infrastructure (PMKI)";
	}

	@Override
	public PMKIConnector createInstance() {
		PMKIConnectorConfiguration conf = new PMKIConnectorConfiguration();
		// predefined credentials for local instances
		conf.email = "admin@vocbench.com";
		conf.password = "admin";
		conf.apiBaseURL = "http://localhost:1979/semanticturkey/";
		return new PMKIConnector(conf);
	}

	@Override
	public PMKIConnector createInstance(PMKIConnectorConfiguration conf) {
		return new PMKIConnector(conf);
	}

	@Override
	public Collection<PMKIConnectorConfiguration> getConfigurations() {
		return Arrays.asList(new PMKIConnectorConfiguration());
	}

}
