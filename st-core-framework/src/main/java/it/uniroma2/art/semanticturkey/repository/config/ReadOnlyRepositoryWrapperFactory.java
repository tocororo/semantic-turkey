package it.uniroma2.art.semanticturkey.repository.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.repository.ReadOnlyRepositoryWrapper;

/**
 * Factory for {@link ReadOnlyRepositoryWrapper}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ReadOnlyRepositoryWrapperFactory implements RepositoryFactory {
	private static final Logger logger = LoggerFactory.getLogger(ReadOnlyRepositoryWrapperFactory.class);

	public static final String REPOSITORY_TYPE = "http://semanticturkey.uniroma2.it/repository/readonlyrepositorywrapper";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new ReadOnlyRepositoryWrapperConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (config instanceof ReadOnlyRepositoryWrapperConfig) {
			RepositoryImplConfig delegateRepoImplConfig = ((ReadOnlyRepositoryWrapperConfig) config)
					.getDelegate();

			RepositoryFactory delegateFactory = RepositoryRegistry.getInstance()
					.get(delegateRepoImplConfig.getType()).orElseThrow(() -> new RepositoryConfigException(
							"Unrecognized repository type: " + delegateRepoImplConfig.getType()));
			Repository delegateRepository = delegateFactory.getRepository(delegateRepoImplConfig);
			return new ReadOnlyRepositoryWrapper(delegateRepository);
		}

		throw new RepositoryConfigException("Invalid configuration class: " + config.getClass());
	}

	/**
	 * Static hook for the registration of this factory to the RDF4J infrastructure. The necessity of this
	 * hook is determined by the fact that under OSGi files under "META-INF/service" are not correctly
	 * recognized.
	 */
	public static void registerFactory() {
		RepositoryRegistry.getInstance().add(new ReadOnlyRepositoryWrapperFactory());
	}

}
