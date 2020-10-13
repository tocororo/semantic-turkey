package it.uniroma2.art.semanticturkey.services.core.resourceview;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.NotClassAxiomException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;

public abstract class AbstractStatementConsumer implements StatementConsumer {

	public static enum ShowInterpretation {
		id, // identifier (i.e. qname, Bnode or IRI)
		ope, // object property expression
		descr, // description
		list, // a list (non necessarly a manchester expression, since the elements may be rendered)
		rendering_or_id // output of the rendering engine falling back to id
	}

	public static final String UNKNOWN_OWL_AXIOM = "unknown OWL axiom";

	private static final Literal DEFAULT_ROLE = SimpleValueFactory.getInstance()
			.createLiteral(RDFResourceRole.undetermined.toString());

	public static Literal computeRole(Map<Resource, Map<String, Value>> resource2attributes,
			Resource resource) {
		return (Literal) resource2attributes.getOrDefault(resource, Collections.emptyMap())
				.getOrDefault("role", DEFAULT_ROLE);
	}

	public static String computeDefaultShow(Resource resource) {
		return computeDefaultShow(resource, Collections.emptyMap());
	}

	public static String computeDefaultShow(Resource resource, Map<String, String> ns2prefixMapping) {
		if (resource instanceof BNode) {
			return "_:" + resource.stringValue();
		} else {
			String prefix = ns2prefixMapping.get(((IRI) resource).getNamespace());

			if (prefix == null) {
				return resource.stringValue();
			} else {
				return prefix + ":" + ((IRI) resource).getLocalName();
			}
		}
	}

	public static String computeGraphs(Set<Resource> graphs) {
		return graphs.stream().map(g -> g == null ? "MAINGRAPH" : g.toString()).collect(joining(","));
	}

	public static TripleScopes computeTripleScope(Set<Resource> graphs, Resource workingGraph) {
		return NatureRecognitionOrchestrator
				.computeTripleScopeFromGraphs(Sets.filter(graphs, Objects::nonNull), workingGraph);
	}

