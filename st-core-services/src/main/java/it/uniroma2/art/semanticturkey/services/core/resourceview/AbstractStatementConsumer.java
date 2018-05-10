package it.uniroma2.art.semanticturkey.services.core.resourceview;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.datarange.DataRangeAbstract;
import it.uniroma2.art.semanticturkey.datarange.DataRangeDataOneOf;
import it.uniroma2.art.semanticturkey.datarange.ParseDataRange;
import it.uniroma2.art.semanticturkey.exceptions.NotClassAxiomException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;
import it.uniroma2.art.semanticturkey.utilities.TurtleHelp;

public abstract class AbstractStatementConsumer implements StatementConsumer {

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

	public static String computeShow(Resource resource, Map<Resource, Map<String, Value>> resource2attributes,
			Model statements) {
		Map<String, String> namespaceToprefixMap = statements.getNamespaces().stream()
				.collect(toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
		if (resource instanceof BNode) {
			if (statements.contains(resource, RDF.TYPE, RDFS.DATATYPE)) {
				DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration((BNode) resource,
						statements);
				if (dataRangeAbstract instanceof DataRangeDataOneOf) {
					return ((DataRangeDataOneOf) dataRangeAbstract).getLiteralList().stream()
							.map(lit -> TurtleHelp.serializeLiteral(lit, namespaceToprefixMap))
							.collect(Collectors.joining(", ", "{", "}"));
				}
			} else if (statements.contains(resource, RDF.TYPE, RDFS.CLASS)
					|| statements.contains(resource, RDF.TYPE, OWL.CLASS)
					|| statements.contains(resource, RDF.TYPE, OWL.RESTRICTION)) {
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
				return expr;
			} else if (statements.contains(resource, RDF.TYPE, RDF.LIST)) {
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

		Map<String, String> ns2prefixMap = statements.getNamespaces().stream()
				.collect(toMap(Namespace::getName, Namespace::getPrefix, (v1, v2) -> v1 != null ? v1 : v2));
		return computeDefaultShow(resource, ns2prefixMap);

	}

	public static void addShowViaDedicatedOrGenericRendering(
			AnnotatedValue<? extends Resource> annotatedResource,
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, IRI predicate, Model statements) {
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
										Literal creShowAsLiteral = (Literal) creShow;
										annotatedResource.setAttribute("show", creShowAsLiteral.getLabel());
										creShowAsLiteral.getLanguage()
												.ifPresent(v -> annotatedResource.setAttribute("lang", v));
										return;
									}
								}
							}
						}
					}
				}
			}
		}
		annotatedResource.setAttribute("show", computeShow(resource, resource2attributes, statements));
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
