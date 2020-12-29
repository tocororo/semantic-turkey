package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link PredefinedRepositoryConfigurer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PredefinedRepositoryImplConfigurerFactory implements
		ConfigurableExtensionFactory<PredefinedRepositoryConfigurer, PredefinedConfiguration>,
		ProjectScopedConfigurableComponent<PredefinedConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurerFactory";
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
