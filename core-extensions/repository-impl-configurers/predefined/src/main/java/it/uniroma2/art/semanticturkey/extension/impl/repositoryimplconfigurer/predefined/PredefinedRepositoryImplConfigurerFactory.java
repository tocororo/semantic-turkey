package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link PredefinedRepositoryConfigurer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryImplConfigurerFactory implements
		ConfigurableExtensionFactory<PredefinedRepositoryConfigurer, PredefinedConfiguration>,
		ProjectScopedConfigurableComponent<PredefinedConfiguration> {

	@Override
	public String getName() {
		return "Predefined RepositoryImpl Configurer";
	}

	@Override
	public String getDescription() {
		return "Supports predefined configurations for RepositoryImpl";
	}

	@Override
	public PredefinedRepositoryConfigurer createInstance(
			PredefinedConfiguration conf) {
		return new PredefinedRepositoryConfigurer(conf);
	}

	@Override
	public Collection<PredefinedConfiguration> getConfigurations() {
		return Arrays.<PredefinedConfiguration>asList(
				new RDF4JPersistentInMemorySailConfiguration(),
				new RDF4JNativeSailConfiguration(), new GraphDBFreeConfiguration(),
				new GraphDBSEConfiguration());
	}

}
