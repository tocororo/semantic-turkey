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
import it.uniroma2.art.owlart.models.TripleQueryModelHTTPConnection;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
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
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
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
import it.uniroma2.art.semanticturkey.services.core.impl.StatementCollector;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;
import it.uniroma2.art.semanticturkey.vocabulary.STVocabUtilities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
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

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

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

	@GenerateSTServiceController
	public Response getResourceView(ARTResource resource) throws Exception {
		OWLModel owlModel = getOWLModel();
		ARTResource[] userNamedGraphs = getUserNamedGraphs();
		ARTResource workingGraph = getWorkingGraph();

		Project<? extends RDFModel> project = getProject();

		ResourcePosition resourcePosition = ResourceLocator.locateResource(project, resource);
		StatementCollector stmtCollector = new StatementCollector();

		retrieveStatements(owlModel, resource, resourcePosition, stmtCollector);

		logger.debug("Requested view for resource {} which the position of which is {}", resource,
				resourcePosition);

		// ************************************
		// Step X : Prepare subject ST resource

		// A resource is editable iff it is a locally defined resource
		boolean subjectResourceEditable = (resourcePosition instanceof LocalResourcePosition)
				&& stmtCollector.hasStatement(resource, NodeFilters.ANY, NodeFilters.ANY,
						NodeFilters.MAINGRAPH);

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

		LinkedHashMap<String, ResourceViewSection> sections = reorganizeInformation(resource, subjectRole,
				stmtCollector, resource2Role, resource2Rendering, xLabel2LiteralForm);

		// ********************************************
		// Step X : Populate other properties structure

		Map<ARTURIResource, STRDFResource> art2STRDFPredicates = new HashMap<ARTURIResource, STRDFResource>();
		Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory.createPredicateObjectsList(
				art2STRDFPredicates, resultPredicateObjectValues);

		for (ARTStatement stmt : stmtCollector.getStatements()) {
			Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);

			if (!stmt.getSubject().equals(resource))
				continue;

			ARTURIResource pred = stmt.getPredicate();

			if (STVocabUtilities.isHiddenResource(pred, getProject().getOntologyManager())) {
				continue;
			}

			STRDFResource stPred = art2STRDFPredicates.get(pred);

			if (stPred == null) {
				stPred = STRDFNodeFactory.createSTRDFURI(pred,
						resource2Role.containsKey(pred) ? resource2Role.get(pred)
								: RDFResourceRolesEnum.property, true, owlModel.getQName(pred.getURI()));
				art2STRDFPredicates.put(pred, stPred);
			}

			ARTNode obj = stmt.getObject();

			STRDFNode stNode = STRDFNodeFactory.createSTRDFNode(owlModel, obj, false,
					graphs.contains(NodeFilters.MAINGRAPH), false);

			if (stNode.isResource()) {
				((STRDFResource) stNode).setRendering(resource2Rendering.get(obj));
				((STRDFResource) stNode).setRole(resource2Role.get(obj));
			}

			stNode.setInfo("graphs", Joiner.on(",").join(graphs));

			resultPredicateObjectValues.put(pred, stNode);
		}

		// ****************************************
		// Step X : Produces the OLD-style response
		XMLResponseREPLY response = servletUtilities.createReplyResponse("getResourceView", RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		// Append the subject resource

		Element resourceElement = XMLHelp.newElement(dataElement, "resource");
		RDFXMLHelp.addRDFNode(resourceElement, stSubjectResource);

		// Append the various sections

		for (Entry<String, ResourceViewSection> entry : sections.entrySet()) {
			Element sectionElement = XMLHelp.newElement(dataElement, entry.getKey());
			entry.getValue().appendToElement(sectionElement);
		}

		// Append the other properties

		Element propertiesElement = XMLHelp.newElement(dataElement, "properties");
		RDFXMLHelp.addPredicateObjectList(propertiesElement, predicateObjectsList);

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

	private LinkedHashMap<String, ResourceViewSection> reorganizeInformation(ARTResource resource,
			RDFResourceRolesEnum resourceRole, StatementCollector stmtCollector,
			Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering, Map<ARTResource, String> xLabel2LiteralForm)
			throws DOMException, ModelAccessException {

		LinkedHashMap<String, ResourceViewSection> result = new LinkedHashMap<String, ResourceView.ResourceViewSection>();

		List<STRDFNode> types;

		/*
		 * Handle types
		 */
		{

			Set<ARTStatement> typeStmts = stmtCollector
					.getStatements(resource, RDF.Res.TYPE, NodeFilters.ANY);

			types = new ArrayList<STRDFNode>();

			for (ARTStatement stmt : typeStmts) {
				Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
				STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
						resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
						resource2Rendering.get(stmt.getObject()));
				stRes.setInfo("graphs", Joiner.on(",").join(graphs));
				types.add(stRes);
			}

			result.put("types", new NodeListSection(types));

			// Remove the typing statements from the resource view
			typeStmts.clear();

		}

		/*
		 * Handle supertypes
		 */
		{

			Set<ARTStatement> superTypeStmts = null;

			if (resourceRole == RDFResourceRolesEnum.cls) {
				superTypeStmts = stmtCollector.getStatements(resource, RDFS.Res.SUBCLASSOF, NodeFilters.ANY);
			} else if (RDFResourceRolesEnum.isProperty(resourceRole)) {
				superTypeStmts = stmtCollector.getStatements(resource, RDFS.Res.SUBPROPERTYOF,
						NodeFilters.ANY);
			} else if (resourceRole == RDFResourceRolesEnum.concept) {
				superTypeStmts = stmtCollector.getStatements(resource, SKOS.Res.BROADER, NodeFilters.ANY);
			}

			if (superTypeStmts != null) {
				List<STRDFNode> superTypes = new ArrayList<STRDFNode>();

				for (ARTStatement stmt : superTypeStmts) {
					Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
					STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
							resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
							resource2Rendering.get(stmt.getObject()));
					stRes.setInfo("graphs", Joiner.on(",").join(graphs));
					superTypes.add(stRes);
				}

				if (resourceRole == RDFResourceRolesEnum.cls) {
					result.put("supertypes", new NodeListSection(superTypes));
				} else if (resourceRole == RDFResourceRolesEnum.concept) {
					result.put("broaders", new NodeListSection(superTypes));
				} else {
					result.put("superproperties", new NodeListSection(superTypes));
				}
				// Remove the super-typing statements from the resource view
				superTypeStmts.clear();

			}
		}

		/*
		 * Handle lexicalizations
		 */
		{

			Map<ARTURIResource, STRDFResource> art2STRDFPredicates = new LinkedHashMap<ARTURIResource, STRDFResource>();
			Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

			PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory
					.createPredicateObjectsList(art2STRDFPredicates, resultPredicateObjectValues);

			for (ARTURIResource pred : Arrays.asList(RDFS.Res.LABEL, SKOS.Res.PREFLABEL, SKOS.Res.ALTLABEL,
					SKOS.Res.HIDDENLABEL, SKOSXL.Res.PREFLABEL, SKOSXL.Res.ALTLABEL, SKOSXL.Res.HIDDENLABEL)) {
				STRDFURI stPred = STRDFNodeFactory.createSTRDFURI(pred,
						resource2Role.containsKey(pred) ? resource2Role.get(pred) : pred.getNamespace()
								.equals(SKOSXL.NAMESPACE) ? RDFResourceRolesEnum.objectProperty
								: RDFResourceRolesEnum.annotationProperty, true, getProject().getOntModel()
								.getQName(pred.getURI()));
				art2STRDFPredicates.put(pred, stPred);
			}

			Iterator<ARTStatement> stmtIt = stmtCollector.getStatements().iterator();

			while (stmtIt.hasNext()) {
				ARTStatement stmt = stmtIt.next();

				Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);

				if (!stmt.getSubject().equals(resource))
					continue;

				ARTURIResource pred = stmt.getPredicate();

				STRDFResource stPred = art2STRDFPredicates.get(pred);

				if (stPred == null)
					continue;

				ARTNode obj = stmt.getObject();

				STRDFNode stNode = STRDFNodeFactory.createSTRDFNode(getProject().getOntModel(), obj, false,
						graphs.contains(NodeFilters.MAINGRAPH), false);

				if (stNode.isResource()) {
					RDFResourceRolesEnum role = resource2Role.get(obj);

					STRDFResource stRes = (STRDFResource) stNode;

					if (RDFResourceRolesEnum.xLabel == role) {
						stRes.setRendering(xLabel2LiteralForm.get(obj));
					} else {
						stRes.setRendering(resource2Rendering.get(obj));
					}
					stRes.setRole(role);
				}

				stNode.setInfo("graphs", Joiner.on(",").join(graphs));

				resultPredicateObjectValues.put(pred, stNode);
				stmtIt.remove();
			}

			result.put("lexicalizations", new PredicateObjectsListSection(predicateObjectsList));
		}

		/*
		 * Handle top concepts of SKOS concept schemes
		 */
		{
			if (resourceRole == RDFResourceRolesEnum.conceptScheme) {
				Set<ARTStatement> topConceptStmts = null;

				topConceptStmts = stmtCollector.getStatements(resource, SKOS.Res.HASTOPCONCEPT,
						NodeFilters.ANY);

				List<STRDFNode> topConcepts = new ArrayList<STRDFNode>();

				for (ARTStatement stmt : topConceptStmts) {
					Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
					STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
							resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
							resource2Rendering.get(stmt.getObject()));
					topConcepts.add(stRes);
					stRes.setInfo("graphs", Joiner.on(",").join(graphs));
				}

				result.put("topconcepts", new NodeListSection(topConcepts));

				// Remove the top concept statements from the resource view
				topConceptStmts.clear();

			}
		}

		/*
		 * Handle properties
		 */
		{
			if (RDFResourceRolesEnum.isProperty(resourceRole)) {

				boolean symmetric = false;
				boolean symmetricExplicit = false;

				boolean functional = false;
				boolean functionalExplicit = false;

				boolean inverseFunctional = false;
				boolean inverseFunctionalExplicit = false;

				boolean transitive = false;
				boolean transitiveExplicit = false;

				for (STRDFNode t : types) {
					if (t.getARTNode().equals(OWL.Res.SYMMETRICPROPERTY)) {
						symmetric = true;
						symmetricExplicit = t.isExplicit();
					} else if (t.getARTNode().equals(OWL.Res.FUNCTIONALPROPERTY)) {
						functional = true;
						functionalExplicit = t.isExplicit();
					} else if (t.getARTNode().equals(OWL.Res.INVERSEFUNCTIONALPROPERTY)) {
						inverseFunctional = true;
						inverseFunctionalExplicit = t.isExplicit();
					} else if (t.getARTNode().equals(OWL.Res.TRANSITIVEPROPERTY)) {
						transitive = true;
						transitiveExplicit = t.isExplicit();
					}
				}

				Set<ARTStatement> inverseOfStmts = stmtCollector.getStatements(resource, OWL.Res.INVERSEOF,
						NodeFilters.ANY);

				List<STRDFNode> inverseOf = new ArrayList<STRDFNode>();

				for (ARTStatement stmt : inverseOfStmts) {
					Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
					STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
							resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
							resource2Rendering.get(stmt.getObject()));
					stRes.setInfo("graphs", Joiner.on(",").join(graphs));
					inverseOf.add(stRes);
				}

				result.put("facets", new PropertyFacets(symmetric, symmetricExplicit, functional,
						functionalExplicit, inverseFunctional, inverseFunctionalExplicit, transitive,
						transitiveExplicit, inverseOf));

				// Remove the inverse of statements from the resource view
				inverseOfStmts.clear();

				Set<ARTStatement> domainStmts = stmtCollector.getStatements(resource, RDFS.Res.DOMAIN,
						NodeFilters.ANY);

				List<STRDFNode> domains = new ArrayList<STRDFNode>();

				for (ARTStatement stmt : domainStmts) {
					Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
					STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
							resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
							resource2Rendering.get(stmt.getObject()));
					stRes.setInfo("graphs", Joiner.on(",").join(graphs));
					domains.add(stRes);
				}
				domainStmts.clear();

				minimizeDomainRanges(domains);

				result.put("domains", new NodeListSection(domains));

				Set<ARTStatement> rangeStmts = stmtCollector.getStatements(resource, RDFS.Res.RANGE,
						NodeFilters.ANY);

				List<STRDFNode> ranges = new ArrayList<STRDFNode>();

				for (ARTStatement stmt : rangeStmts) {
					Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);
					STRDFResource stRes = STRDFNodeFactory.createSTRDFResource(stmt.getObject().asResource(),
							resource2Role.get(stmt.getObject()), graphs.contains(NodeFilters.MAINGRAPH),
							resource2Rendering.get(stmt.getObject()));
					stRes.setInfo("graphs", Joiner.on(",").join(graphs));
					ranges.add(stRes);
				}
				rangeStmts.clear();

				minimizeDomainRanges(ranges);

				result.put("ranges", new NodeListSection(ranges));

			}

			/*
			 * Handle ontology imports
			 */
			{
				if (resourceRole == RDFResourceRolesEnum.ontology) {
					Set<ARTStatement> importStmts = stmtCollector.getStatements(resource, OWL.Res.IMPORTS,
							NodeFilters.ANY);

					List<STRDFNode> imports = new ArrayList<STRDFNode>();

					for (ARTStatement stmt : importStmts) {
						Set<ARTResource> graphs = stmtCollector.getGraphsFor(stmt);

						ARTNode obj = stmt.getObject();

						if (obj.isURIResource()) {
							STRDFURI stUri = STRDFNodeFactory.createSTRDFURI(obj.asURIResource(),
									resource2Role.get(stmt.getObject()),
									graphs.contains(NodeFilters.MAINGRAPH),
									resource2Rendering.get(stmt.getObject()));
							stUri.setInfo("graphs", Joiner.on(",").join(graphs));
							imports.add(stUri);
						}
					}

					importStmts.clear();

					result.put("imports", new NodeListSection(imports));
				}
			}
		}

		return result;
	}

	private void minimizeDomainRanges(List<STRDFNode> typeList) {
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

	private static interface ResourceViewSection {
		void appendToElement(Element parent);
	}

	private static class PredicateObjectsListSection implements ResourceViewSection {

		private PredicateObjectsList predicateObjectsList;

		public PredicateObjectsListSection(PredicateObjectsList predicateObjectsList) {
			this.predicateObjectsList = predicateObjectsList;
		}

		@Override
		public void appendToElement(Element parent) {
			RDFXMLHelp.addPredicateObjectList(parent, predicateObjectsList);
		}
	}

	private static class NodeListSection implements ResourceViewSection {

		private List<STRDFNode> nodeList;

		public NodeListSection(List<STRDFNode> nodeList) {
			this.nodeList = nodeList;
		}

		@Override
		public void appendToElement(Element parent) {
			RDFXMLHelp.addRDFNodes(parent, nodeList);
		}
	}

	private static class PropertyFacets implements ResourceViewSection {

		private boolean symmetric;
		private boolean symmetricExplicit;

		private boolean functional;
		private boolean functionalExplicit;

		private boolean inverseFunctional;
		private boolean inverseFunctionalExplicit;

		private boolean transitive;
		private boolean transitiveExplicit;

		private List<STRDFNode> inverseOf;

		public PropertyFacets(boolean symmetric, boolean symmetricExplicit, boolean functional,
				boolean functionalExplicit, boolean inverseFunctional, boolean inverseFunctionalExplicit,
				boolean transitive, boolean transitiveExplicit, List<STRDFNode> inverseOf) {
			this.symmetric = symmetric;
			this.symmetricExplicit = symmetricExplicit;
			this.functional = functional;
			this.functionalExplicit = functionalExplicit;
			this.inverseFunctional = inverseFunctional;
			this.inverseFunctionalExplicit = inverseFunctionalExplicit;
			this.transitive = transitive;
			this.transitiveExplicit = transitiveExplicit;
			this.inverseOf = inverseOf;
		}

		@Override
		public void appendToElement(Element parent) {
			if (symmetric) {
				Element symmetricElement = XMLHelp.newElement(parent, "symmetric");
				symmetricElement.setAttribute("value", "true");
				symmetricElement.setAttribute("explicit", Boolean.toString(symmetricExplicit));
			}

			if (functional) {
				Element functionalElement = XMLHelp.newElement(parent, "functional");
				functionalElement.setAttribute("value", "true");
				functionalElement.setAttribute("explicit", Boolean.toString(functionalExplicit));
			}

			if (inverseFunctional) {
				Element functionalElement = XMLHelp.newElement(parent, "inverseFunctional");
				functionalElement.setAttribute("value", "true");
				functionalElement.setAttribute("explicit", Boolean.toString(inverseFunctionalExplicit));
			}

			if (transitive) {
				Element functionalElement = XMLHelp.newElement(parent, "transitive");
				functionalElement.setAttribute("value", "true");
				functionalElement.setAttribute("explicit", Boolean.toString(transitiveExplicit));
			}

			Element inverseOfElement = XMLHelp.newElement(parent, "inverseof");
			RDFXMLHelp.addRDFNodes(inverseOfElement, inverseOf);
		}

	}

}
