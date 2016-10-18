package it.uniroma2.art.semanticturkey.sparql;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.art.semanticturkey.services.support.QueryBuilderException;

public class TupleQueryShallowModel {

	private final String query;
	private final int projectionInsertionIndex;
	private final int graphPatternInsertionIndex;
	private final int groupByInsertionIndex;

	private final List<String> initialQueryVariables;
	private final List<ProjectionElement> projectionElements;
	private final List<GraphPattern> graphPatterns;
	private final List<String> additionalGroupingVariables;

	/**
	 * 
	 * @param query
	 * @param queryVariables
	 * @param projectionInsertionIndex
	 * @param graphPatternInsertionIndex
	 * @param groupByInsertionIndex
	 *            <code>-1</code> if GROUP BY not present
	 */
	public TupleQueryShallowModel(String query, List<String> queryVariables, int projectionInsertionIndex, int graphPatternInsertionIndex,
			int groupByInsertionIndex) {
		this.query = query;
		this.initialQueryVariables = queryVariables;
		this.projectionInsertionIndex = projectionInsertionIndex;
		this.graphPatternInsertionIndex = graphPatternInsertionIndex;
		this.groupByInsertionIndex = groupByInsertionIndex;
		this.projectionElements = new ArrayList<>();
		this.graphPatterns = new ArrayList<>();
		this.additionalGroupingVariables = new ArrayList<>();
	}
	
	public List<String> getSignature() {
		return initialQueryVariables;
	}

	public String linearize() {
		StringBuilder sb = new StringBuilder();

		sb.append(query.substring(0, projectionInsertionIndex));

		sb.append(projectionElements.stream().map(ProjectionElement::getSPARQLFragment)
				.collect(joining(" ", "", " ")));

		sb.append(query.substring(projectionInsertionIndex, graphPatternInsertionIndex));

		sb.append(graphPatterns.stream().map(GraphPattern::getSPARQLPattern)
				.collect(joining("}\nOPTIONAL{\n", "\nOPTIONAL{\n", "\n}\n")));

		if (groupByInsertionIndex != -1) {
			sb.append(query.substring(graphPatternInsertionIndex, groupByInsertionIndex));
			sb.append(
					additionalGroupingVariables.stream().map(varName -> "?" + varName).collect(joining(" ")));
			sb.append(query.substring(groupByInsertionIndex));
		} else {
			sb.append(query.substring(graphPatternInsertionIndex));
		}

		return sb.toString();
	}

	public TupleQueryShallowModel appendGraphPattern(GraphPattern gp) {
		ProjectionElement pe = gp.getProjectionElement();
		
		if (!this.hasGroupBy() && pe.isAggregate()) {
			throw new IllegalArgumentException("Could not add an aggregate");
		}
		
		projectionElements.add(pe);
		if (this.hasGroupBy() && !pe.isAggregate()) {
			this.appendGroupBy(pe.getTargetVariable());
		}
		
		graphPatterns.add(gp);
		return this;
	}

	public boolean hasGroupBy() {
		return groupByInsertionIndex != -1;
	}

	public TupleQueryShallowModel appendGroupBy(String groupingVariable) throws IllegalArgumentException {
		if (groupByInsertionIndex == -1) {
			throw new IllegalStateException("Query does not have a GROUP BY");
		}
		this.additionalGroupingVariables.add(groupingVariable);

		return this;
	}

	public List<String> getInitialQueryVariables() {
		return initialQueryVariables;
	}

}
