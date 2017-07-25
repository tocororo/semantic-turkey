package it.uniroma2.art.semanticturkey.plugin.impls.search;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

public abstract class AbstractSearchStrategy implements SearchStrategy {
	protected RepositoryConnection getThreadBoundTransaction(STServiceContext stServiceContext) {
		return RDF4JRepositoryUtils.getConnection(STServiceContextUtils.getRepostory(stServiceContext),
				false);
	}

}
