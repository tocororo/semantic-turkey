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

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.google.common.collect.Iterators;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.LinkedDataResolver;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.models.TripleQueryModelHTTPConnection;
import it.uniroma2.art.owlart.models.impl.OWLModelImpl;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.rendering.RenderingOrchestrator;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.core.impl.ResourceViewSection;
import it.uniroma2.art.semanticturkey.services.core.impl.StatementCollector;
import it.uniroma2.art.semanticturkey.services.core.impl.StatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.StatementConsumerProvider;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * This service produces a view showing the details of a resource. This service operates uniformly (as much as
 * possible) both on local resources and remote ones.
 * 
 */
@GenerateSTServiceController
@Validated
@Component
public class ResourceView extends STServiceAdapter {

	public static final ARTURIResource INFERENCE_GRAPH = VocabUtilities.nodeFactory
			.createURIResource("http://semanticturkey/inference-graph");

	private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);

	@Autowired
	private ResourceLocator resourceLocator;

	@Autowired
	private StatementConsumerProvider statementConsumerProvider;

	@GenerateSTServiceController
	public Response getResourceView(ARTResource resource, @Optional ResourcePosition resourcePosition)
			throws Exception {
		// ARTResource[] userNamedGraphs = getUserNamedGraphs();
		ARTResource workingGraph = getWorkingGraph();

		Project<? extends RDFModel> project = getProject();

		if (resourcePosition == null) {
			resourcePosition = resourceLocator.locateResource(project, resource);
		}

		OWLModel stmtCollector = createEmptyOWLModel();

		retrieveStatements(resource, resourcePosition, stmtCollector);

		logger.debug("Requested view for resource {} whose position is {}", resource, resourcePosition);

		// ************************************
		// Step X : Prepare subject ST resource

		// A resource is editable iff it is a locally defined resource
		boolean subjectResourceEditable = (resourcePosition instanceof LocalResourcePosition)
				&& stmtCollector.isLocallyDefined(resource, workingGraph);

		STRDFResource stSubjectResource = STRDFNodeFactory.createSTRDFResource(resource,
				RDFResourceRolesEnum.undetermined, subjectResourceEditable, null);
		stSubjectResource.setInfo("resourcePosition", resourcePosition.toString());

		// ******************************************
		// Step X: Renderize resources & compute role

		Collection<ARTResource> resourcesToBeRendered = RDFIterators.getCollectionFromIterator(
				RDFIterators.filterResources(RDFIterators.listObjects(stmtCollector.listStatements(resource,
						NodeFilters.ANY, NodeFilters.ANY, false, NodeFilters.ANY))));
		resourcesToBeRendered.add(resource);

		RenderingEngine renderingOrchestrator = RenderingOrchestrator.getInstance();
		RoleRecognitionOrchestrator roleRecognitionOrchestrator = RoleRecognitionOrchestrator.getInstance();

		String gp_rendering = renderingOrchestrator.getGraphPatternForDescribe(resourcePosition, resource,
				"rendering_");

		String gp_role = roleRecognitionOrchestrator.getGraphPatternForDescribe(resourcePosition, resource,
				"role_");

		String gp_literalForm = "optional {?resource a <http://www.w3.org/2008/05/skos-xl#Label> . ?resource <http://www.w3.org/2008/05/skos-xl#literalForm> ?resource_xlabel_literalForm . } optional {?object a <http://www.w3.org/2008/05/skos-xl#Label> . ?object <http://www.w3.org/2008/05/skos-xl#literalForm> ?object_xlabel_literalForm}";

		String gp = String.format("{{?resource ?predicate ?object . %1$s} {%2$s union %3$s}}", gp_literalForm,
				gp_rendering, gp_role);

		logger.debug("graph pattern for resource {} is {}", resource, gp);

		Collection<TupleBindings> bindings = matchGraphPattern(resourcePosition, resource, gp);

		Map<ARTResource, String> resource2Rendering = renderingOrchestrator.render(project, resourcePosition,
				resource, stmtCollector, resourcesToBeRendered, bindings, "rendering_");

		logger.debug("graph pattern: {}", gp);
		logger.debug("resources to be rendered: {}", resourcesToBeRendered);
		logger.debug("resource2Rendering: {}", resource2Rendering);

		Map<ARTResource, RDFResourceRolesEnum> resource2Role = roleRecognitionOrchestrator.computeRoleOf(
				project, resourcePosition, resource, stmtCollector, resourcesToBeRendered, bindings, "role_");

		Map<ARTResource, ARTLiteral> xLabel2LiteralForm = collectXLabels(bindings);

		// ********************************************
		// Step X: Update subject with role & rendering

		String subjectRendering = resource2Rendering.get(resource);
		if (subjectRendering != null) {
			stSubjectResource.setRendering(subjectRendering);
		}

		RDFResourceRolesEnum subjectRole = resource2Role.get(resource);
		if (subjectRole != null) {
			stSubjectResource.setRole(subjectRole);
		} else {
			subjectRole = RDFResourceRolesEnum.undetermined;
		}

		if (subjectRole == RDFResourceRolesEnum.xLabel) {
			ARTLiteral lit = xLabel2LiteralForm.get(resource);

			if (lit != null) {
				stSubjectResource.setRendering(lit.getLabel());

				String lang = lit.getLanguage();

				if (lang != null) {
					stSubjectResource.setInfo("lang", lang);
				}
			}
		}

		LinkedHashMap<String, ResourceViewSection> sections = reorganizeInformation(resource,
				resourcePosition, workingGraph, subjectRole, stmtCollector, resource2Role, resource2Rendering,
				xLabel2LiteralForm);

		// ****************************************
		// Step X : Produces the OLD-style response
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		// Append the subject resource

		Element resourceElement = XMLHelp.newElement(dataElement, "resource");
		RDFXMLHelp.addRDFNode(resourceElement, stSubjectResource);

		// Append the various sections

		for (Entry<String, ResourceViewSection> entry : sections.entrySet()) {
			Element sectionElement = XMLHelp.newElement(dataElement, entry.getKey());
			entry.getValue().appendToElement(sectionElement);
		}

		return response;

	}

	/**
	 * Retrieves the statements about <code>resource</code> and places them inside <code>stmtCollector</code>.
	 * <p>
	 * The retrieval mechanism depends on the <code>position</code> of the resource, which may be either an
	 * open local project or a remote dataset (with or without a SPARQL endpoint).
	 * </p>
	 * <p>
	 * In case of local projects the statements will be differentiated between different graphs and inferred
	 * statements will be properly identified, while in all other cases they are simply put in one graph. The
	 * inferred statements as well as all statements in case of remote datasets are put in the graph
	 * {@link #INFERENCE_GRAPH}.
	 * </p>
	 * 
	 * @param resource
	 * @param position
	 * @param stmtCollector
	 * @throws ModelAccessException
	 * @throws ModelCreationException
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws UnsupportedQueryLanguageException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ModelUpdateException
	 */
	private void retrieveStatements(ARTResource resource, ResourcePosition position, RDFModel stmtCollector)
			throws ModelAccessException, ModelCreationException, UnavailableResourceException,
			ProjectInconsistentException, UnsupportedQueryLanguageException, MalformedQueryException,
			QueryEvaluationException, MalformedURLException, IOException, ModelUpdateException {
		if (position instanceof LocalResourcePosition) {
			logger.debug("Retrieving statements for resource {} locally", resource);
			// RDFModel model = ((LocalResourcePosition) position).getProject().getOntModel();
			RDFModel model = acquireConnection(((LocalResourcePosition) position).getProject());
			ARTStatementIterator it = model.listStatements(resource, NodeFilters.ANY, NodeFilters.ANY, false,
					NodeFilters.ANY);
			try {
				while (it.streamOpen()) {
					ARTStatement stmt = it.getNext();
					stmtCollector.addStatement(stmt);
				}
			} finally {
				it.close();
			}

			GraphQuery describeQuery;

			if (resource.isURIResource()) {
				describeQuery = model.createGraphQuery("describe " + RDFNodeSerializer.toNT(resource));
			} else {
				describeQuery = model
						.createGraphQuery("describe ?resource where {bind(?resource2 as ?resource)}");
				describeQuery.setBinding("resource2", resource);
			}
			it = describeQuery.evaluate(true);
			try {
				while (it.streamOpen()) {
					ARTStatement stmt = it.getNext();
					if (!stmtCollector.hasStatement(stmt, false, NodeFilters.ANY)) {
						stmtCollector.addTriple(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(),
								INFERENCE_GRAPH);
					}
				}
			} finally {
				it.close();
			}

		} else if (position instanceof RemoteResourcePosition) {
			DatasetMetadata meta = ((RemoteResourcePosition) position).getDatasetMetadata();

			String sparqlEndpoint = meta.getSparqlEndpoint();

			if (sparqlEndpoint != null) {
				logger.debug("Retrieving statements for resource {} via SPARQL", resource);

				TripleQueryModelHTTPConnection conn = getCurrentModelFactory()
						.loadTripleQueryHTTPConnection(sparqlEndpoint);

				GraphQuery describeQuery = conn.createGraphQuery(QueryLanguage.SPARQL, "describe ?resource",
						null);
				describeQuery.setBinding("resource", resource);
				try {
					ARTStatementIterator it = describeQuery.evaluate(true);
					try {
						while (it.streamOpen()) {
							ARTStatement stmt = it.getNext();
							if (!stmtCollector.hasStatement(stmt, false, NodeFilters.ANY)) {
								stmtCollector.addTriple(stmt.getSubject(), stmt.getPredicate(),
										stmt.getObject(), INFERENCE_GRAPH);
							}
						}
					} finally {
						it.close();
					}
				} finally {
					conn.disconnect();
				}
			} else if (meta.isDereferenceable()) {
				logger.debug("Retrieving statements for resource {} via dereferencing", resource);

				LinkedDataResolver resolver = getCurrentModelFactory().loadLinkedDataResolver();

				if (resource.isURIResource()) {
					Collection<ARTStatement> retrievedStatements = resolver.lookup(resource.asURIResource());

					for (ARTStatement stmt : retrievedStatements) {
						stmtCollector.addTriple(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(),
								INFERENCE_GRAPH);
					}

				}

			}
		} else {
			logger.debug("Retrieving statements for resource {} via dereferencing", resource);
			LinkedDataResolver resolver = getCurrentModelFactory().loadLinkedDataResolver();

			if (resource.isURIResource()) {
				Collection<ARTStatement> retrievedStatements = resolver.lookup(resource.asURIResource());

				for (ARTStatement stmt : retrievedStatements) {
					stmtCollector.addTriple(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(),
							INFERENCE_GRAPH);
				}

			}

		}
	}

	/**
	 * Protected method that the resource view may use to acquire a connection to the model of a project
	 * (possibly not the current one). This method is intended to be invoked during the handling of a user
	 * request, and the acquired connections are automatically released when the request handling is complete.
	 * 
	 * @param project
	 * @return
	 * @throws ModelCreationException
	 */
	protected RDFModel acquireConnection(Project<?> project) throws ModelCreationException {
		return connectionAcquisitionHelper.acquireConnection(project);
	}

	private Map<ARTResource, ARTLiteral> collectXLabels(Collection<TupleBindings> bindings) {

		String[] subjectVariables = { "resource", "object" };

		String[] xlabelVariables = new String[subjectVariables.length];

		for (int i = 0; i < subjectVariables.length; i++) {
			xlabelVariables[i] = subjectVariables[i] + "_xlabel_literalForm";
		}

		Map<ARTResource, ARTLiteral> result = new HashMap<ARTResource, ARTLiteral>();
		for (TupleBindings b : bindings) {
			for (int i = 0; i < subjectVariables.length; i++) {
				String subjVar = subjectVariables[i];
				String xLabelVar = xlabelVariables[i];

				if (b.hasBinding(xLabelVar)) {
					ARTNode subject = b.getBoundValue(subjVar);
					ARTNode literalForm = b.getBoundValue(xLabelVar);

					if (subject.isResource() && literalForm.isLiteral()) {
						ARTResource xLabel = subject.asResource();
						ARTLiteral literalFormAsLiteral = literalForm.asLiteral();

						result.put(xLabel, literalFormAsLiteral);
					}
				}
			}
		}

		return result;
	}

	private LinkedHashMap<String, ResourceViewSection> reorganizeInformation(ARTResource resource,
			ResourcePosition resourcePosition, ARTResource workingGraph, RDFResourceRolesEnum resourceRole,
			OWLModel stmtCollector, Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering, Map<ARTResource, ARTLiteral> xLabel2LiteralForm)
					throws DOMException, ModelAccessException {

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();

		StatementCollector newCollector = new StatementCollector();

		try (ARTStatementIterator it = stmtCollector.listStatements(NodeFilters.ANY, NodeFilters.ANY,
				NodeFilters.ANY, false, NodeFilters.ANY)) {
			while (it.streamOpen()) {
				ARTStatement stmt = it.getNext();
				newCollector.addStatement(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(),
						stmt.getNamedGraph());
			}
		}

		for (StatementConsumer stmtConsumer : statementConsumerProvider
				.getTemplateForResourceRole(resourceRole)) {
			LinkedHashMap<String, ResourceViewSection> newResults = stmtConsumer.consumeStatements(
					getProject(), resource, resourcePosition, workingGraph, resourceRole, newCollector,
					resource2Role, resource2Rendering, xLabel2LiteralForm);

			result.putAll(newResults);
		}

		return result;
	}

	@GenerateSTServiceController
	public Response getLexicalizationProperties(@Optional ARTResource resource,
			@Optional ResourcePosition resourcePosition) throws ModelAccessException, ProjectAccessException {
		if (resourcePosition == null) {
			resourcePosition = resource != null ? resourceLocator.locateResource(getProject(), resource)
					: ResourceLocator.UNKNOWN_RESOURCE_POSITION;
		}

		Collection<STRDFURI> lexicalizationProperties = STRDFNodeFactory.createEmptyURICollection();
		for (ARTURIResource pred : getLexicalizationPropertiesHelper(resource, resourcePosition)) {
			STRDFURI stPred = STRDFNodeFactory.createSTRDFURI(pred,
					pred.getNamespace().equals(SKOSXL.NAMESPACE) ? RDFResourceRolesEnum.objectProperty
							: RDFResourceRolesEnum.annotationProperty,
					true, getOWLModel().getQName(pred.getURI()));
			lexicalizationProperties.add(stPred);
		}

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		RDFXMLHelp.addRDFNodes(response.getDataElement(), lexicalizationProperties);

		return response;
	}

	// TODO place this method into a better place
	public static void minimizeDomainRanges(List<STRDFNode> typeList) {
		// @author starred
		// TODO this should be replaced by an efficient procedure for producing the shortest number of ranges
		// which are NOT in a hierarchical relationship among them has to be found. This can be complemented
		// with quick heuristics. I'll write a few heuristic first, so this will not be a complete filter

		// TODO also, restrictions should be reduced to a set of elements (it is possible, on a first attempt,
		// that both OR and AND of types are translated to their sequence, as it is then up to the user, in
		// case of an AND, to take an instance which respects all the ANDed types.

		Set<STRDFNode> typeSet = new HashSet<STRDFNode>(typeList);

		Iterator<STRDFNode> it = typeList.iterator();
		while (it.hasNext()) {
			STRDFNode node = it.next();

			if (node.isExplicit())
				continue;

			if (node.getARTNode().equals(RDFS.Res.LITERAL)) {
				it.remove();
			} else if (node.getARTNode().equals(RDFS.Res.RESOURCE)) {

				for (STRDFNode sn : typeSet) {
					if (sn.getARTNode().equals(OWL.Res.THING)) {
						it.remove();
						break;
					}
				}

			}

		}
	}

	// TODO place this method into a better place
	public static List<ARTURIResource> getLexicalizationPropertiesHelper(ARTResource resource,
			ResourcePosition resourcePosition) throws ModelAccessException {

		if (resourcePosition instanceof LocalResourcePosition) {
			Project<?> hostingProject = ((LocalResourcePosition) resourcePosition).getProject();
			RDFModel ontModel = hostingProject.getPrimordialOntModel();
			if (ontModel instanceof SKOSXLModel) {
				return Arrays.asList(SKOSXL.Res.PREFLABEL, SKOSXL.Res.ALTLABEL, SKOSXL.Res.HIDDENLABEL);
			} else if (ontModel instanceof SKOSModel) {
				return Arrays.asList(SKOS.Res.PREFLABEL, SKOS.Res.ALTLABEL, SKOS.Res.HIDDENLABEL);
			} else {
				return Arrays.asList(RDFS.Res.LABEL);
			}
		}

		return Arrays.asList(RDFS.Res.LABEL, SKOSXL.Res.PREFLABEL, SKOSXL.Res.ALTLABEL,
				SKOSXL.Res.HIDDENLABEL, SKOS.Res.PREFLABEL, SKOS.Res.ALTLABEL, SKOS.Res.HIDDENLABEL);
	}

	private Collection<TupleBindings> matchGraphPattern(ResourcePosition resourcePosition,
			ARTResource resource, String gp) throws UnsupportedQueryLanguageException, ModelAccessException,
					MalformedQueryException, QueryEvaluationException, UnavailableResourceException,
					ProjectInconsistentException, ModelCreationException {

		if (resourcePosition instanceof LocalResourcePosition) {
			logger.debug("Matching pattern against local project: {}",
					((LocalResourcePosition) resourcePosition).getProject());
			// RDFModel ontModel = ((LocalResourcePosition) resourcePosition).getProject().getOntModel();
			RDFModel ontModel = acquireConnection(((LocalResourcePosition) resourcePosition).getProject());
			TupleQuery q = ontModel.createTupleQuery("select * where " + gp);
			q.setBinding("resource", resource);
			TupleBindingsIterator bindingsIt = q.evaluate(true);
			try {
				return Arrays.asList(Iterators.toArray(bindingsIt, TupleBindings.class));
			} finally {
				bindingsIt.close();
			}
		}

		if (resourcePosition instanceof RemoteResourcePosition) {
			RemoteResourcePosition remoteResourcePosition = (RemoteResourcePosition) resourcePosition;
			DatasetMetadata meta = remoteResourcePosition.getDatasetMetadata();

			logger.debug("Matching pattern against remote dataset {}", meta);

			String sparqlEndpoint = meta.getSparqlEndpoint();

			if (sparqlEndpoint != null) {
				ModelFactory<?> fact = getCurrentModelFactory();
				TripleQueryModelHTTPConnection conn = fact.loadTripleQueryHTTPConnection(sparqlEndpoint);
				try {
					TupleQuery q = conn.createTupleQuery(QueryLanguage.SPARQL, "select * where " + gp, null);
					q.setBinding("resource", resource);
					TupleBindingsIterator bindingsIt = q.evaluate(true);
					try {
						return Arrays.asList(Iterators.toArray(bindingsIt, TupleBindings.class));
					} finally {
						bindingsIt.close();
					}
				} finally {
					conn.disconnect();
				}
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Returns the {@link ModelFactory} used by the active project (see {@link #getProject()}).
	 * 
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 */
	private ModelFactory<?> getCurrentModelFactory()
			throws UnavailableResourceException, ProjectInconsistentException {
		return PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
	}

	/**
	 * Returns an empty {@link OWLModel} created in a lightweight manner by the current model factory (see
	 * {@link #getCurrentModelFactory()}).
	 * 
	 * @return
	 * @throws ProjectInconsistentException
	 * @throws UnavailableResourceException
	 */
	private OWLModel createEmptyOWLModel() throws UnavailableResourceException, ProjectInconsistentException {
		return new OWLModelImpl(getCurrentModelFactory().createLightweightRDFModel());
	}

	/**
	 * This class is public only for technical reasons: it is not part of the public API.
	 */
	public static class ConnectionAcquisitionHelper implements Closeable {
		private Set<Project<?>> projects2close = new HashSet<>();

		public RDFModel acquireConnection(Project<?> project) throws ModelCreationException {
			if (!project.isModelBoundToThread()) {
				project.createModelAndBoundToThread();
				projects2close.add(project);
			}

			return project.getOntModel();
		}

		@Override
		public void close() throws IOException {
			for (Project<?> aProject : projects2close) {
				try {
					System.out.println("Closing dependent project");
					aProject.getOntModel().close();
				} catch (ModelUpdateException e) {
					logger.debug("An exception occured when closing a dependent project", e);
				}
			}
		}
	}

	// The machinery below (i.e. the field connectionAcquisitionHelper and the @Bean annotated method
	// ConnectionAcquisitionHelper connectionAcquisitionHelper()) creates a request-scoped helper object,
	// which can be used to acquire a connection to additional projects other than the current one.

	@Autowired
	private ConnectionAcquisitionHelper connectionAcquisitionHelper;

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	private ConnectionAcquisitionHelper connectionAcquisitionHelper() {
		return new ConnectionAcquisitionHelper();
	}
}
