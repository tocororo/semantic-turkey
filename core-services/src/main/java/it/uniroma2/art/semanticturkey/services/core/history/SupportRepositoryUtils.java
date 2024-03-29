package it.uniroma2.art.semanticturkey.services.core.history;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.core.History.SortingDirection;
import it.uniroma2.art.semanticturkey.services.tracker.OperationDescription;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for interacting with the support repository.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class SupportRepositoryUtils {

	public static String conditionalOptional(boolean insertOptional, String innerPattern) {
		if (insertOptional) {
			return "OPTIONAL {\n" + innerPattern + "\n}\n";
		} else {
			return innerPattern;
		}
	}

	public static String addInnerPattern(boolean addPatter, String innerPatter) {
		if (addPatter) {
			return innerPatter;
		} else {
			return "";
		}
	}

	public static String computeTimeBoundsSPARQLFilter(String timeLowerBound, String timeUpperBound,
			String timeLowerBoundeVar, String timeUpperBoundVar) throws IllegalArgumentException {
		String timeLowerBoundSPARQLFilter;
		if (timeLowerBound != null) {
			if (!XMLDatatypeUtil.isValidDateTime(timeLowerBound)) {
				throw new IllegalArgumentException(
						"Time lower bound is not a valid xsd:dateTime lexical form: " + timeLowerBound);
			}

			timeLowerBoundSPARQLFilter = "FILTER(" + timeLowerBoundeVar + " >= " + RenderUtils.toSPARQL(
					SimpleValueFactory.getInstance().createLiteral(timeLowerBound, XSD.DATETIME))
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

			timeUpperBoundSPARQLFilter = "FILTER(" + timeUpperBoundVar + " <= " + RenderUtils.toSPARQL(
					SimpleValueFactory.getInstance().createLiteral(timeUpperBound, XSD.DATETIME))
					+ ")\n";

		} else {
			timeUpperBoundSPARQLFilter = "";
		}

		return timeLowerBoundSPARQLFilter + timeUpperBoundSPARQLFilter;
	}

	public static String computeOrderBySPARQLFragment(SortingDirection operationSorting,
			SortingDirection timeSorting, boolean useRevisionNumber) {
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

		String timeComparisonVar = useRevisionNumber ? "revisionNumber" : "endTime";

		switch (timeSorting) {
		case Ascending:
			orderBy += " ASC(?" + timeComparisonVar + ")";
			break;
		case Descending:
			orderBy += " DESC(?" + timeComparisonVar + ")";
			break;
		default:
		}

		if (!orderBy.isEmpty()) {
			orderBy = "ORDER BY " + orderBy + "\n";
		}
		return orderBy;
	}

	/*
	 * public static String computeOperationSPARQLFilter(IRI[] operationFilter) { String operationSPARQLFilter
	 * = operationFilter.length != 0 ? "FILTER(?operationT IN " +
	 * Arrays.stream(operationFilter).map(RenderUtils::toSPARQL) .collect(Collectors.joining(", ", "(", ")"))
	 * + ")\n" : ""; return operationSPARQLFilter; }
	 */

	public static String computeInCollectionSPARQLFilter(Value[] values, String variableName) {
		return values.length == 0 ? ""
				: "FILTER(?" + variableName + " IN " + Arrays.stream(values).map(RenderUtils::toSPARQL)
						.collect(Collectors.joining(", ", "(", ")")) + ")\n";
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

	public static IRI obtainBlacklistGraph(RepositoryConnection coreRepoConnection)
			throws IllegalStateException, QueryEvaluationException, RepositoryException {
		IRI blacklistGraph = Models
				.objectIRI(
						QueryResults.asModel(coreRepoConnection.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT,
								CHANGETRACKER.BLACKLIST_GRAPH, null, CHANGETRACKER.GRAPH_MANAGEMENT)))
				.orElseThrow(() -> new IllegalStateException(
						"Could not obtain the blacklist graph. Perhaps this project is without blacklist management"));
		return blacklistGraph;
	}

	public static List<ParameterInfo> deserializeOperationParameters(String serializedParameters) {

		if (serializedParameters.isEmpty())
			return Collections.emptyList();
		Pattern pattern = Pattern.compile("(?<!\\\\)(\\\\\\\\)*(?<separator>\\$)");

		Matcher matcher = pattern.matcher(serializedParameters);

		List<String> splitList = new ArrayList<>(16);

		int index = 0;
		while (matcher.find()) {
			int sepIndex = matcher.start("separator");
			splitList.add(serializedParameters.substring(index, sepIndex));
			index = sepIndex + 1;
		}

		if (index < serializedParameters.length()) {
			splitList.add(serializedParameters.substring(index));
		}

		String[] splits = splitList.toArray(new String[splitList.size()]);

		int nParams = splits.length / 2;

		Map<Integer, ParameterInfo> rv = new TreeMap<>();
		for (int i = 0; i < splits.length; i += 2) {
			String pIRI = splits[i].replaceAll("\\\\\\$", Matcher.quoteReplacement("$"))
					.replaceAll("\\\\\\\\", Matcher.quoteReplacement("\\"));
			int startParamIndex = pIRI.lastIndexOf("/param-") + 7;
			int endParamIndex = pIRI.indexOf("-", startParamIndex);

			int pIndex = Integer.parseInt(pIRI.substring(startParamIndex, endParamIndex));

			String pName = pIRI.substring(endParamIndex + 1);

			String pValue = splits[i + 1].replaceAll("\\\\\\$", Matcher.quoteReplacement("$"))
					.replaceAll("\\\\\\\\", Matcher.quoteReplacement("\\"));
			if (StringUtils.equalsAny(pValue, CHANGELOG.NULL.stringValue(), SESAME.NIL.stringValue())) {
				pValue = null;
			}

			rv.put(pIndex, new ParameterInfo(pName, pValue));
		}

		return new ArrayList<>(rv.values());
	}

	public static void computeOperationDisplay(STServiceTracker stServiceTracker,
			AnnotatedValue<IRI> operation) {
		String displayName = stServiceTracker.getOperationDescription(operation.getValue())
				.flatMap(OperationDescription::getDisplayName).filter(s -> !s.isEmpty()).orElseGet(() -> {
					int operationStartIndex = operation.getValue().stringValue().lastIndexOf("/");
					String operationName = operation.getValue().stringValue()
							.substring(operationStartIndex + 1);

					return Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(operationName))
							.map(String::toLowerCase).collect(Collectors.joining(" "));
				});
		operation.setAttribute("show", displayName);
	}
}