	/**
	 * Returns the value of the show and, optionally, an indication of the meaning of the show (e.g.
	 * Manchester class expression).
	 * 
	 * @param resource
	 * @param resource2attributes
	 * @param statements
	 * @param useRenderingEngine
	 * @return
	 */
	public static Pair<String, ShowInterpretation> computeShow(Resource resource,
			Map<Resource, Map<String, Value>> resource2attributes, Model statements,
			boolean useRenderingEngine) {
		Map<String, String> namespaceToprefixMap = statements.getNamespaces().stream()
				.collect(toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
		if (resource instanceof BNode) {
			if (statements.contains(resource, RDF.TYPE, RDFS.CLASS)
					|| statements.contains(resource, RDF.TYPE, OWL.CLASS)
					|| statements.contains(resource, RDF.TYPE, OWL.RESTRICTION)
					|| statements.contains(resource, RDF.TYPE, RDFS.DATATYPE)) {
				String expr;
				try {
					expr = ManchesterSyntaxUtils.getManchExprFromBNode((BNode) resource, namespaceToprefixMap,
							true, new Resource[0], null, true, statements);
					if (expr != null && !expr.isEmpty()) {
						if (expr.startsWith("(") && expr.endsWith("")) {
							// remove the starting '(' and the end ')'
							expr = expr.substring(1, expr.length() - 1).trim();
						}
					}
				} catch (NotClassAxiomException e) {
					// there was a problem parsing the expression, so report it
					expr = UNKNOWN_OWL_AXIOM;
				}
				return ImmutablePair.of(expr, ShowInterpretation.descr);
			} else if (statements.contains(resource, RDF.TYPE, RDF.LIST)
					|| statements.contains(resource, RDF.FIRST, null)
					|| statements.contains(resource, RDF.REST, null)) {
				ArrayList<Value> values = RDFCollections.asValues(statements, resource, new ArrayList<>());

				String listExpression = values.stream().map(v -> {
					if (v instanceof Resource) {
						return computeShow((Resource) v, resource2attributes, statements, useRenderingEngine)
								.getLeft();
					} else {
						return NTriplesUtil.toNTriplesString(v);
					}
				}).collect(joining(", ", "[", "]"));
				return ImmutablePair.of(listExpression, ShowInterpretation.list);
			} else if (statements.contains(resource, OWL.INVERSEOF, null)) {
				Resource inverseProp = Models.getPropertyResource(statements, resource, OWL.INVERSEOF)
						.orElse(null);

				// returns an object property expression. Rendering is disabled as in case of class
				// expressions
				String objectPropertyExpression = "INVERSE "
						+ computeShow(inverseProp, resource2attributes, statements, false).getLeft();
				return ImmutablePair.of(objectPropertyExpression, ShowInterpretation.ope);
			}
		} else if (useRenderingEngine) {
			Map<String, Value> resourceAttributes = resource2attributes.get(resource);

			if (resourceAttributes != null) {
				Value show = resourceAttributes.get("show");
				if (show != null) {
					return ImmutablePair.of(show.stringValue(), ShowInterpretation.rendering_or_id);
				}
			}
		}

		Map<String, String> ns2prefixMap = statements.getNamespaces().stream()
				.collect(toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
		return ImmutablePair.of(computeDefaultShow(resource, ns2prefixMap), ShowInterpretation.id);

	}

	public static void addShowViaDedicatedOrGenericRendering(
			AnnotatedValue<? extends Resource> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, IRI predicate, Model statements,
			boolean useRenderingEngine) {
		Resource resource = annotatedResource.getValue();

		Map<String, Value> resourceAttributes = resource2attributes.get(resource);

		if (resourceAttributes != null) {
			Value lexicalEntryRendering = resourceAttributes.get("ontolexLexicalEntryRendering");

			if (lexicalEntryRendering != null) {
				annotatedResource.setAttribute("show", lexicalEntryRendering.stringValue());
				return;
			} else {
				Value lexiconRendering = resourceAttributes.get("limeLexiconRendering");
				if (lexiconRendering != null) {
					annotatedResource.setAttribute("show", lexiconRendering.stringValue());
					return;
				} else {
					Value formRendering = resourceAttributes.get("ontolexFormRendering");
					if (formRendering != null) {
						annotatedResource.setAttribute("show", formRendering.stringValue());
						return;
					} else {
						Value literalForm = resourceAttributes.get("literalForm");
						if (literalForm != null) {
							Literal literalFormAsLiteral = (Literal) literalForm;
							annotatedResource.setAttribute("show", literalFormAsLiteral.getLabel());
							literalFormAsLiteral.getLanguage()
									.ifPresent(v -> annotatedResource.setAttribute("lang", v));
							return;
						} else {
							Value componentRendering = resourceAttributes.get("decompComponentRendering");
							if (componentRendering != null) {
								annotatedResource.setAttribute("show", componentRendering.stringValue());
								return;
							} else {
								if (predicate != null) {
									Map<Resource, Literal> resource2CreShow = predicate2resourceCreShow
											.getOrDefault(predicate, Collections.emptyMap());
									Literal creShow = resource2CreShow.get(resource);

									if (creShow != null) {
										annotatedResource.setAttribute("show", creShow.getLabel());
										creShow.getLanguage()
												.ifPresent(v -> annotatedResource.setAttribute("lang", v));
										IRI dt = creShow.getDatatype();
										if (!Objects.equals(dt, RDF.LANGSTRING)) {
											annotatedResource.setAttribute("dataType", dt);
										}
										return;
									}
								}
							}
						}
					}
				}
			}
		}
		Pair<String, ShowInterpretation> computedShow = computeShow(resource, resource2attributes, statements,
				useRenderingEngine);
		annotatedResource.setAttribute("show", computedShow.getLeft());
		if (resource instanceof BNode) {
			annotatedResource.setAttribute("show_interpretation", computedShow.getRight().toString());
		}
	}

	public static <T extends Resource> void addRole(AnnotatedValue<T> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes) {
		annotatedResource.setAttribute("role",
				computeRole(resource2attributes, annotatedResource.getValue()));
	}

	public static <T extends Resource> void addNature(AnnotatedValue<T> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes) {
		Map<String, Value> attr2values = resource2attributes.get(annotatedResource.getValue());

		if (attr2values != null) {
			Value nature = attr2values.get("nature");
			if (nature != null) {
				annotatedResource.setAttribute("nature", nature);
			}

		}
	}

	public static <T extends Resource> void addQName(AnnotatedValue<T> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes) {
		Map<String, Value> attr2values = resource2attributes.get(annotatedResource.getValue());

		if (attr2values != null) {
			Value qname = attr2values.get("qname");
			if (qname != null) {
				annotatedResource.setAttribute("qname", qname);
			}

		}
	}

}
