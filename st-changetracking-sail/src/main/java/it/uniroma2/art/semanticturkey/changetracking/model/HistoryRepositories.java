package it.uniroma2.art.semanticturkey.changetracking.model;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;

/**
 * Utility class encapsulating common operations with connections to history repositories.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class HistoryRepositories {

	/**
	 * Return the tips of the MASTER branch in the connected history repository. This method throws an
	 * IllegalStateException, if more than one tip is found for MASTER.
	 * 
	 * @param conn
	 * @param historyGraph
	 * @return
	 * @throws IllegalStateException
	 * @throws RDF4JException
	 */
	public static Optional<Resource> getTip(RepositoryConnection conn, IRI historyGraph)
			throws IllegalStateException, RDF4JException {
		TupleQuery tipQuery = conn.prepareTupleQuery(
			// @formatter:off
			" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                   \n" +
			" SELECT ?tip                                                                    \n" +
			" WHERE {                                                                        \n" +
			"   cl:MASTER cl:tip ?tip .                                                      \n" +
			"   FILTER(isIRI(?tip) || isBLANK(?tip))                                         \n" +
			" }                                                                              \n" +
			" LIMIT 2                                                                          "
			// @formatter:on
		);
		SimpleDataset dataset = new SimpleDataset();
		dataset.addDefaultGraph(historyGraph);
		tipQuery.setDataset(dataset);

		tipQuery.setIncludeInferred(false);
		List<Resource> tipList = QueryResults.stream(tipQuery.evaluate()).map(bs -> bs.getValue("tip"))
				.map(Resource.class::cast).collect(toList());

		int tipCount = tipList.size();
		if (tipCount > 1) {
			throw new IllegalStateException("MASTER has an ambiguous tip");
		}

		if (tipCount == 0) {
			return Optional.empty();
		} else {
			return Optional.of(tipList.get(0));
		}
	}

	/**
	 * Returns the statements additions associated with a commit
	 * 
	 * @param conn
	 * @param commit
	 * @param historyGraph
	 * @return
	 */
	public static QueryResult<Statement> getAddedStaments(RepositoryConnection conn, Resource commit,
			IRI historyGraph) {
		return getCommitStaments(conn, commit, CHANGELOG.ADDED_STATEMENT, historyGraph);
	}

	/**
	 * Returns the statements removals associated with a commit
	 * 
	 * @param conn
	 * @param commit
	 * @param historyGraph
	 * @return
	 */
	public static QueryResult<Statement> getRemovedStaments(RepositoryConnection conn, Resource commit,
			IRI historyGraph) {
		return getCommitStaments(conn, commit, CHANGELOG.REMOVED_STATEMENT, historyGraph);
	}

	/**
	 * Helper function returning the statements associated with a commit by a certain property. This function
	 * encapsulate the dereification of statements.
	 * 
	 * @param conn
	 * @param commit
	 * @param statementPredicate
	 * @param historyGraph
	 * @return
	 */
	private static QueryResult<Statement> getCommitStaments(RepositoryConnection conn, Resource commit,
			IRI statementPredicate, IRI historyGraph) {
		TupleQuery tipQuery = conn.prepareTupleQuery(
			// @formatter:off
			" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                   \n" +
			" SELECT ?s ?p ?o ?c                                                             \n" +
			" WHERE {                                                                        \n" +
			"   ?commit ?statementPredicate ?q .                                             \n" +
			"   ?q cl:subject ?s .                                                           \n" +
			"   ?q cl:predicate ?p .                                                         \n" +
			"   ?q cl:object ?o .                                                            \n" +
			"   ?q cl:context ?c .                                                           \n" +
			" }                                                                                "
			// @formatter:on
		);

		tipQuery.setBinding("statementPredicate", statementPredicate);
		tipQuery.setBinding("commit", commit);

		SimpleDataset dataset = new SimpleDataset();
		dataset.addDefaultGraph(historyGraph);
		tipQuery.setDataset(dataset);

		tipQuery.setIncludeInferred(false);
		return new TupleBinding2StatementIteration(tipQuery.evaluate());
	}

	public static Optional<Resource> getParent(RepositoryConnection conn, Resource commit, IRI historyGraph) {
		List<Statement> parentList = QueryResults
				.asList(conn.getStatements(commit, CHANGELOG.PARENT_COMMIT, null, historyGraph));

		int parentSize = parentList.size();
		
		if (parentSize > 1) {
			throw new IllegalStateException(
					"Commit " + NTriplesUtil.toNTriplesString(commit) + " has more than one parent");
		}
		
		return parentSize != 0 ? Optional.of((Resource)parentList.get(0).getObject()) : Optional.empty();
	}
}

class TupleBinding2StatementIteration
		extends ConvertingIteration<BindingSet, Statement, QueryEvaluationException>
		implements QueryResult<Statement> {

	private final ValueFactory vf;

	public TupleBinding2StatementIteration(
			Iteration<? extends BindingSet, ? extends QueryEvaluationException> iter) {
		super(iter);
		vf = SimpleValueFactory.getInstance();
	}

	@Override
	protected Statement convert(BindingSet sourceObject) throws QueryEvaluationException {
		Resource subject = (Resource) Objects.requireNonNull(sourceObject.getValue("s"),
				"variable ?s may not be null");
		IRI predicate = (IRI) Objects.requireNonNull(sourceObject.getValue("p"),
				"variable ?p may not be null");
		Value object = Objects.requireNonNull(sourceObject.getValue("o"), "variable ?o may not be null");
		Resource context = (Resource) Objects.requireNonNull(sourceObject.getValue("c"),
				"variable ?c may not be null");
		if (SESAME.NIL.equals(context)) {
			context = null;
		}
		return vf.createStatement(subject, predicate, object, context);
	}

}
