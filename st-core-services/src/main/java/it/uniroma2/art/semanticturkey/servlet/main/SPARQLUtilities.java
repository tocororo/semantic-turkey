package it.uniroma2.art.semanticturkey.servlet.main;

import java.util.Collection;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;
import org.w3c.dom.DOMException;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;

public class SPARQLUtilities {
	public static String buildResourceQueryWithExplicit(String queryFragment, ARTURIResource workingGraph,
			String... additionalVariables) {
		StringBuilder queryStringBuilder = new StringBuilder();

		queryStringBuilder.append("SELECT DISTINCT ?resource ?explicit");

		for (String aVar : additionalVariables) {
			queryStringBuilder.append(" ?").append(aVar);
		}
		queryStringBuilder.append(" {\n");
		queryStringBuilder.append(queryFragment).append("\n");
		queryStringBuilder.append("OPTIONAL {\n");
		queryStringBuilder.append("GRAPH ").append(RDFNodeSerializer.toNT(workingGraph)).append(" {\n");
		queryStringBuilder.append(queryFragment).append("\n");
		queryStringBuilder
				.append("?resource <http://semanticturkey.uniroma2.it/NOT-A-URI>* ?explicitTemp . \n");
		queryStringBuilder.append("}\n");
		queryStringBuilder.append("}\n");
		queryStringBuilder.append("BIND(BOUND(?explicitTemp) as ?explicit)\n");
		queryStringBuilder.append("}");

		return queryStringBuilder.toString();
	}

	public static String wrapQueryWithGroupConcat(String innerQuery, Collection<String> variables,
			String fragment, String fragmentVariable, String separator) {
		StringBuilder queryStringBuilder = new StringBuilder();

		queryStringBuilder.append("SELECT DISTINCT");

		for (String variable : variables) {
			queryStringBuilder.append(" ?").append(variable);
		}

		queryStringBuilder.append(" (GROUP_CONCAT(DISTINCT ?").append(fragmentVariable)
				.append("Temp;separator=\"").append(SPARQLUtil.encodeString(separator)).append("\") as ?")
				.append(fragmentVariable).append("){\n" + "	{" + innerQuery + "}\n").append(fragment)
				.append("}\n").append("GROUP BY");

		for (String variable : variables) {
			queryStringBuilder.append(" ?").append(variable);
		}

		queryStringBuilder.append("\nHAVING BOUND(?").append(variables.iterator().next()).append(")\n");

		return queryStringBuilder.toString();
	}

	public static String wrapQueryWithMax(String innerQuery, Collection<String> variables, String fragment,
			String fragmentVariable) {
		StringBuilder queryStringBuilder = new StringBuilder();

		queryStringBuilder.append("SELECT DISTINCT");

		for (String variable : variables) {
			queryStringBuilder.append(" ?").append(variable);
		}

		queryStringBuilder.append(" (MAX(?").append(fragmentVariable).append("Temp) as ?")
				.append(fragmentVariable).append("){\n" + "	{" + innerQuery + "}\n").append(fragment)
				.append("}\n").append("GROUP BY");

		for (String variable : variables) {
			queryStringBuilder.append(" ?").append(variable);
		}

		queryStringBuilder.append("\nHAVING BOUND(?").append(variables.iterator().next()).append(")\n");
		return queryStringBuilder.toString();
	}

	public static Collection<STRDFResource> getSTRDFResourcesFromTupleQuery(RDFModel model,
			String queryString) throws DOMException, ModelAccessException, UnsupportedQueryLanguageException,
					MalformedQueryException, QueryEvaluationException, IllegalAccessException {
		TupleQuery query = model.createTupleQuery(queryString);

		try (TupleBindingsIterator it = query.evaluate(true)) {
			Collection<STRDFResource> collection = STRDFNodeFactory.createEmptyResourceCollection();

			while (it.streamOpen()) {
				TupleBindings bindings = it.getNext();
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

}
