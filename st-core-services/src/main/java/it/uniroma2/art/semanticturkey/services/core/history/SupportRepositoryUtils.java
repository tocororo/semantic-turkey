package it.uniroma2.art.semanticturkey.services.core.history;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.core.History.SortingDirection;

/**
 * Utility class for interacting with the support repository.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SupportRepositoryUtils {
	public static String computeTimeBoundsSPARQLFilter(String timeLowerBound, String timeUpperBound)
			throws IllegalArgumentException {
		String timeLowerBoundSPARQLFilter;
		if (timeLowerBound != null) {
			if (!XMLDatatypeUtil.isValidDateTime(timeLowerBound)) {
				throw new IllegalArgumentException(
						"Time lower bound is not a valid xsd:dateTime lexical form: " + timeLowerBound);
			}

			timeLowerBoundSPARQLFilter = "FILTER(?endTime >= " + RenderUtils.toSPARQL(
					SimpleValueFactory.getInstance().createLiteral(timeLowerBound, XMLSchema.DATETIME))
					+ ")\n";

		} else {
			timeLowerBoundSPARQLFilter = "";
		}

		String timeUpperBoundSPARQLFilter;
		if (timeUpperBound != null) {
			if (!XMLDatatypeUtil.isValidDateTime(timeUpperBound)) {
				throw new IllegalArgumentException(
						"Time lower bound is not a valid xsd:dateTime lexical form: " + timeUpperBound);
			}

			timeUpperBoundSPARQLFilter = "FILTER(?endTime <= " + RenderUtils.toSPARQL(
					SimpleValueFactory.getInstance().createLiteral(timeUpperBound, XMLSchema.DATETIME))
					+ ")\n";

		} else {
			timeUpperBoundSPARQLFilter = "";
		}

		return timeLowerBoundSPARQLFilter + timeUpperBoundSPARQLFilter;
	}

	public static String computeOrderBySPARQLFragment(SortingDirection operationSorting,
			SortingDirection timeSorting) {
		String orderBy = "";

		switch (operationSorting) {
		case Ascending:
			orderBy += " ASC(?operation)";
			break;
		case Descending:
			orderBy += " DESC(?operation)";
			break;
		default:
		}

		switch (timeSorting) {
		case Ascending:
			orderBy += " ASC(?revisionNumber)";
			break;
		case Descending:
			orderBy += " DESC(?revisionNumber)";
			break;
		default:
		}

		if (!orderBy.isEmpty()) {
			orderBy = "ORDER BY " + orderBy + "\n";
		}
		return orderBy;
	}

	public static String computeOperationSPARQLFilter(IRI[] operationFilter) {
		String operationSPARQLFilter = operationFilter.length != 0
				? "FILTER(?operation IN " + Arrays.stream(operationFilter).map(RenderUtils::toSPARQL)
						.collect(Collectors.joining(", ", "(", ")")) + ")\n"
				: "";
		return operationSPARQLFilter;
	}

	public static IRI obtainHistoryGraph(RepositoryConnection coreRepoConnection)
			throws IllegalStateException, QueryEvaluationException, RepositoryException {
		IRI historyGraph = Models
				.objectIRI(
						QueryResults.asModel(coreRepoConnection.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT,
								CHANGETRACKER.HISTORY_GRAPH, null, CHANGETRACKER.GRAPH_MANAGEMENT)))
				.orElseThrow(() -> new IllegalStateException(
						"Could not obtain the history graph. Perhaps this project is without history"));
		return historyGraph;
	}

	public static IRI obtainValidationGraph(RepositoryConnection coreRepoConnection)
			throws IllegalStateException, QueryEvaluationException, RepositoryException {
		IRI historyGraph = Models
				.objectIRI(
						QueryResults.asModel(coreRepoConnection.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT,
								CHANGETRACKER.VALIDATION_GRAPH, null, CHANGETRACKER.GRAPH_MANAGEMENT)))
				.orElseThrow(() -> new IllegalStateException(
						"Could not obtain the validation graph. Perhaps this project is without validation"));
		return historyGraph;
	}
}
