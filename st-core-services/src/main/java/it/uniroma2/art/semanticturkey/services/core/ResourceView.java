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

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelCreationException;
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
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
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
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.role.RoleRecognitionOrchestrator;
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
import it.uniroma2.art.semanticturkey.services.core.impl.BroaderStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.DomainsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.LexicalizationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.OntologyImportsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.PropertyFactesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.RangesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.ResourceViewSection;
import it.uniroma2.art.semanticturkey.services.core.impl.StatementCollector;
import it.uniroma2.art.semanticturkey.services.core.impl.StatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.SubClassOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.SubPropertyOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.TopConceptsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.impl.TypesStatementConsumer;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

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
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import com.google.common.collect.Iterators;

/**
 * This service produces a view showing the details of a resource. This service operates uniformly (as much as
 * possible) both on local resources and remote ones.
 * 
 */
@GenerateSTServiceController
@Validated
@Component
public class ResourceView extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);
	
	private Map<RDFResourceRolesEnum, List<StatementConsumer>> role2template;

	private TypesStatementConsumer typesStatementConsumer;

	private SubClassOfStatementConsumer subClassofStatementConsumer;

	private LexicalizationsStatementConsumer lexicalizationsStatementConsumer;

	private BroaderStatementConsumer broaderStatementConsumer;

	private SubPropertyOfStatementConsumer subPropertyOfStatementConsumer;

	private PropertyFactesStatementConsumer propertyFactesStatementConsumer;

	private DomainsStatementConsumer domainsStatementConsumer;

	private RangesStatementConsumer rangesStatementConsumer;

	private TopConceptsStatementConsumer topConceptsStatementConsumer;

	private OntologyImportsStatementConsumer ontologyImportsStatementConsumer;

	private OtherPropertiesStatementConsumer otherPropertiesStatementConsumer;

	public ResourceView() {
		typesStatementConsumer = new TypesStatementConsumer();
		subClassofStatementConsumer = new SubClassOfStatementConsumer();
		lexicalizationsStatementConsumer = new LexicalizationsStatementConsumer();
		broaderStatementConsumer = new BroaderStatementConsumer();
		subPropertyOfStatementConsumer = new SubPropertyOfStatementConsumer();
		propertyFactesStatementConsumer = new PropertyFactesStatementConsumer();
		domainsStatementConsumer = new DomainsStatementConsumer();
		rangesStatementConsumer = new RangesStatementConsumer();
		topConceptsStatementConsumer = new TopConceptsStatementConsumer();
		ontologyImportsStatementConsumer = new OntologyImportsStatementConsumer();
		otherPropertiesStatementConsumer = new OtherPropertiesStatementConsumer();
		
		
		role2template = new HashMap<RDFResourceRolesEnum, List<StatementConsumer>>();
		role2template.put(RDFResourceRolesEnum.cls, Arrays.asList(typesStatementConsumer, subClassofStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.concept, Arrays.asList(typesStatementConsumer, broaderStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.property, Arrays.asList(typesStatementConsumer, subPropertyOfStatementConsumer, lexicalizationsStatementConsumer, propertyFactesStatementConsumer, domainsStatementConsumer, rangesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.conceptScheme, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, topConceptsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.ontology, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, ontologyImportsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.individual, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
	}
	
	// TODO: implement a converter for ResourcePosition
	@GenerateSTServiceController
	public Response getResourceView(ARTResource resource, @Optional ResourcePosition resourcePosition) throws Exception {
		OWLModel owlModel = getOWLModel();
		ARTResource[] userNamedGraphs = getUserNamedGraphs();
		ARTResource workingGraph = getWorkingGraph();

		Project<? extends RDFModel> project = getProject();

		if (resourcePosition == null) {
			resourcePosition = ResourceLocator.locateResource(project, resource);
		}
		
		StatementCollector stmtCollector = new StatementCollector();

		retrieveStatements(owlModel, resource, resourcePosition, stmtCollector);

		logger.debug("Requested view for resource {} which the position of which is {}", resource,
				resourcePosition);

		// ************************************
		// Step X : Prepare subject ST resource

		// A resource is editable iff it is a locally defined resource
		boolean subjectResourceEditable = (resourcePosition instanceof LocalResourcePosition)
				&& stmtCollector.hasStatement(resource, NodeFilters.ANY, NodeFilters.ANY,
						workingGraph);

		STRDFResource stSubjectResource = STRDFNodeFactory.createSTRDFResource(resource,
				RDFResourceRolesEnum.undetermined, subjectResourceEditable, null);

		// ******************************************
		// Step X: Renderize resources & compute role

		Collection<ARTResource> resourcesToBeRendered = RDFIterators.getCollectionFromIterator(RDFIterators
				.filterResources(RDFIterators.listObjects(RDFIterators
						.createARTStatementIterator(stmtCollector.getStatements().iterator()))));
		resourcesToBeRendered.add(resource);

		RenderingEngine renderingOrchestrator = RenderingOrchestrator.getInstance();
		RoleRecognitionOrchestrator roleRecognitionOrchestrator = RoleRecognitionOrchestrator.getInstance();

		String gp_rendering = renderingOrchestrator.getGraphPatternForDescribe(resourcePosition, resource,
				"rendering_");

		String gp_role = roleRecognitionOrchestrator.getGraphPatternForDescribe(resourcePosition, resource,
				"role_");

		String gp_literalForm = "optional {?object a <http://www.w3.org/2008/05/skos-xl#Label> . ?object <http://www.w3.org/2008/05/skos-xl#literalForm> ?xlabel_literalForm}";

		String gp = String.format(
				"{{{%1$s ?resource ?object . %4$s} union {%1$s ?predicate ?resource}} {%2$s union %3$s}}",
				RDFNodeSerializer.toNT(resource), gp_rendering, gp_role, gp_literalForm);

		logger.debug("graph pattern for resource {} is {}", resource, gp);

		Collection<TupleBindings> bindings = matchGraphPattern(resourcePosition, gp);

		Map<ARTResource, String> resource2Rendering = renderingOrchestrator.render(project, resourcePosition,
				resource, stmtCollector.getStatements(), resourcesToBeRendered, bindings, "rendering_");

		Map<ARTResource, RDFResourceRolesEnum> resource2Role = roleRecognitionOrchestrator.computeRoleOf(
				project, resourcePosition, resource, stmtCollector.getStatements(), resourcesToBeRendered,
				bindings, "role_");

		Map<ARTResource, String> xLabel2LiteralForm = collectXLabels(bindings);

		// ********************************************
		// Step X: Update subject with role & rendering

		String subjectRendering = resource2Rendering.get(resource);
		if (subjectRendering != null) {
			stSubjectResource.setRendering(subjectRendering);
		}

		RDFResourceRolesEnum subjectRole = resource2Role.get(resource);
		if (subjectRole != null) {
			stSubjectResource.setRole(subjectRole);
		}

		LinkedHashMap<String, ResourceViewSection> sections = reorganizeInformation(resource, resourcePosition, subjectRole,
				stmtCollector, resource2Role, resource2Rendering, xLabel2LiteralForm);


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

	private Map<ARTResource, String> collectXLabels(Collection<TupleBindings> bindings) {
		Map<ARTResource, String> result = new HashMap<ARTResource, String>();
		for (TupleBindings b : bindings) {
			if (b.hasBinding("xlabel_literalForm")) {
				ARTNode object = b.getBoundValue("object");
				ARTNode literalForm = b.getBoundValue("xlabel_literalForm");

				if (object.isResource() && literalForm.isLiteral()) {
					ARTResource xLabel = object.asResource();
					ARTLiteral literalFormAsLiteral = literalForm.asLiteral();

					String render = literalFormAsLiteral.getLabel();
					if (literalFormAsLiteral.getLanguage() != null) {
						render = render + " (" + literalFormAsLiteral.getLanguage() + ")";
					}

					result.put(xLabel, render);
				}
			}
		}

		return result;
	}

	private LinkedHashMap<String, ResourceViewSection> reorganizeInformation(ARTResource resource, ResourcePosition resourcePosition,
			RDFResourceRolesEnum resourceRole, StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering, Map<ARTResource, String> xLabel2LiteralForm)
			throws DOMException, ModelAccessException {

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceViewSection>();
		
		for (StatementConsumer stmtConsumer : getTemplateForResourceRole(resourceRole)) {
			LinkedHashMap<String, ResourceViewSection> newResults = stmtConsumer.consumeStatements(getProject(), resource, resourcePosition, resourceRole, stmtCollector, resource2Role, resource2Rendering, xLabel2LiteralForm);
		
			result.putAll(newResults);
		}
		
		return result;
	}

	// TODO: implement a converter for ResourcePosition
	@GenerateSTServiceController
	public Response getLexicalizationProperties(@Optional ARTResource resource, @Optional ResourcePosition resourcePosition) throws ModelAccessException {
		if (resourcePosition == null) {
			resourcePosition = resource != null ? ResourceLocator.locateResource(getProject(), resource) : ResourceLocator.UNKNOWN_RESOURCE_POSITION;
		}
			
		Collection<STRDFURI> lexicalizationProperties = STRDFNodeFactory.createEmptyURICollection();
		for (ARTURIResource pred : getLexicalizationPropertiesHelper(resource, resourcePosition)) {
			STRDFURI stPred = STRDFNodeFactory
					.createSTRDFURI(
							pred,
							pred.getNamespace().equals(
											SKOSXL.NAMESPACE) ? RDFResourceRolesEnum.objectProperty
											: RDFResourceRolesEnum.annotationProperty,
							true,
							getOWLModel().getQName(pred.getURI()));
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
	public static List<ARTURIResource> getLexicalizationPropertiesHelper(ARTResource resource, ResourcePosition resourcePosition) throws ModelAccessException {
		
		if (resourcePosition instanceof LocalResourcePosition) {
			Project<?> hostingProject = ((LocalResourcePosition) resourcePosition).getProject();
			RDFModel ontModel = hostingProject.getOntModel();
			if (ontModel instanceof SKOSXLModel) {
				return Arrays.asList(SKOSXL.Res.PREFLABEL, SKOSXL.Res.ALTLABEL, SKOSXL.Res.HIDDENLABEL);
			} else if (ontModel instanceof SKOSModel) {
				return Arrays.asList(SKOS.Res.PREFLABEL, SKOS.Res.ALTLABEL, SKOS.Res.HIDDENLABEL);
			} else {
				return Arrays.asList(RDFS.Res.LABEL);
			}
		}
		
		return Arrays.asList(RDFS.Res.LABEL, SKOSXL.Res.PREFLABEL, SKOSXL.Res.ALTLABEL, SKOSXL.Res.HIDDENLABEL, SKOS.Res.PREFLABEL, SKOS.Res.ALTLABEL, SKOS.Res.HIDDENLABEL);
	}

	private Collection<TupleBindings> matchGraphPattern(ResourcePosition resourcePosition, String gp)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException,
			QueryEvaluationException, UnavailableResourceException, ProjectInconsistentException,
			ModelCreationException {

		if (resourcePosition instanceof LocalResourcePosition) {
			logger.debug("Matching pattern against local project: {}",
					((LocalResourcePosition) resourcePosition).getProject());
			RDFModel ontModel = ((LocalResourcePosition) resourcePosition).getProject().getOntModel();
			TupleQuery q = ontModel.createTupleQuery("select * where " + gp);
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

	private void retrieveStatements(RDFModel model, ARTResource resource, ResourcePosition position,
			StatementCollector collector) throws ModelAccessException, ModelCreationException,
			UnavailableResourceException, ProjectInconsistentException, UnsupportedQueryLanguageException,
			MalformedQueryException, QueryEvaluationException, MalformedURLException, IOException {
		if (position instanceof LocalResourcePosition) {
			logger.debug("Retrieving statements for resource {} locally", resource);
			ARTStatementIterator it = model.listStatements(resource, NodeFilters.ANY, NodeFilters.ANY, false,
					NodeFilters.ANY);
			try {
				while (it.streamOpen()) {
					ARTStatement stmt = it.getNext();
					collector.addStatement(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(),
							stmt.getNamedGraph());
				}
			} finally {
				it.close();
			}

			it = model.listStatements(resource, NodeFilters.ANY, NodeFilters.ANY, true, NodeFilters.ANY);
			try {
				while (it.streamOpen()) {
					ARTStatement stmt = it.getNext();
					collector.addInferredStatement(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
				}
			} finally {
				it.close();
			}

		} else if (position instanceof RemoteResourcePosition) {
			DatasetMetadata meta = ((RemoteResourcePosition) position).getDatasetMetadata();

			String sparqlEndpoint = meta.getSparqlEndpoint();

			if (sparqlEndpoint != null) {
				logger.debug("Retrieving statements for resource {} via SPARQL", resource);

				TripleQueryModelHTTPConnection queryModel = getCurrentModelFactory()
						.loadTripleQueryHTTPConnection(sparqlEndpoint);

				StringBuilder sb = new StringBuilder();
				sb.append("select ?pred ?obj {");
				sb.append("   ").append(RDFNodeSerializer.toNT(resource)).append(" ?pred ?obj . \n");
				sb.append("}");

				TupleBindingsIterator it = queryModel.createTupleQuery(QueryLanguage.SPARQL, sb.toString(),
						null).evaluate(true);
				try {
					while (it.streamOpen()) {
						TupleBindings tupleBindings = it.getNext();

						collector.addInferredStatement(resource, tupleBindings.getBinding("pred")
								.getBoundValue().asURIResource(), tupleBindings.getBinding("obj")
								.getBoundValue());
					}
				} finally {
					it.close();
				}
			} else if (meta.isDereferenceable()) {
				logger.debug("Retrieving statements for resource {} via dereferencing", resource);

				LinkedDataResolver resolver = getCurrentModelFactory().loadLinkedDataResolver();

				if (resource.isURIResource()) {
					Collection<ARTStatement> retrievedStatements = resolver.lookup(resource.asURIResource());

					for (ARTStatement stmt : retrievedStatements) {
						collector.addInferredStatement(stmt.getSubject(), stmt.getPredicate(),
								stmt.getObject());
					}

				}

			}
		} else {
			logger.debug("Retrieving statements for resource {} via dereferencing", resource);
			LinkedDataResolver resolver = getCurrentModelFactory().loadLinkedDataResolver();

			if (resource.isURIResource()) {
				Collection<ARTStatement> retrievedStatements = resolver.lookup(resource.asURIResource());

				for (ARTStatement stmt : retrievedStatements) {
					collector.addInferredStatement(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
				}

			}

		}
	}

	private ModelFactory<?> getCurrentModelFactory() throws UnavailableResourceException,
			ProjectInconsistentException {
		return PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
	}
	
	private List<StatementConsumer> getTemplateForResourceRole(RDFResourceRolesEnum role) {
		if (role.isProperty()) {
			return role2template.get(RDFResourceRolesEnum.property);
		} else if (role.isClass()) {
			return role2template.get(RDFResourceRolesEnum.cls);
		} else {
			List<StatementConsumer> result = role2template.get(role);
			
			if (result != null) {
				return result;
			} else {
				return role2template.get(RDFResourceRolesEnum.individual);
			}
		}
	}
	
}
