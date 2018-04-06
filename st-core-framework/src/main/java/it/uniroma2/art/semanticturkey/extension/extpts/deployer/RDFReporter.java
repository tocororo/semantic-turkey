package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandler;

/**
 * An object able to report to an {@link RDFHandler}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@FunctionalInterface
public interface RDFReporter {
	void export(RDFHandler handler);

	static RDFReporter fromRepositoryConnection(RepositoryConnection conn, Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) {
		return handler -> conn.exportStatements(subj, pred, obj, includeInferred, handler, contexts);
	}

	static RDFReporter fromGraphQueryResult(GraphQueryResult result) {
		return handler -> QueryResults.report(result, handler);
	}

	RDFReporter EmptyReporter = new RDFReporter() {

		@Override
		public void export(RDFHandler handler) {
			handler.startRDF();
			handler.endRDF();
		}
	};

}