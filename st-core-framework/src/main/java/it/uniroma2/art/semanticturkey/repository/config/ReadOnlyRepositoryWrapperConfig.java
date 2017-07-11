package it.uniroma2.art.semanticturkey.repository.config;

import org.eclipse.rdf4j.repository.config.AbstractDelegatingRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;

import it.uniroma2.art.semanticturkey.repository.ReadOnlyRepositoryWrapper;

/**
 * A configuration class for the {@link ReadOnlyRepositoryWrapper} repository.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ReadOnlyRepositoryWrapperConfig extends AbstractDelegatingRepositoryImplConfig {

	public ReadOnlyRepositoryWrapperConfig() {
		super(ReadOnlyRepositoryWrapperFactory.REPOSITORY_TYPE);
	}

	public ReadOnlyRepositoryWrapperConfig(RepositoryImplConfig delegate) {
		super(ReadOnlyRepositoryWrapperFactory.REPOSITORY_TYPE, delegate);
	}

}
