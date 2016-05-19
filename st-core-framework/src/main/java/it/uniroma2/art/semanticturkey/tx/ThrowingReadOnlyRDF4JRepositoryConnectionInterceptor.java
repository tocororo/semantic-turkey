package it.uniroma2.art.semanticturkey.tx;

import org.openrdf.model.IRI;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryReadOnlyException;
import org.openrdf.repository.event.RepositoryConnectionInterceptor;

public class ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor implements RepositoryConnectionInterceptor {

	@Override
	public boolean close(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean begin(RepositoryConnection conn) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean commit(RepositoryConnection conn) {
		throw new RepositoryReadOnlyException();
	}

	@Override
	public boolean rollback(RepositoryConnection conn) {
		throw new RepositoryReadOnlyException();
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
