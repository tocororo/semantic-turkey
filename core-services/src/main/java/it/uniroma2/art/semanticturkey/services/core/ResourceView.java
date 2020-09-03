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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.FormRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexiconRenderer;
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
import it.uniroma2.art.semanticturkey.utilities.ErrorRecoveringValueFactory;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;
import org.apache.commons.lang3.mutable.MutableLong;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
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
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Nullable;
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

import static java.util.stream.Collectors.*;

/**
 * This service produces a view showing the details of a resource. This service operates uniformly (as much as
 * possible) both on local resources and remote ones.
 * 
 */
@STService
public class ResourceView extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);

	@Autowired
	private ResourceLocator resourceLocator;

	@Autowired
	private StatementConsumerProvider statementConsumerProvider;

	@Autowired
	private CustomFormManager customFormManager;

	@Autowired
	private MetadataRegistryBackend metadataRegistryBackend;

	@Autowired
	private MediationFramework mediationFramework;

	private ThreadLocal<Map<Project, RepositoryConnection>> projectConnectionHolder = ThreadLocal
			.withInitial(HashMap::new);

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ')', 'R')")
	public Map<String, ResourceViewSection> getResourceView(Resource resource,
			@Optional ResourcePosition resourcePosition,
			@Optional(defaultValue = "false") boolean includeInferred,
			@Optional(defaultValue = "false") boolean ignorePropertyExclusions) throws Exception {
		try {
			Project project = getProject();
			Resource workingGraph = getWorkingGraph();

			if (resourcePosition == null) {
				resourcePosition = resourceLocator.locateResource(getProject(), getRepository(), resource);
			}

			MutableLong excludedObjectsCount = new MutableLong();

			AccessMethod accessMethod = computeAccessMethod(resourcePosition);

			Model retrievedStatements = accessMethod.retrieveStatements(resourcePosition, resource,
					includeInferred, ignorePropertyExclusions, excludedObjectsCount);

			// A resource is editable iff it is a locally defined resource (i.e. it is the subject of at least
			// one triple in the working graph)
			boolean subjectResourceEditable = (resourcePosition instanceof LocalResourcePosition)
					&& retrievedStatements.contains(resource, null, null, workingGraph);

			QueryResults.stream(getManagedConnection().getNamespaces()).forEach(ns -> {
				retrievedStatements.setNamespace(ns.getPrefix(), ns.getName());
			});

			Set<IRI> resourcePredicates = retrievedStatements.filter(resource, null, null).predicates();

			SubjectAndObjectsInfos subjectAndObjectsAddtionalInfo = accessMethod
					.retrieveSubjectAndObjectsAddtionalInformation(resourcePosition, resource,
							includeInferred, retrievedStatements, resourcePredicates,
							ignorePropertyExclusions);

			Map<Resource, Map<String, Value>> resource2attributes = subjectAndObjectsAddtionalInfo.resource2attributes;
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow = subjectAndObjectsAddtionalInfo.predicate2resourceCreShow;

			Set<Statement> processedStatements = new HashSet<>();

			Map<String, ResourceViewSection> description = new LinkedHashMap<>();
			AnnotatedValue<Resource> annotatedResource = new AnnotatedValue<>(resource);
			annotatedResource.setAttribute("resourcePosition", resourcePosition.toString());
			annotatedResource.setAttribute("explicit", subjectResourceEditable);
			annotatedResource.setAttribute("excludedObjectsCount", excludedObjectsCount.longValue());
			annotatedResource.setAttribute("accessMethod", accessMethod.getName());
			AbstractStatementConsumer.addNature(annotatedResource, resource2attributes);

			RDFResourceRole resourceRole = getRoleFromNature(
					java.util.Optional.ofNullable(annotatedResource.getAttributes().get("nature"))
							.map(Value::stringValue).orElse(""));

			AbstractStatementConsumer.addShowViaDedicatedOrGenericRendering(annotatedResource,
					resource2attributes, predicate2resourceCreShow, null, retrievedStatements, true);
			AbstractStatementConsumer.addQName(annotatedResource, resource2attributes);
			description.put("resource", new ResourceSection(annotatedResource));

			List<StatementConsumer> viewTemplate = statementConsumerProvider
					.getTemplateForResourceRole(resourceRole);

			Set<IRI> specialProperties = viewTemplate.stream().flatMap(c -> c.getMatchedProperties().stream())
					.collect(toSet());

			// Always consider special predicates, even if they are not mentioned, because it may be the case
			// that they are shown anyway in the resource view
			Set<IRI> predicatesToEnrich = Sets.union(resourcePredicates, specialProperties);
			Model propertyModel = accessMethod.retrievePredicateInformation(resourcePosition,
					predicatesToEnrich, specialProperties, resource2attributes, retrievedStatements);

			for (StatementConsumer aConsumer : viewTemplate) {
				Map<String, ResourceViewSection> producedSections = aConsumer.consumeStatements(project,
						conditionalAcquireConnectionToProject(project, resourcePosition), resourcePosition,
						resource, retrievedStatements, processedStatements, workingGraph, resource2attributes,
						predicate2resourceCreShow, propertyModel);
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

	protected AccessMethod computeAccessMethod(ResourcePosition resourcePosition) {
		if (resourcePosition instanceof LocalResourcePosition) {
			return new LocalProjectAccessMethod(((LocalResourcePosition) resourcePosition).getProject());
		} else if (resourcePosition instanceof RemoteResourcePosition) {
			DatasetMetadata datasetMetadata = ((RemoteResourcePosition) resourcePosition)
					.getDatasetMetadata();
			if (!datasetMetadata.isAccessible()) {
				throw new IllegalArgumentException(String.format("Dataset %s is not accessible. "
						+ "Please verify that in the Metadata Registry there is the SPARQL endpoint of this dataset or it is dereferenceable.",
						datasetMetadata.getIdentity()));
			}

			return datasetMetadata.getSparqlEndpointMetadata()
					.<AccessMethod>map(meta -> new SPARQLAccessMethod(meta.getEndpoint(),
							!meta.getLimitations().contains(METADATAREGISTRY.NO_AGGREGATION)))
					.orElseGet(DerefenciationAccessMethod::new);
		} else {
			return new DerefenciationAccessMethod();
		}
	}

	/**
	 * Returns a connection to the repository associated with the provided <em>resource position</em>, if it
	 * is a {@link LocalResourcePosition}.
	 * 
	 * @param consumer
	 * @param resourcePosition
	 * 
	 * @return
	 * @throws ProjectAccessException
	 */
	private @Nullable RepositoryConnection conditionalAcquireConnectionToProject(ProjectConsumer consumer,
			ResourcePosition resourcePosition) throws ProjectAccessException {
		if (resourcePosition instanceof LocalResourcePosition) {
			return acquireManagedConnectionToProject(consumer,
					((LocalResourcePosition) resourcePosition).getProject());
		} else {
			return null;
		}
	}

	@STServiceOperation
	@Read
	public List<AnnotatedValue<IRI>> getLexicalizationProperties(@Optional Resource resource,
			@Optional ResourcePosition resourcePosition)
			throws ProjectAccessException {
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
		HashSet<IRI> objectProps = Sets.newHashSet(SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL,
				ONTOLEX.IS_DENOTED_BY);

		for (IRI pred : getLexicalizationPropertiesHelper(resource, resourcePosition)) {
			AnnotatedValue<IRI> annotatedPred = new AnnotatedValue<>(pred);
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
			ResourcePosition resourcePosition) {
		if (resourcePosition instanceof LocalResourcePosition) {
			Project hostingProject = ((LocalResourcePosition) resourcePosition).getProject();
			if (hostingProject.getLexicalizationModel().equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
				return Arrays.asList(SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL);
			} else if (hostingProject.getLexicalizationModel().equals(Project.SKOS_LEXICALIZATION_MODEL)) {
				return Arrays.asList(SKOS.PREF_LABEL, SKOS.ALT_LABEL, SKOS.HIDDEN_LABEL);
			} else if (hostingProject.getLexicalizationModel()
					.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
				return Arrays.asList(ONTOLEX.IS_DENOTED_BY);
			} else {
				return Arrays.asList(RDFS.LABEL);
			}
		}

		return Arrays.asList(RDFS.LABEL, SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL,
				SKOS.PREF_LABEL, SKOS.ALT_LABEL, SKOS.HIDDEN_LABEL);
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

	/**
	 * Returns a set of properties that should be excluded from the Resource View, unless
	 * {@code ignorePropertyExclusons} is {@code false}. Currently, the property {@link LIME#ENTRY} is always
	 * excluded.
	 * 
	 * @param resourcePosition
	 * @return
	 */
	protected Set<IRI> getExcludedProperties(ResourcePosition resourcePosition,
			boolean ignorePropertyExclusons) {
		return ignorePropertyExclusons ? Collections.emptySet() : Collections.singleton(LIME.ENTRY);
	}

	/**
	 * Returns a set of properties that should be excluded from the Resource View. Currently, the property
	 * {@link LIME#ENTRY} is always excluded.
	 * 
	 * @param resourcePosition
	 * @return
	 */
	protected Set<IRI> getExcludedProperties(ResourcePosition resourcePosition) {
		return Collections.singleton(LIME.ENTRY);
	}

	/**
	 * Returns a {@code FILTER} that can be used to exclude the properties returned by
	 * {@link #getExcludedProperties(ResourcePosition)} only if {@code ignorePropertyExclusions} is
	 * {@code false}.
	 * 
	 * @param resourcePosition
	 * @param varName
	 * @param ignorePropertyExclusions
	 * @return
	 */
	protected String getPropertyExclusionFilter(ResourcePosition resourcePosition, String varName,
			boolean ignorePropertyExclusions) {
		if (ignorePropertyExclusions) {
			return "";
		} else {
			return getPropertyExclusionFilter(resourcePosition, varName);
		}
	}

	/**
	 * Returns a {@code FILTER} that can be used to exclude the properties returned by
	 * {@link #getExcludedProperties(ResourcePosition)}.
	 * 
	 * @param resourcePosition
	 * @param varName
	 * @return
	 */
	protected String getPropertyExclusionFilter(ResourcePosition resourcePosition, String varName) {
		Set<IRI> props = getExcludedProperties(resourcePosition);

		if (props.isEmpty())
			return "";

		return "FILTER ( ?" + varName + " NOT IN "
				+ props.stream().map(RenderUtils::toSPARQL).collect(joining(", ", "(", ")")) + ")\n";
	}

	public Repository createSPARQLRepository(String sparqlEndpoint) {
		return new SPARQLRepository(sparqlEndpoint) {
			@Override
			protected SPARQLProtocolSession createHTTPClient() {
				SPARQLProtocolSession rv = super.createHTTPClient();
				rv.setValueFactory(ErrorRecoveringValueFactory.getInstance());
				return rv;
			}
		};
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

	/**
	 * Root class of access methods, such as local access, SPARQL or dereferenciation
	 */
	public abstract class AccessMethod {
		public abstract String getName();

		public abstract Model retrieveStatements(ResourcePosition resourcePosition, Resource resource,
				boolean includeInferred, boolean ignorePropertyExclusions, MutableLong excludedObjectsCount)
				throws ProjectAccessException, RDF4JException, IOException;

		public Model retrievePredicateInformation(ResourcePosition resourcePosition,
				Set<IRI> resourcePredicates, Set<IRI> specialProperties,
				Map<Resource, Map<String, Value>> resource2attributes, Model statements)
				throws ProjectAccessException {
			Model propertiesModel = new LinkedHashModel();
			specialProperties.stream().forEach(p -> propertiesModel.add(p, RDFS.SUBPROPERTYOF, p));
			return propertiesModel;
		}

		public abstract SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformation(
				ResourcePosition resourcePosition, Resource resource, boolean includeInferred,
				Model statements, Set<IRI> resourcePredicates, boolean ignorePropertyExclusions)
				throws ProjectAccessException, AssessmentException;

		/**
		 * Base method that can be used by subclasses for implementing the API
		 * 
		 * @param conn
		 * @param resourcePosition
		 * @param resourcePredicates
		 * @param specialProperties
		 * @param resource2attributes
		 * @param statements
		 * @return
		 * @throws ProjectAccessException
		 */
		protected Model retrievePredicateInformation(RepositoryConnection conn,
				ResourcePosition resourcePosition, Set<IRI> resourcePredicates, Set<IRI> specialProperties,
				Map<Resource, Map<String, Value>> resource2attributes, Model statements) {

			SimpleValueFactory vf = SimpleValueFactory.getInstance();

			String predicatesValuesFrag = Sets.union(resourcePredicates, specialProperties).stream()
					.map(p -> "(" + NTriplesUtil.toNTriplesString(p) + ")").collect(joining(" "));
			String specialPredicatesValuesFrag = specialProperties.stream()
					.map(p -> "(" + NTriplesUtil.toNTriplesString(p) + ")").collect(joining(" "));

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
			Model propertyModel = new LinkedHashModel();
			qb.runQuery(conn).stream().forEach(annotatedPredicate -> {
				List<IRI> parents = Arrays
						.stream(annotatedPredicate.getAttributes().getOrDefault("parents", RDF.NIL)
								.stringValue().split("><"))
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

		/**
		 * Base method that can be used by subclasses for implementing the API
		 * 
		 * @param useGroupBy
		 *            TODO
		 */
		protected SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformationFromConnection(
				RepositoryConnection conn, Model statements, RenderingEngine renderingEngine,
				ResourcePosition resourcePosition, Resource resource, boolean includeInferred,
				Set<IRI> resourcePredicates, boolean ignorePropertyExclusions, boolean useGroupBy) {
			String propertyExclusionFilterWithVarPredicate = getPropertyExclusionFilter(resourcePosition,
					"predicate", ignorePropertyExclusions);

			StringBuilder sb = new StringBuilder();
			sb.append(
			// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                         \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                              \n" +
					" PREFIX owl: <http://www.w3.org/2002/07/owl#>                                      \n" +
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                               \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                               \n" +
					" SELECT ?resource " + (useGroupBy ? "?predicate (MAX(?attr_creShowTemp) as ?predattr_creShow) " + generateNatureSPARQLSelectPart() : "") + "\n"+
					" WHERE { 																			\n" +
					"   {                                                                               \n" +
					"       {                                                                           \n");
			if(resource instanceof IRI) {
				sb.append("BIND (<"+resource.stringValue()+"> AS ?subjectResource )\n");
			}
			sb.append("         ?subjectResource ?predicate ?tempResource .                               \n" +
					"         " + propertyExclusionFilterWithVarPredicate +
					"       }                                                                           \n" +
					"     ?tempResource (rdf:rest*/rdf:first)* ?resource                                \n" +
					"   } UNION {                                                                       \n");
			if(resource instanceof IRI) {
				sb.append("BIND (<"+resource.stringValue()+"> AS ?subjectResource )\n");
			}
			sb.append("     bind(?subjectResource as ?resource)                                           \n" +
					"   }                                                                               \n" +
					"   FILTER(!isLITERAL(?resource))                                                   \n" +
					(useGroupBy ? generateNatureSPARQLWherePart("resource") : "")
					// @formatter:on
			);

			if (useGroupBy) {
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
						"        " + propertyExclusionFilterWithVarPredicate +
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

			} else {
				sb.append(" }\n");
			}
			QueryBuilder qb = createQueryBuilder(sb.toString());
			if (useGroupBy && renderingEngine != null) {
				qb.processRendering(renderingEngine);
			}
			qb.processQName();
			qb.process(XLabelLiteralFormQueryProcessor.INSTANCE, "resource", "attr_literalForm");
			if (useGroupBy) {
				qb.process(DecompComponentRenderer.INSTANCE_WITHOUT_FALLBACK, "resource",
						"attr_decompComponentRendering");
				qb.process(FormRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_ontolexFormRendering");
				qb.process(LexicalEntryRenderer.INSTANCE_WITHOUT_FALLBACK, "resource",
						"attr_ontolexLexicalEntryRendering");
				qb.process(LexiconRenderer.INSTANCE_WITHOUT_FALLBACK, "resource",
						"attr_limeLexiconRendering");
			}
			if(!(resource instanceof IRI)) {
				qb.setBinding("subjectResource", resource);
			}
			qb.setIncludeInferred(includeInferred); // inference is required to properly render / assign
													// nature to inferred objects

			Repository repo = null;
			Collection<BindingSet> bindingSets;
			try {
				bindingSets = qb.runQuery(conn, QueryResultsProcessors.toBindingSets());
			} finally {
				if (repo != null) {
					conn.close();
					repo.shutDown();
				}
			}
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow = new HashMap<>();
			Map<Resource, Map<String, Value>> resource2attributes = new HashMap<>();

			for (BindingSet bs : bindingSets) {
				IRI predicate = (IRI) bs.getValue("predicate");

				Map<Resource, Literal> resource2CreShow = useGroupBy
						? predicate2resourceCreShow.computeIfAbsent(predicate, key -> new HashMap<>())
						: Collections.emptyMap();

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

			if (!useGroupBy) {
				SubjectAndObjectsInfos tempSubjectsAndObjectsInfos = this
						.retrieveSubjectAndObjectsAddtionalInformationFromStatements(resourcePosition,
								resource, includeInferred, statements, resourcePredicates,
								ignorePropertyExclusions);
				Map<String, Value> subjectAttributes = tempSubjectsAndObjectsInfos.resource2attributes
						.get(resource);
				resource2attributes.merge(resource, subjectAttributes, (m1, m2) -> {
					Map<String, Value> m3 = new HashMap<>(m1);
					m3.putAll(m2);
					return m3;
				});

			}

			return new SubjectAndObjectsInfos(resource2attributes, predicate2resourceCreShow);
		}

		/**
		 * Base method that can be used by subclasses for implementing the API
		 */
		protected SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformationFromStatements(
				ResourcePosition resourcePosition, Resource resource, boolean includeInferred,
				Model statements, Set<IRI> resourcePredicates, boolean ignorePropertyExclusions) {
			Repository repo = new SailRepository(new MemoryStore());
			repo.init();
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(statements);
				// It is necessary to load the OWL vocabulary, because the nature processor assumes its
				// presence
				try {
					conn.add(OntologyManager.class.getResource("owl.rdf"), null, RDFFormat.RDFXML,
							conn.getValueFactory().createIRI("http://www.w3.org/2002/07/owl"));
				} catch (RDFParseException | RepositoryException | IOException e) {
					throw new RuntimeException(e);
				}
				String nature = NatureRecognitionOrchestrator.computeNature(resource, conn);
				Map<Resource, Map<String, Value>> resourceAttributes = new HashMap<>();
				Map<String, Value> subjectAttributes = new HashMap<>();
				subjectAttributes.put("nature", SimpleValueFactory.getInstance().createLiteral(nature));
				resourceAttributes.put(resource, subjectAttributes);
				return new SubjectAndObjectsInfos(resourceAttributes, Collections.emptyMap());
			} finally {
				repo.shutDown();
			}
		}

	}

	public class LocalProjectAccessMethod extends AccessMethod {

		private Project resourceHoldingProject;

		public LocalProjectAccessMethod(Project project) {
			this.resourceHoldingProject = project;
		}

		@Override
		public String getName() {
			return "local";
		}

		@Override
		public Model retrieveStatements(ResourcePosition resourcePosition, Resource resource,
				boolean includeInferred, boolean ignorePropertyExclusions, MutableLong excludedObjectsCount)
				throws ProjectAccessException, RDF4JException {
			LocalResourcePosition localResourcePosition = (LocalResourcePosition) resourcePosition;
			RepositoryConnection managedConnection = acquireManagedConnectionToProject(getProject(),
					resourceHoldingProject);

			Model retrievedStatements = new LinkedHashModel();

			TupleQuery tupleQuery = managedConnection.prepareTupleQuery(
		// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>              \n" +
					" SELECT ?g ?s ?p ?o ?g2 ?s2 ?p2 ?o2{                                    \n" +
					"     GRAPH ?g {                                                         \n" +
					"       ?s ?p ?o .                                                       \n" +
					"       " + getPropertyExclusionFilter(localResourcePosition, "p", ignorePropertyExclusions ) +
					"     }                                                                  \n" +
					"     OPTIONAL {                                                         \n" +
					"       ?o rdf:rest* ?s2                                                 \n" +
					"       FILTER(isBLANK(?o))                                              \n" +
					"       GRAPH ?g2 {                                                      \n" +
					"         ?s2 ?p2 ?o2 .                                                  \n" +
					"         " + getPropertyExclusionFilter(localResourcePosition, "p2", ignorePropertyExclusions ) +
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
			describeQuery.setIncludeInferred(includeInferred);

			Set<IRI> excludedProperties = getExcludedProperties(localResourcePosition);

			QueryResults.stream(describeQuery.evaluate()).forEach(stmt -> {
				Resource subject = stmt.getSubject();
				IRI predicate = stmt.getPredicate();
				Value object = stmt.getObject();

				if (excludedProperties.contains(predicate)) {
					if (subject.equals(resource)) {
						excludedObjectsCount.increment();
					}
					return;
				}

				if (retrievedStatements.contains(subject, predicate, object))
					return;

				retrievedStatements.add(subject, predicate, object, NatureRecognitionOrchestrator.INFERENCE_GRAPH);
			});

			return retrievedStatements;
		}

		@Override
		public Model retrievePredicateInformation(ResourcePosition resourcePosition,
				Set<IRI> resourcePredicates, Set<IRI> specialProperties,
				Map<Resource, Map<String, Value>> resource2attributes, Model statements)
				throws ProjectAccessException {
			RepositoryConnection managedConnection = acquireManagedConnectionToProject(getProject(),
					resourceHoldingProject);
			return super.retrievePredicateInformation(managedConnection, resourcePosition, resourcePredicates,
					specialProperties, resource2attributes, statements);
		}

		@Override
		public SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformation(
				ResourcePosition resourcePosition, Resource resource, boolean includeInferred,
				Model statements, Set<IRI> resourcePredicates, boolean ignorePropertyExclusions)
				throws ProjectAccessException {
			RepositoryConnection managedConnection = acquireManagedConnectionToProject(getProject(),
					resourceHoldingProject);
			return super.retrieveSubjectAndObjectsAddtionalInformationFromConnection(managedConnection,
					statements, resourceHoldingProject.getRenderingEngine(), resourcePosition, resource,
					includeInferred, resourcePredicates, ignorePropertyExclusions, true);
		}

	}

	public class SPARQLAccessMethod extends AccessMethod {
		private IRI sparqlEndpoint;
		private boolean useGroupBy;

		public SPARQLAccessMethod(IRI sparqlEndpoint, boolean useGroupBy) {
			this.sparqlEndpoint = sparqlEndpoint;
			this.useGroupBy = useGroupBy;
		}

		@Override
		public String getName() {
			return useGroupBy ? "sparql" : "sparql-degraded";
		}

		@Override
		public Model retrieveStatements(ResourcePosition resourcePosition, Resource resource,
				boolean includeInferred, boolean ignorePropertyExclusions, MutableLong excludedObjectsCount)
				throws RDF4JException {
			Repository sparqlRepository = createSPARQLRepository(sparqlEndpoint.stringValue());
			sparqlRepository.init();
			try {
				Model retrievedStatements = new LinkedHashModel();
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

					// At this point, we know that resource is an IRI
					GraphQuery describeQuery = conn
							.prepareGraphQuery("DESCRIBE " + RenderUtils.toSPARQL(resource));
					QueryResults.stream(describeQuery.evaluate()).forEach(stmt -> {
						Resource subject = stmt.getSubject();
						IRI predicate = stmt.getPredicate();
						Value object = stmt.getObject();
						if (retrievedStatements.contains(subject, predicate, object))
							return;

						retrievedStatements.add(subject, predicate, object);
					});
				}
				return retrievedStatements;
			} finally {
				sparqlRepository.shutDown();
			}
		}

		@Override
		public Model retrievePredicateInformation(ResourcePosition resourcePosition,
				Set<IRI> resourcePredicates, Set<IRI> specialProperties,
				Map<Resource, Map<String, Value>> resource2attributes, Model statements)
				throws ProjectAccessException {
			if (useGroupBy) {
				Repository sparqlRepository = createSPARQLRepository(sparqlEndpoint.stringValue());
				sparqlRepository.init();
				try {
					try (RepositoryConnection conn = sparqlRepository.getConnection()) {

						return super.retrievePredicateInformation(conn, resourcePosition, resourcePredicates,
								specialProperties, resource2attributes, statements);
					}
				} finally {
					sparqlRepository.shutDown();
				}
			} else {
				return super.retrievePredicateInformation(resourcePosition, resourcePredicates,
						specialProperties, resource2attributes, statements);
			}
		}

		@Override
		public SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformation(
				ResourcePosition resourcePosition, Resource resource, boolean includeInferred,
				Model statements, Set<IRI> resourcePredicates, boolean ignorePropertyExclusions)
				throws AssessmentException {
			Repository sparqlRepository = createSPARQLRepository(sparqlEndpoint.stringValue());
			sparqlRepository.init();
			try (RepositoryConnection conn = sparqlRepository.getConnection()) {
				IRI dataset = ((RemoteResourcePosition) resourcePosition).getDatasetMetadata().getIdentity();
				java.util.Optional<IRI> lexicalizationModel = mediationFramework
						.assessLexicalizationModel(dataset, metadataRegistryBackend.extractProfile(dataset));
				RenderingEngine renderingEngine = BaseRenderingEngine
						.getRenderingEngineForLexicalizationModel(
								lexicalizationModel.orElse(Project.RDFS_LEXICALIZATION_MODEL))
						.orElse(null);
				return super.retrieveSubjectAndObjectsAddtionalInformationFromConnection(conn, statements,
						renderingEngine, resourcePosition, resource, includeInferred, resourcePredicates,
						ignorePropertyExclusions, useGroupBy);
			} finally {
				sparqlRepository.shutDown();
			}
		}
	}

	public class DerefenciationAccessMethod extends AccessMethod {
		@Override
		public String getName() {
			return "dereferenciation";
		}

		@Override
		public Model retrieveStatements(ResourcePosition resourcePosition, Resource resource,
				boolean includeInferred, boolean ignorePropertyExclusions, MutableLong excludedObjectsCount)
				throws RDF4JException, IOException {
			Model retrievedStatements = new LinkedHashModel();
			RDFLoader rdfLoader = RDF4JUtilities.createRobustRDFLoader();
			StatementCollector statementCollector = new StatementCollector(retrievedStatements);
			rdfLoader.load(new URL(resource.stringValue()), null, null, statementCollector);

			// Move the null context to a graph named after the resource
			Model nullCtx = new LinkedHashModel(
					retrievedStatements.filter(null, null, null, (Resource) null));
			if (!nullCtx.isEmpty()) {
				nullCtx.forEach(stmt -> retrievedStatements.add(stmt.getSubject(), stmt.getPredicate(),
						stmt.getObject(), resource));
				retrievedStatements.clear((Resource) null);
			}
			return retrievedStatements;
		}

		@Override
		public SubjectAndObjectsInfos retrieveSubjectAndObjectsAddtionalInformation(
				ResourcePosition resourcePosition, Resource resource, boolean includeInferred,
				Model statements, Set<IRI> resourcePredicates, boolean ignorePropertyExclusions) {
			return super.retrieveSubjectAndObjectsAddtionalInformationFromStatements(resourcePosition,
					resource, includeInferred, statements, resourcePredicates, ignorePropertyExclusions);
		}

	}
}

class XLabelLiteralFormQueryProcessor implements QueryBuilderProcessor {

	public static final QueryBuilderProcessor INSTANCE = new XLabelLiteralFormQueryProcessor();

	@Override
	public GraphPattern getGraphPattern(STServiceContext context) {
		return GraphPatternBuilder.create().prefix("skosxl", SKOSXL.NAMESPACE)
				.pattern("?resource skosxl:literalForm ?literalForm . FILTER(isLITERAL(?literalForm))")
				.projection(ProjectionElementBuilder.variable("literalForm")).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		return resultTable.stream().filter(bs -> bs.getValue("literalForm") != null).collect(
				groupingBy(bs -> bs.getValue("resource"), mapping(bs -> (Literal) bs.getValue("literalForm"),
						reducing(null, (v1, v2) -> v1 != null ? v1 : v2))));
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

}

class DecompComponentRenderer extends BaseRenderingEngine {

	private static AbstractLabelBasedRenderingEngineConfiguration conf;

	static {
		conf = new AbstractLabelBasedRenderingEngineConfiguration() {

			@Override
			public String getShortName() {
				return "foo";
			}

		};
		conf.languages = null;
	}

	private DecompComponentRenderer() {
		super(conf, true);
	}

	private DecompComponentRenderer(boolean fallbackToTerm) {
		super(conf, fallbackToTerm);
	}

	public static final DecompComponentRenderer INSTANCE = new DecompComponentRenderer();
	public static final DecompComponentRenderer INSTANCE_WITHOUT_FALLBACK = new DecompComponentRenderer(
			false);

	@Override
	public void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
		// @formatter:off
			"?resource <http://www.w3.org/ns/lemon/decomp#correspondsTo> [ \n" +
			"  <http://www.w3.org/ns/lemon/ontolex#canonicalForm> [ \n" +
			"    <http://www.w3.org/ns/lemon/ontolex#writtenRep> ?labelInternal \n" +
			"  ] \n" +
			"] \n"
			// @formatter:on
		);
	}

}