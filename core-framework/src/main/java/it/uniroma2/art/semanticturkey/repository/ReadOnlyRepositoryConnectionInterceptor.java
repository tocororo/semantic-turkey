package it.uniroma2.art.semanticturkey.repository;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionInterceptor;

/**
 * A {@link RepositoryConnectionInterceptor} that forbids mutation operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ReadOnlyRepositoryConnectionInterceptor implements RepositoryConnectionInterceptor {

	@Override
	public boolean close(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean begin(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
		return false;
	}

	@Override
	public boolean commit(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean rollback(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean clear(RepositoryConnection conn, Resource... contexts) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean setNamespace(RepositoryConnection conn, String prefix, String name) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean removeNamespace(RepositoryConnection conn, String prefix) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean clearNamespaces(RepositoryConnection conn) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI,
			Update operation) {
		throw new RepositoryReadOnlyException();
	}

}
