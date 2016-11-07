package it.uniroma2.art.semanticturkey.servlet.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.impl.RDFModelImpl;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.rdf4jimpl.factory.ARTModelFactoryRDF4JImpl;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;

public class SPARQLUtilities {

	private static final Logger logger = LoggerFactory.getLogger(SPARQLUtilities.class);
	
	public static Collection<STRDFResource> getSTRDFResourcesFromTupleQuery(RDFModel model, TupleQuery query)
			throws DOMException, ModelAccessException, UnsupportedQueryLanguageException,
			MalformedQueryException, QueryEvaluationException, IllegalAccessException {
		try (TupleBindingsIterator it = query.evaluate(true)) {
			Collection<STRDFResource> collection = STRDFNodeFactory.createEmptyResourceCollection();

			while (it.streamOpen()) {
				TupleBindings bindings = it.getNext();
				
				if (!bindings.hasBinding("resource")) {
					continue;
				}
				
				ARTResource collectionResource = bindings.getBoundValue("resource").asResource();
				boolean isResourceExplicit = bindings.hasBinding("explicit")
						? bindings.getBoundValue("explicit").asLiteral().getLabel().equalsIgnoreCase("true")
						: false;
				String resourceShow = null;

				if (bindings.hasBinding("show")) {
					resourceShow = bindings.getBoundValue("show").asLiteral().getLabel();
				}

				// The adopted SPARQL query based on group_concat returns an empty string when no label is
				// available
				if (resourceShow == null || resourceShow.equals("")) {
					if (collectionResource.isBlank()) {
						resourceShow = "_:" + collectionResource.getNominalValue();
					} else {
						resourceShow = model.getQName(collectionResource.getNominalValue());
						if (resourceShow.equals(collectionResource.getNominalValue())) {
							resourceShow = RDFNodeSerializer.toNT(collectionResource);
						}
					}
				}

				RDFResourceRolesEnum resourceRole = bindings.hasBinding("role")
						? RDFResourceRolesEnum.valueOf(bindings.getBoundValue("role").asLiteral().getLabel())
						: RDFResourceRolesEnum.undetermined;

				STRDFResource stResource = STRDFNodeFactory.createSTRDFResource(collectionResource,
						resourceRole, isResourceExplicit, resourceShow);

				for (String bindingName : bindings.getBindingNames()) {
					if (bindingName.startsWith("info_") && bindings.hasBinding(bindingName)) {
						String propName = bindingName.substring("info_".length());
						String propValue = bindings.getBoundValue(bindingName).getNominalValue();

						stResource.setInfo(propName, propValue);
					}
				}

				collection.add(stResource);
			}

			return collection;
		}
	}

	public static class CountDistinctAggregation {
		private String varName;
		private String aggregatedVarName;

		public CountDistinctAggregation(String varName, String aggregatedVarName) {
			this.varName = varName;
			this.aggregatedVarName = aggregatedVarName;
		}

		public String getSelectTerm() {
			return String.format("(COUNT(DISTINCT ?%s) as ?%s)", varName, aggregatedVarName);
		}
	}

	public static class ResourceQuery {

		private final String resourceVariable;
		private final String queryFragment;

		private final List<String> variables;
		private final Set<String> aggregatedVariables;
		private final List<String> orderingVariables;
		private final List<String> groupingVariables;
		private final Map<String, CountDistinctAggregation> aggregationSpecs;
		private final StringBuilder sb;
		private final RDFModel rdfModel;

		public ResourceQuery(RDFModel rdfModel, String resourceVariable, String queryFragment) {
			this.rdfModel = rdfModel;
			this.resourceVariable = resourceVariable;
			this.variables = new ArrayList<>();
			this.aggregatedVariables = new HashSet<>();
			this.orderingVariables = new ArrayList<>();
			this.groupingVariables = new ArrayList<>();
			this.aggregationSpecs = new HashMap<>();
			this.queryFragment = queryFragment;
			this.variables.add("resource");
			this.variables.add("explicit");
			sb = new StringBuilder();
			sb.append(queryFragment);
			sb.append("OPTIONAL \n{GRAPH ?workingGraph {\n");
			sb.append(queryFragment);
			sb.append("}\n BIND(1 AS ?explicitTemp)}\n");
			sb.append("BIND(BOUND(?explicitTemp) as ?explicit)\n");
		}

		public ResourceQuery addInformation(String fragmentVariable, String fragmentPattern) {
			sb.append(fragmentPattern.replace("%resource%", "?" + resourceVariable));
			variables.add(fragmentVariable);
			return this;
		}

		public ResourceQuery addConcatenatedInformation(String fragmentVariable, String fragmentPattern) {
			sb.append(fragmentPattern.replace("%resource%", "?" + resourceVariable));
			variables.add(fragmentVariable);
			aggregatedVariables.add(fragmentVariable);
			return this;
		}

		public TupleQuery query(ARTResource workingGraph)
				throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
			StringBuilder sb2 = new StringBuilder();

			sb2.append("SELECT DISTINCT");
			for (String varName : variables) {
				if (aggregatedVariables.contains(varName)) {
					CountDistinctAggregation aggregationSpec = aggregationSpecs.get(varName);
					if (aggregationSpec != null) {
						sb2.append(" ").append(aggregationSpec.getSelectTerm());
					} else {
						sb2.append(" (GROUP_CONCAT(DISTINCT ?").append(varName)
								.append("It; separator=\", \") as ?").append(varName).append(")");
					}
				} else {
					sb2.append(" ?").append(varName);
				}
			}
			sb2.append(" {\n");

