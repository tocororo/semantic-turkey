package it.uniroma2.art.semanticturkey.repository;

import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.event.base.InterceptingRepositoryWrapper;

/**
 * A {@link DelegatingRepository} that wraps another {@link Repository} forbudding any mutation operation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ReadOnlyRepositoryWrapper extends InterceptingRepositoryWrapper {

	public ReadOnlyRepositoryWrapper(Repository delegate) {
		super(delegate);
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return false;
	}

	@Override
	public void initialize() throws RepositoryException {
		addRepositoryConnectionInterceptor(new ReadOnlyRepositoryConnectionInterceptor());
		super.initialize();
	}

}
