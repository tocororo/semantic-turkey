package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link PredefinedRepositoryImplConfigurer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryImplConfigurerFactory implements
		ConfigurableExtensionFactory<PredefinedRepositoryImplConfigurer, PredefinedRepositoryImplConfigurerConfiguration>,
		ProjectScopedConfigurableComponent<PredefinedRepositoryImplConfigurerConfiguration> {

	@Override
	public String getName() {
		return "Predefined RepositoryImpl Configurer";
	}

	@Override
	public String getDescription() {
		return "Supports predefined configurations for RepositoryImpl";
	}

	@Override
	public PredefinedRepositoryImplConfigurer createInstance(
			PredefinedRepositoryImplConfigurerConfiguration conf) {
		return new PredefinedRepositoryImplConfigurer(conf);
	}

	@Override
	public Collection<PredefinedRepositoryImplConfigurerConfiguration> getConfigurations() {
		return Arrays.<PredefinedRepositoryImplConfigurerConfiguration>asList(
				new RDF4JPersistentInMemorySailConfigurerConfiguration(),
				new RDF4JNativeSailConfigurerConfiguration(), new GraphDBFreeConfigurerConfiguration(),
				new GraphDBSEConfigurerConfiguration());
	}

}
