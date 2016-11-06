package it.uniroma2.art.semanticturkey.services.core.resourceview;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

public abstract class AbstractStatementConsumer implements StatementConsumer {
	private static final Literal DEFAULT_ROLE = SimpleValueFactory.getInstance()
			.createLiteral(RDFResourceRolesEnum.undetermined.toString());

	public static Literal computeRole(Map<Resource, Map<String, Value>> resource2attributes,
			Resource resource) {
		return (Literal) resource2attributes.getOrDefault(resource, Collections.emptyMap())
				.getOrDefault("role", DEFAULT_ROLE);
	}

	public static String computeDefaultShow(Resource resource) {
		if (resource instanceof BNode) {
			return "_:" + resource.stringValue();
		} else {
			return resource.stringValue();
		}
	}

	public static String computeGraphs(Set<Resource> graphs) {
		return graphs.stream().map(g -> g == null ? "MAINGRAPH" : g.toString()).collect(joining(","));
	}

	@SuppressWarnings("unchecked")
	public static String computeShow(Resource resource, Map<Resource, Map<String, Value>> resource2attributes,
			Model statements) {
		Map<String, String> rv = null;

		if (resource instanceof BNode) {
			if (statements.contains(resource, RDF.TYPE, RDF.LIST)) {
				ArrayList<Value> values = RDFCollections.asValues(statements, resource, new ArrayList<>());

				return values.stream().map(v -> {
					if (v instanceof Resource) {
						return computeShow((Resource) v, resource2attributes, statements);
					} else {
						return NTriplesUtil.toNTriplesString(v);
					}
				}).collect(joining(", ", "[", "]"));
			}
		} else {
			Map<String, Value> resourceAttributes = resource2attributes.get(resource);

			if (resourceAttributes != null) {
				Value show = resourceAttributes.get("show");
				if (show != null) {
					return show.stringValue();
				}
			}
		}

		return computeDefaultShow(resource);
	}

	public static void addShowOrRenderXLabel(AnnotatedValue<? extends Resource> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes, Model statements) {
		Resource resource = annotatedResource.getValue();

		Map<String, Value> resourceAttributes = resource2attributes.get(resource);
		if (resourceAttributes != null) {
			Value literalForm = resourceAttributes.get("literalForm");
			if (literalForm != null) {
				Literal literalFormAsLiteral = (Literal) literalForm;
				annotatedResource.setAttribute("show", literalFormAsLiteral.getLabel());
				literalFormAsLiteral.getLanguage().ifPresent(v -> annotatedResource.setAttribute("lang", v));
				return;
			}
		}
		annotatedResource.setAttribute("show", computeShow(resource, resource2attributes, statements));
	}

	public static void addRole(AnnotatedValue<? extends Resource> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes) {
		annotatedResource.setAttribute("role",
				computeRole(resource2attributes, annotatedResource.getValue()));
	}

}