			sb2.append(sb.toString());

			sb2.append("}\n");

			boolean groupByPresent = false;

			if (!aggregatedVariables.isEmpty()) {
				groupByPresent = true;
				sb2.append("GROUP BY");
				for (String varName : variables) {
					if (!aggregatedVariables.contains(varName)) {
						sb2.append(" ?").append(varName);
					}
				}
			}

			if (!groupingVariables.isEmpty()) {
				if (!groupByPresent) {
					sb2.append("GROUP BY");
					groupByPresent = true;
				}

				for (String varName : groupingVariables) {
					sb2.append(" ?").append(varName);
				}
			}

			sb2.append("\n");
			
			if (groupByPresent) {
				sb2.append("HAVING BOUND(?").append(resourceVariable).append(")\n");
			}

			if (!orderingVariables.isEmpty()) {
				sb2.append("ORDER BY");
				for (String varName : orderingVariables) {
					sb2.append(" ?").append(varName);
				}
			}

			String queryString = sb2.toString();

			logger.debug("query = {}", queryString);
			TupleQuery query = rdfModel.createTupleQuery(queryString);
			query.setBinding("workingGraph", workingGraph);
			return query;
		}

		public ResourceQuery groupBy(String groupingVariable) {
			groupingVariables.add(groupingVariable);
			return this;
		}

		public ResourceQuery countDistinct(String varName, String aggregatedVarName) {
			variables.add(aggregatedVarName);
			aggregatedVariables.add(aggregatedVarName);
			aggregationSpecs.put(aggregatedVarName, new CountDistinctAggregation(varName, aggregatedVarName));
			return this;
		}

		public ResourceQuery orderBy(String orderingVariable) {
			orderingVariables.add(orderingVariable);
			return this;
		}
	}

	public static class UnboundResourceQuery {
		private RDFModel rdfModel;

		public UnboundResourceQuery(RDFModel rdfModel) {
			this.rdfModel = rdfModel;
		}

		public ResourceQuery withPattern(String resourceVariable, String queryFragment) {
			return new ResourceQuery(rdfModel, resourceVariable, queryFragment);
		}
	}

	public static UnboundResourceQuery buildResourceQuery(RDFModel rdfModel) {
		return new UnboundResourceQuery(rdfModel);

	}

	protected static String getShowQueryFragment(String lang) {
		StringBuilder queryStringBuilder = new StringBuilder();

		String propertyPath = "<http://www.w3.org/2008/05/skos-xl#prefLabel>/<http://www.w3.org/2008/05/skos-xl#literalForm>";

		// @formatter:off
		queryStringBuilder.append(
		"	OPTIONAL {\n" +
		"		?resource " + propertyPath + " ?showIt .\n" +
		"		FILTER(LANG(?showIt) = \"" + SPARQLUtil.encodeString(lang) + "\")\n" +
		"	}\n"
		);
		// @formatter:on

		return queryStringBuilder.toString();
	}

	public static void main(String[] args)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		// @formatter:off
		String queryFragment =
			"{\n" +
		    "	FILTER NOT EXISTS {<http://test.it/myOrderedCollection> <http://www.w3.org/2004/02/skos/core#memberList> []}\n" +
			"	<http://test.it/myOrderedCollection> <http://www.w3.org/2004/02/skos/core#member> ?resource .\n" +
			"	{?resource a <http://www.w3.org/2004/02/skos/core#Collection> .} union {?resource a <http://www.w3.org/2004/02/skos/core#OrderedCollection> .}\n" +
			"} UNION {\n" +
			"	<http://test.it/myOrderedCollection> <http://www.w3.org/2004/02/skos/core#memberList> ?memberList .\n" +
			"	?memberList <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>* ?mid .\n" +
			"	?mid <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>* ?node .\n" +
			"	?node <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?resource .\n" +
			"	{?resource a <http://www.w3.org/2004/02/skos/core#Collection> .} union {?resource a <http://www.w3.org/2004/02/skos/core#OrderedCollection> .}\n" +
			"}\n";
		// @formatter:on

		// @formatter:off
		String moreFragment =
				"OPTIONAL {\n" +
				"		%resource% <http://www.w3.org/2004/02/skos/core#member> ?nestedCollection .\n" +
				"	    {?nestedCollection a <http://www.w3.org/2004/02/skos/core#Collection> .} UNION {?nestedCollection a <http://www.w3.org/2004/02/skos/core#OrderedCollection> .}\n" +
				"	}\n" +
				"	BIND(IF(BOUND(?nestedCollection), \"1\", \"0\") as ?info_more)\n";
		// @formatter:on

		String showFragment = getShowQueryFragment("en");

		buildResourceQuery(new RDFModelImpl(new ARTModelFactoryRDF4JImpl().createLightweightRDFModel()))
				.withPattern("resource", queryFragment).addInformation("info_more", moreFragment)
				.addConcatenatedInformation("show", showFragment).groupBy("node")
				.countDistinct("mid", "index").orderBy("index")
				.query(VocabUtilities.nodeFactory.createURIResource("http://test.it/"));
	}
}
