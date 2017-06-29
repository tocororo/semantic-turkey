/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2014.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */
package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.StatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.StatementConsumerProvider;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.services.support.QueryResultsProcessors;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * This service produces a view showing the details of a resource. This service operates uniformly (as much as
 * possible) both on local resources and remote ones.
 * 
 */
@STService
public class ResourceView extends STServiceAdapter {

	public static final IRI INFERENCE_GRAPH = SimpleValueFactory.getInstance()
			.createIRI("http://semanticturkey/inference-graph");

	private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);

	@Autowired
	private ResourceLocator resourceLocator;

	@Autowired
	private StatementConsumerProvider statementConsumerProvider;

	@Autowired
	private CustomFormManager customFormManager;

	private ThreadLocal<Map<Project, RepositoryConnection>> projectConnectionHolder = ThreadLocal
			.withInitial(HashMap::new);

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ')', 'R')")
	public Map<String, ResourceViewSection> getResourceView(Resource resource,
			@Optional ResourcePosition resourcePosition) throws Exception {
		try {
			Project project = getProject();
			Resource workingGraph = getWorkingGraph();

			SimpleValueFactory vf = SimpleValueFactory.getInstance();

			if (resourcePosition == null) {
				resourcePosition = resourceLocator.locateResource(getProject(), getRepository(), resource);
			}

			Model retrievedStatements = retrieveStatements(resourcePosition, resource);

			// A resource is editable iff it is a locally defined resource (i.e. it is the subject of at least
			// one triple in the working graph)
			boolean subjectResourceEditable = (resourcePosition instanceof LocalResourcePosition)
					&& retrievedStatements.contains(resource, null, null, workingGraph);

			QueryResults.stream(getManagedConnection().getNamespaces()).forEach(ns -> {
				retrievedStatements.setNamespace(ns.getPrefix(), ns.getName());
			});

			Set<IRI> resourcePredicates = retrievedStatements.filter(resource, null, null).predicates();

			SubjectAndObjectsInfos subjectAndObjectsAddtionalInfo = retrieveSubjectAndObjectsAddtionalInformation(
					resourcePosition, resource, resourcePredicates);

			Map<Resource, Map<String, Value>> resource2attributes = subjectAndObjectsAddtionalInfo.resource2attributes;
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow = subjectAndObjectsAddtionalInfo.predicate2resourceCreShow;

			Set<Statement> processedStatements = new HashSet<>();

			Map<String, ResourceViewSection> description = new LinkedHashMap<>();

			AnnotatedValue<Resource> annotatedResource = new AnnotatedValue<Resource>(resource);
			annotatedResource.setAttribute("resourcePosition", resourcePosition.toString());
			annotatedResource.setAttribute("explicit", subjectResourceEditable);

			AbstractStatementConsumer.addNature(annotatedResource, resource2attributes);

			RDFResourceRole resourceRole = getRoleFromNature(
					java.util.Optional.ofNullable(annotatedResource.getAttributes().get("nature"))
							.map(Value::stringValue).orElse(""));

			AbstractStatementConsumer.addShowOrRenderXLabelOrCRE(annotatedResource, resource2attributes,
					predicate2resourceCreShow, null, retrievedStatements);

			description.put("resource", new ResourceSection(annotatedResource));

			List<StatementConsumer> viewTemplate = statementConsumerProvider
					.getTemplateForResourceRole(resourceRole);

			Set<IRI> specialProperties = viewTemplate.stream().flatMap(c -> c.getMatchedProperties().stream())
					.collect(toSet());

			// Always consider special predicates, even if they are not mentioned, because it may be the case
			// that they are shown anyway in the resource view
			Set<IRI> predicatesToEnrich = Sets.union(resourcePredicates, specialProperties);
			Model propertyModel = retrievePredicateInformation(resourcePosition, predicatesToEnrich,
					specialProperties, resource2attributes, retrievedStatements);

			for (StatementConsumer aConsumer : viewTemplate) {
				Map<String, ResourceViewSection> producedSections = aConsumer.consumeStatements(project,
						resourcePosition, resource, retrievedStatements, processedStatements, workingGraph,
						resource2attributes, predicate2resourceCreShow, propertyModel);
				description.putAll(producedSections);
			}

			return description;
		} finally {
			for (RepositoryConnection otherConn : projectConnectionHolder.get().values()) {
				try {
					otherConn.close();
				} catch (RepositoryException e) {
					logger.debug("Exception closing additional project", e);
				}
			}
			projectConnectionHolder.remove();
		}
	}

	@STServiceOperation
	@Read
	public List<AnnotatedValue<IRI>> getLexicalizationProperties(@Optional Resource resource,
			@Optional ResourcePosition resourcePosition)
			throws ProjectAccessException, ProjectInconsistentException {
		if (resourcePosition == null) {
			resourcePosition = resource != null
					? resourceLocator.locateResource(getProject(), getRepository(), resource)
					: ResourceLocator.UNKNOWN_RESOURCE_POSITION;
		}

		Map<String, String> ns2prefixMap = QueryResults.stream(getManagedConnection().getNamespaces())
				.collect(toMap(Namespace::getName, Namespace::getPrefix, (x, y) -> x));

		ValueFactory vf = SimpleValueFactory.getInstance();

		List<AnnotatedValue<IRI>> lexicalizationProperties = new ArrayList<>();

		HashSet<IRI> annotationProps = Sets.newHashSet(RDFS.LABEL, SKOS.PREF_LABEL, SKOS.ALT_LABEL,
				SKOS.HIDDEN_LABEL);
		HashSet<IRI> objectProps = Sets.newHashSet(SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL);

		for (IRI pred : getLexicalizationPropertiesHelper(resource, resourcePosition)) {
			AnnotatedValue<IRI> annotatedPred = new AnnotatedValue<IRI>(pred);
			Map<String, Value> predAttrs = annotatedPred.getAttributes();

			String prefix = ns2prefixMap.get(pred.getNamespace());

			String show = pred.stringValue();
			if (prefix != null) {
				show = prefix + ":" + pred.getLocalName();
			}
			predAttrs.put("show", vf.createLiteral(show));

			RDFResourceRole role = RDFResourceRole.property;

			if (annotationProps.contains(pred)) {
				role = RDFResourceRole.annotationProperty;
			} else if (objectProps.contains(pred)) {
				role = RDFResourceRole.objectProperty;
			}

			predAttrs.put("role", vf.createLiteral(role.toString()));

			lexicalizationProperties.add(annotatedPred);
		}

		return lexicalizationProperties;
	}

	// TODO place this method into a better place
	public static List<IRI> getLexicalizationPropertiesHelper(Resource resource,
			ResourcePosition resourcePosition) throws ProjectInconsistentException {
		if (resourcePosition instanceof LocalResourcePosition) {
			Project hostingProject = ((LocalResourcePosition) resourcePosition).getProject();
			if (hostingProject.getLexicalizationModel().equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
				return Arrays.asList(SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL);
			} else if (hostingProject.getLexicalizationModel().equals(Project.SKOS_LEXICALIZATION_MODEL)) {
				return Arrays.asList(SKOS.PREF_LABEL, SKOS.ALT_LABEL, SKOS.HIDDEN_LABEL);
			} else {
				return Arrays.asList(RDFS.LABEL);
			}
		}

		return Arrays.asList(RDFS.LABEL, SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL,
				SKOS.PREF_LABEL, SKOS.ALT_LABEL, SKOS.HIDDEN_LABEL);
	}

	private Model retrievePredicateInformation(ResourcePosition resourcePosition, Set<IRI> resourcePredicates,
			Set<IRI> specialProperties, Map<Resource, Map<String, Value>> resource2attributes,
			Model statements) throws ProjectAccessException {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		String predicatesValuesFrag = Sets.union(resourcePredicates, specialProperties).stream()
				.map(p -> "(" + NTriplesUtil.toNTriplesString(p) + ")").collect(joining(" "));
		String specialPredicatesValuesFrag = specialProperties.stream()
				.map(p -> "(" + NTriplesUtil.toNTriplesString(p) + ")").collect(joining(" "));

		if (resourcePosition instanceof LocalResourcePosition) {
			RepositoryConnection acquireManagedConnection = acquireManagedConnectionToProject(getProject(),
					((LocalResourcePosition) resourcePosition).getProject());

			QueryBuilder qb = createQueryBuilder(String.format(
				// @formatter:off  
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                             \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                         \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                              \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>                                      \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                               \n" +
					" 	SELECT ?resource (GROUP_CONCAT(DISTINCT $parentProp; separator = \"><\") AS ?attr_parents)" + generateNatureSPARQLSelectPart() + " WHERE {     \n" +
					" 	  VALUES(?resource){                                                          \n" +
					" 	    %s						                                                  \n" +
					" 	  }                                                                           \n" +
					" 	  VALUES(?specialProp){                                                       \n" +
					" 	    %s						                                                  \n" +
					" 	  }                                                                           \n" +
					" 	  OPTIONAL {                                                                  \n" +
					" 	    ?resource rdfs:subPropertyOf* ?specialProp                                \n" +
					" 	    BIND(STR(?specialProp) as $parentProp)                                    \n" +
					" 	  }	                                                                          \n" +
					generateNatureSPARQLWherePart("resource") +
					" 	}                                                                             \n" +
					" 	GROUP BY ?resource                                                            \n"
				// @formatter:on
					, predicatesValuesFrag, specialPredicatesValuesFrag));
			qb.processRole();
			Model propertyModel = new LinkedHashModel();
			qb.runQuery(acquireManagedConnection).stream().forEach(annotatedPredicate -> {
				List<IRI> parents = Arrays
						.stream(annotatedPredicate.getAttributes().get("parents").stringValue().split("><"))
						.filter(s -> !s.isEmpty()).map(s -> vf.createIRI(s)).collect(toList());
				IRI predicate = (IRI) annotatedPredicate.getValue();
				parents.forEach(parent -> {
					propertyModel.add(predicate, RDFS.SUBPROPERTYOF, parent);
				});

				Map<String, Value> attrs = resource2attributes.get(predicate);
				if (attrs == null) {
					attrs = new HashMap<>();
					resource2attributes.put(predicate, attrs);
				}
				attrs.putAll(annotatedPredicate.getAttributes());
			});

			Map<String, String> ns2prefixMap = new HashMap<>();
			for (Namespace ns : statements.getNamespaces()) {
				ns2prefixMap.put(ns.getName(), ns.getPrefix());
			}
			for (IRI predicate : resourcePredicates) {
				Map<String, Value> attrs = resource2attributes.computeIfAbsent(predicate,
						k -> new HashMap<>());

				String prefix = ns2prefixMap.get(predicate.getNamespace());
				if (prefix == null) {
					attrs.put("show", vf.createLiteral(predicate.stringValue()));
				} else {
					attrs.put("show", vf.createLiteral(prefix + ":" + predicate.getLocalName()));
				}
			}

			return propertyModel;
		}

		Model propertiesModel = new LinkedHashModel();
		specialProperties.stream().forEach(p -> propertiesModel.add(p, RDFS.SUBPROPERTYOF, p));
		return propertiesModel;
	}

	private SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformation(
			ResourcePosition resourcePosition, Resource resource, Set<IRI> resourcePredicates)
			throws ProjectAccessException {
		if (resourcePosition instanceof LocalResourcePosition) {
			LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;
			StringBuilder sb = new StringBuilder();
			sb.append(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                         \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                              \n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>                                      \n" +
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                               \n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                               \n" +
				" SELECT ?resource ?predicate (MAX(?attr_creShowTemp) as ?predattr_creShow) " + generateNatureSPARQLSelectPart() + " WHERE { \n" +
				"   {                                                                               \n" +
				"     ?subjectResource ?predicate ?tempResource .                                   \n" +
				"     ?tempResource (rdf:rest*/rdf:first)* ?resource                                \n" +
				"   } UNION {                                                                       \n" +
				"     bind(?subjectResource as ?resource)                                           \n" +
				"   }                                                                               \n" +
				"   FILTER(!isLITERAL(?resource))                                                   \n" +
				generateNatureSPARQLWherePart("resource")
				// @formatter:on
			);

			Multimap<List<IRI>, IRI> chain2pred = HashMultimap.create();

			for (IRI pred : resourcePredicates) {
				customFormManager.getCustomForms(getProject(), pred).stream()
						.filter(CustomFormGraph.class::isInstance).map(CustomFormGraph.class::cast)
						.forEach(cf -> {
							List<IRI> chain = cf.getShowPropertyChain();

							if (chain == null || chain.isEmpty())
								return;

							chain2pred.put(chain, pred);
						});
			}

			int i = 0;

			for (List<IRI> chain : chain2pred.keySet()) {
				String selectorCollection = chain2pred.get(chain).stream().map(RenderUtils::toSPARQL)
						.collect(joining(",", "(", ")"));

				String showChain = chain.stream().map(RenderUtils::toSPARQL).collect(joining("/"));

				sb.append(
					// @formatter:off
					"     OPTIONAL {                                                          \n" +
					"        ?subjectResource ?predicate ?resource .                          \n" +
					"        FILTER(?predicate IN " + selectorCollection + ")                 \n" + 	
					"        ?resource " +  showChain + " ?attr_creShowTemp" + (i++) + " .    \n" +
					"     }                                                                   \n"
					// @formatter:on
				);
			}

			if (i != 0) {
				sb.append("     BIND(COALESCE(\n");

				for (int j = 0; j < i; j++) {
					sb.append("       ?attr_creShowTemp" + j);
					if (j + 1 != i) {
						sb.append(", ");
					}
					sb.append("\n");
				}

				sb.append("     ) AS ?attr_creShowTemp) \n");
			}

			sb.append(
				// @formatter:off
				" }                                                                           \n" +
				" GROUP BY ?resource ?predicate                                               \n"
				// @formatter:on
			);

			QueryBuilder qb = createQueryBuilder(sb.toString());
			qb.processRendering();
			qb.process(XLabelLiteralFormQueryProcessor.INSTANCE, "resource", "attr_literalForm");
			qb.setBinding("subjectResource", resource);
			qb.setIncludeInferred(true);

			Collection<BindingSet> bindingSets = qb.runQuery(
					acquireManagedConnectionToProject(getProject(), localResourcePosition.getProject()),
					QueryResultsProcessors.toBindingSets());

			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow = new HashMap<>();
			Map<Resource, Map<String, Value>> resource2attributes = new HashMap<>();

			for (BindingSet bs : bindingSets) {
				IRI predicate = (IRI) bs.getValue("predicate");

				Map<Resource, Literal> resource2CreShow = predicate2resourceCreShow.computeIfAbsent(predicate,
						key -> new HashMap<>());

				Value resourceValue = bs.getValue("resource");

				if (!(resourceValue instanceof Resource))
					continue;

				Resource resourceResource = (Resource) resourceValue;

				Map<String, Value> attributes = resource2attributes.computeIfAbsent(resourceResource,
						key -> new HashMap<>());

				for (Binding b : bs) {
					String bindingName = b.getName();
					Value bindingValue = b.getValue();

					if (bindingValue == null)
						continue;

					if (bindingName.startsWith("attr_")) {
						attributes.put(bindingName.substring("attr_".length()), bindingValue);
					}

					if (bindingName.equals("predattr_creShow")) {
						Literal bindingValueAsLiteral = (Literal) bindingValue;

						if (bindingValueAsLiteral.getLabel().isEmpty())
							continue;

						resource2CreShow.put(resourceResource, bindingValueAsLiteral);
					}
				}

			}

			return new SubjectAndObjectsInfos(resource2attributes, predicate2resourceCreShow);
		}

		return new SubjectAndObjectsInfos(Collections.emptyMap(), Collections.emptyMap());
	}

	private static class SubjectAndObjectsInfos {

		public SubjectAndObjectsInfos(Map<Resource, Map<String, Value>> resource2attributes,
				Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow) {
			this.resource2attributes = resource2attributes;
			this.predicate2resourceCreShow = predicate2resourceCreShow;
		}

		public final Map<Resource, Map<String, Value>> resource2attributes;
		public final Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow;
	}

	private Model retrieveStatements(ResourcePosition resourcePosition, Resource resource)
			throws ProjectAccessException, RDF4JException, IOException {
		if (resourcePosition instanceof LocalResourcePosition) {
			LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;

			Project resourceHoldingProject = localResourcePosition.getProject();

			RepositoryConnection managedConnection = acquireManagedConnectionToProject(getProject(),
					resourceHoldingProject);

			Model retrievedStatements = new LinkedHashModel();

			TupleQuery tupleQuery = managedConnection.prepareTupleQuery(
					// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>              \n" +
					" SELECT ?g ?s ?p ?o ?g2 ?s2 ?p2 ?o2{                                    \n" +
					"     GRAPH ?g {                                                         \n" +
					"       ?s ?p ?o .                                                       \n" +
					"     }                                                                  \n" +
					"     OPTIONAL {                                                         \n" +
					"       ?o rdf:rest* ?s2                                                 \n" +
					"       FILTER(isBLANK(?o))                                              \n" +
					"       GRAPH ?g2 {                                                      \n" +
					"         ?s2 ?p2 ?o2 .                                                  \n" +
					"       }                                                                \n" +
					"     }                                                                  \n" +
					" }	                                                                     \n"
					// @formatter:on
			);
			tupleQuery.setBinding("s", resource);
			tupleQuery.setIncludeInferred(false);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					Resource g = (Resource) bindingSet.getValue("g");
					Resource s = (Resource) bindingSet.getValue("s");
					IRI p = (IRI) bindingSet.getValue("p");
					Value o = bindingSet.getValue("o");
					retrievedStatements.add(s, p, o, g);

					if (bindingSet.hasBinding("g2")) {
						Resource g2 = (Resource) bindingSet.getValue("g2");
						Resource s2 = (Resource) bindingSet.getValue("s2");
						IRI p2 = (IRI) bindingSet.getValue("p2");
						Value o2 = bindingSet.getValue("o2");

						retrievedStatements.add(s2, p2, o2, g2);
					}
				}
			}

			GraphQuery describeQuery = managedConnection
					.prepareGraphQuery("DESCRIBE ?x WHERE {BIND(?y as ?x)}");
			describeQuery.setBinding("y", resource);
			describeQuery.setIncludeInferred(true);
			QueryResults.stream(describeQuery.evaluate()).forEach(stmt -> {
				Resource subject = stmt.getSubject();
				IRI predicate = stmt.getPredicate();
				Value object = stmt.getObject();
				if (retrievedStatements.contains(subject, predicate, object))
					return;

				retrievedStatements.add(subject, predicate, object, INFERENCE_GRAPH);
			});

			return retrievedStatements;
		}

		if (!(resource instanceof IRI)) {
			throw new IllegalArgumentException(
					"Could not retrieve the description of a blank node from a remote dataset");
		}

		if (resourcePosition instanceof RemoteResourcePosition) {
			Model retrievedStatements = new LinkedHashModel();

			RemoteResourcePosition remotePosition = (RemoteResourcePosition) resourcePosition;
			String sparqlEndpoint = remotePosition.getDatasetMetadata().getSparqlEndpoint();

			if (sparqlEndpoint != null) {
				Repository sparqlRepository = new SPARQLRepository(sparqlEndpoint);
				sparqlRepository.initialize();
				try {
					try (RepositoryConnection conn = sparqlRepository.getConnection()) {
						TupleQuery tupleQuery = conn.prepareTupleQuery(
							// @formatter:off
							" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>              \n" +
							" SELECT ?g ?s ?p ?o ?g2 ?s2 ?p2 ?o2{                                    \n" +
							"     ?s ?p ?o .                                                         \n" +
							"     OPTIONAL {                                                         \n" +
							"     	GRAPH ?g {                                                       \n" +
							"     	  ?s ?p ?o .                                                     \n" +
							"    	 }                                                               \n" +
							"     }                                                                  \n" +
//							"     OPTIONAL {                                                         \n" +
//							"       ?o rdf:rest* ?s2                                                 \n" +
//							"       FILTER(isBLANK(?o))                                              \n" +
//							"       GRAPH ?g2 {                                                      \n" +
//							"         ?s2 ?p2 ?o2 .                                                  \n" +
//							"       }                                                                \n" +
//							"     }                                                                  \n" +
							" }	                                                                     \n"
							// @formatter:on
						);
						tupleQuery.setBinding("s", resource);
						try (TupleQueryResult result = tupleQuery.evaluate()) {
							while (result.hasNext()) {
								BindingSet bindingSet = result.next();
								Resource g = (Resource) bindingSet.getValue("g");
								Resource s = (Resource) bindingSet.getValue("s");
								IRI p = (IRI) bindingSet.getValue("p");
								Value o = bindingSet.getValue("o");
								if (g != null) {
									retrievedStatements.add(s, p, o, g);
								} else {
									retrievedStatements.add(s, p, o);
								}

								if (bindingSet.hasBinding("g2")) {
									Resource g2 = (Resource) bindingSet.getValue("g2");
									Resource s2 = (Resource) bindingSet.getValue("s2");
									IRI p2 = (IRI) bindingSet.getValue("p2");
									Value o2 = bindingSet.getValue("o2");

									retrievedStatements.add(s2, p2, o2, g2);
								}
							}
						}

						GraphQuery describeQuery = conn
								.prepareGraphQuery("DESCRIBE ?x WHERE {BIND(?y as ?x)}");
						describeQuery.setBinding("y", resource);
						QueryResults.stream(describeQuery.evaluate()).forEach(stmt -> {
							Resource subject = stmt.getSubject();
							IRI predicate = stmt.getPredicate();
							Value object = stmt.getObject();
							if (retrievedStatements.contains(subject, predicate, object))
								return;

							retrievedStatements.add(subject, predicate, object);
						});
					}
				} finally {
					sparqlRepository.shutDown();
				}
			} else if (!remotePosition.getDatasetMetadata().isDereferenceable()) {
				throw new IllegalArgumentException(
						"Could not dereference dataset:" + remotePosition.getDatasetMetadata().getBaseURI());
			}
		}

		Model retrievedStatements = new LinkedHashModel();
		RDFLoader rdfLoader = new RDFLoader(new ParserConfig(), SimpleValueFactory.getInstance());
		StatementCollector statementCollector = new StatementCollector(retrievedStatements);
		rdfLoader.load(new URL(resource.stringValue()), null, null, statementCollector);
		return retrievedStatements;
	}

	private RepositoryConnection acquireManagedConnectionToProject(ProjectConsumer consumer,
			Project resourceHoldingProject) throws ProjectAccessException {
		if (consumer.equals(resourceHoldingProject)) {
			return getManagedConnection();
		} else {
			AccessResponse accessResponse = ProjectManager.checkAccessibility(consumer,
					resourceHoldingProject, AccessLevel.R, LockLevel.NO);

			if (!accessResponse.isAffirmative()) {
				throw new ProjectAccessException(accessResponse.getMsg());
			}

			return projectConnectionHolder.get().computeIfAbsent(resourceHoldingProject,
					p -> RDF4JRepositoryUtils.wrapReadOnlyConnection(p.getRepository().getConnection()));
		}
	}
}

class XLabelLiteralFormQueryProcessor implements QueryBuilderProcessor {

	public static final QueryBuilderProcessor INSTANCE = new XLabelLiteralFormQueryProcessor();

	@Override
	public GraphPattern getGraphPattern(Project currentProject) {
		return GraphPatternBuilder.create().prefix("skosxl", SKOSXL.NAMESPACE)
				.pattern("?resource skosxl:literalForm ?literalForm . FILTER(isLITERAL(?literalForm))")
				.projection(ProjectionElementBuilder.variable("literalForm")).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public Map<Value, Literal> processBindings(Project currentProject, List<BindingSet> resultTable) {
		return resultTable.stream().filter(bs -> bs.getValue("literalForm") != null).collect(
				groupingBy(bs -> bs.getValue("resource"), mapping(bs -> (Literal) bs.getValue("literalForm"),
						reducing(null, (v1, v2) -> v1 != null ? v1 : v2))));
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

}