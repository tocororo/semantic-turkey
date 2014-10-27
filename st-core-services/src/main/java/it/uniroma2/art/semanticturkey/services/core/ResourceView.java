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
import it.uniroma2.art.owlart.filter.StatementWithAnyOfGivenComponents_Predicate;
import it.uniroma2.art.owlart.filter.StatementWithAnyOfGivenSubjects_Predicate;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTNamespace;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.LinkedDataResolver;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.TripleQueryModel;
import it.uniroma2.art.owlart.models.TripleQueryModelHTTPConnection;
import it.uniroma2.art.owlart.navigation.ARTNamespaceIterator;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.utilities.PropertyChainsTree;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
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
import it.uniroma2.art.semanticturkey.rendering.RenderingOrchestrator;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;

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

		boolean localResource = isLocalResource(owlModel, resource);

		boolean subjectEditability = localResource; // TODO: implement the right check

		// ******************************************************************************************
		// Step 1: Retrieve the given resource description, and, if possible, the explicit statements

		Collection<ARTStatement> resourceDescription;
		Set<ARTStatement> explicitStatements;

		if (localResource) { // local resource
			// Statements about the resource (both explicit and implicit ones)
//			if (resource.isURIResource()) {
//				GraphQuery describeQuery = owlModel.createGraphQuery("describe " + RDFNodeSerializer.toNT(resource));
//				describeQuery.setBinding("resource", resource);
//				resourceDescription = RDFIterators.getSetFromIterator(describeQuery.evaluate(true));
//			} else {
//				resourceDescription = ModelUtilities.createCustomCBD(owlModel, resource, true, new PropertyChainsTree());
//			}
			
			GraphQuery describeQuery = owlModel.createGraphQuery("describe ?x where {bind(?resource as ?x)}");
			describeQuery.setBinding("resource", resource);
			resourceDescription = RDFIterators.getSetFromIterator(describeQuery.evaluate(true));
			
			System.out.println(resourceDescription);
			
			// Explicit statements (triples belonging to the current working graph).
			// We must use the API, since there is no standard way for mentioning the null context in a SPARQL
			// query
			explicitStatements = RDFIterators.getSetFromIterator(owlModel.listStatements(resource,
					NodeFilters.ANY, NodeFilters.ANY, false, NodeFilters.MAINGRAPH));

		} else { // external resource
			resourceDescription = remoteLookup(resource);

			// Unable to distinguish explicit vs implicit statements
			explicitStatements = new HashSet<ARTStatement>(); // All statements are considered implicit
		}

		// **************************
		// Step X: Render the subject

		RenderingEngine renderingOrchestrator = RenderingOrchestrator.getInstance();
		Map<ARTResource, String> artResource2Rendering = renderingOrchestrator.render(getProject(), resource,
				resourceDescription, resource);

		STRDFResource subjectResource = STRDFNodeFactory.createSTRDFResource(owlModel, resource, false,
				subjectEditability, false);
		subjectResource.setRendering(artResource2Rendering.get(resource));

		// *************************************
		// Step X: Remove linguistic information

		// Produces the property-values forms
		HashMap<ARTURIResource, STRDFResource> art2STRDFPredicatesLabels = new HashMap<ARTURIResource, STRDFResource>();
		HashMultimap<ARTURIResource, STRDFNode> resultPredicateObjectValuesLabels = HashMultimap.create();

		// Filter rdfs:labels

		Collection<ARTStatement> rdfsLabelStmts = Collections2.filter(resourceDescription,
				StatementWithAnyOfGivenComponents_Predicate.getFilter(resource, RDFS.Res.LABEL,
						NodeFilters.ANY));

		STRDFURI rdfsLabelRes = STRDFNodeFactory.createSTRDFURI(RDFS.Res.LABEL,
				RDFResourceRolesEnum.annotationProperty, false, "rdfs:label");
		art2STRDFPredicatesLabels.put(RDFS // TODO
				.Res.LABEL, rdfsLabelRes);

		for (ARTStatement stmt : rdfsLabelStmts) {

			boolean explicit = false;

			if (explicitStatements.contains(stmt)) {
				explicit = true;
			}

			STRDFNode labelNode = STRDFNodeFactory.createSTRDFNode(owlModel, stmt.getObject(), false,
					explicit, false);
			if (stmt.getObject().isLiteral()) {
				labelNode.setRendering(stmt.getObject().getNominalValue());
			}
			resultPredicateObjectValuesLabels.put(RDFS.Res.LABEL, labelNode);

		}

		// Remove processed statements
		rdfsLabelStmts.clear();

		// Filter skos:{pref,alt,hidden}Label

		@SuppressWarnings("unchecked")
		Collection<ARTStatement> skosLabelStmts = Collections2.filter(resourceDescription, Predicates.or(
				StatementWithAnyOfGivenComponents_Predicate.getFilter(resource, SKOS.Res.PREFLABEL,
						NodeFilters.ANY), StatementWithAnyOfGivenComponents_Predicate.getFilter(resource,
						SKOS.Res.ALTLABEL, NodeFilters.ANY), StatementWithAnyOfGivenComponents_Predicate
						.getFilter(resource, SKOS.Res.HIDDENLABEL, NodeFilters.ANY)));

		STRDFURI skosPrefLabelRes = STRDFNodeFactory.createSTRDFURI(SKOS.Res.PREFLABEL,
				RDFResourceRolesEnum.annotationProperty, false, "skos:prefLabel");
		art2STRDFPredicatesLabels.put(SKOS.Res.PREFLABEL, skosPrefLabelRes);

		STRDFURI skosAltLabelRes = STRDFNodeFactory.createSTRDFURI(SKOS.Res.ALTLABEL,
				RDFResourceRolesEnum.annotationProperty, false, "skos:altLabel");
		art2STRDFPredicatesLabels.put(SKOS.Res.ALTLABEL, skosAltLabelRes);

		STRDFURI skosHiddenLabelRes = STRDFNodeFactory.createSTRDFURI(SKOS.Res.HIDDENLABEL,
				RDFResourceRolesEnum.annotationProperty, false, "skos:hiddenLabel");
		art2STRDFPredicatesLabels.put(SKOS.Res.HIDDENLABEL, skosHiddenLabelRes);

		for (ARTStatement stmt : skosLabelStmts) {

			boolean explicit = false;

			if (explicitStatements.contains(stmt)) {
				explicit = true;
			}

			STRDFNode labelNode = STRDFNodeFactory.createSTRDFNode(owlModel, stmt.getObject(), false,
					explicit, false);
			if (stmt.getObject().isLiteral()) {
				labelNode.setRendering(stmt.getObject().getNominalValue());
			}
			resultPredicateObjectValuesLabels.put(stmt.getPredicate(), labelNode);

		}

		// Remove processed statements
		skosLabelStmts.clear();

		// Filter skosxl:{pref,alt,hidden}Label

		@SuppressWarnings("unchecked")
		Collection<ARTStatement> skosxlLabelStmts = Collections2.filter(resourceDescription, Predicates.or(
				StatementWithAnyOfGivenComponents_Predicate.getFilter(resource, SKOSXL.Res.PREFLABEL,
						NodeFilters.ANY), StatementWithAnyOfGivenComponents_Predicate.getFilter(resource,
						SKOSXL.Res.ALTLABEL, NodeFilters.ANY), StatementWithAnyOfGivenComponents_Predicate
						.getFilter(resource, SKOSXL.Res.HIDDENLABEL, NodeFilters.ANY)));

		STRDFURI skosxlPrefLabelRes = STRDFNodeFactory.createSTRDFURI(SKOSXL.Res.PREFLABEL,
				RDFResourceRolesEnum.objectProperty, false, "skosxl:prefLabel");
		art2STRDFPredicatesLabels.put(SKOSXL.Res.PREFLABEL, skosxlPrefLabelRes);

		STRDFURI skosxlAltLabelRes = STRDFNodeFactory.createSTRDFURI(SKOSXL.Res.ALTLABEL,
				RDFResourceRolesEnum.objectProperty, false, "skosxl:altLabel");
		art2STRDFPredicatesLabels.put(SKOSXL.Res.ALTLABEL, skosxlAltLabelRes);

		STRDFURI skosxlHiddenLabelRes = STRDFNodeFactory.createSTRDFURI(SKOSXL.Res.HIDDENLABEL,
				RDFResourceRolesEnum.objectProperty, false, "skosxl:hiddenLabel");
		art2STRDFPredicatesLabels.put(SKOSXL.Res.HIDDENLABEL, skosxlHiddenLabelRes);

		Map<ARTURIResource, STRDFURI> art2stxlabels = new HashMap<ARTURIResource, STRDFURI>();

		for (ARTStatement stmt : skosxlLabelStmts) {

			boolean explicit = false;

			if (explicitStatements.contains(stmt)) {
				explicit = true;
			}

			STRDFNode xLabelNode = STRDFNodeFactory.createSTRDFNode(owlModel, stmt.getObject(), false,
					explicit, false);

			if (xLabelNode.isURIResource()) {
				STRDFURI xLabelUri = (STRDFURI) xLabelNode;
				xLabelUri.setRole(RDFResourceRolesEnum.xLabel);

				// Retain xlabels for computing their rendering
				art2stxlabels.put(stmt.getObject().asURIResource(), xLabelUri);
			}
			resultPredicateObjectValuesLabels.put(stmt.getPredicate(), xLabelNode);

		}

		renderXLabels(resource, localResource, art2stxlabels);

		// Remove processed statements
		skosxlLabelStmts.clear();

		// Create labels predicateObjectLists
		PredicateObjectsList labellingPredicateObjectsList = PredicateObjectsListFactory
				.createPredicateObjectsList(art2STRDFPredicatesLabels, resultPredicateObjectValuesLabels);

		// *********************************
		// Step X: Process remaining triples

		Collection<ARTStatement> outlinks = Collections2.filter(resourceDescription,
				StatementWithAnyOfGivenSubjects_Predicate.getFilter(Arrays.<ARTResource> asList(resource)));

		// ****************************
		// Step X: Render the resources

		Set<ARTResource> resourcesToBeRendered = new HashSet<ARTResource>();
		resourcesToBeRendered.addAll(RDFIterators.getSetFromIterator(RDFIterators
				.filterResources(RDFIterators.listObjects(RDFIterators.createARTStatementIterator(outlinks
						.iterator())))));
		artResource2Rendering = renderingOrchestrator.render(getProject(), resource, resourceDescription,
				resourcesToBeRendered.toArray(new ARTResource[resourcesToBeRendered.size()]));

		// **************************
		// Step X: Computes the roles

		Set<ARTResource> resourcesForComputingRole = new HashSet<ARTResource>(resourcesToBeRendered);
		resourcesForComputingRole.add(resource);
		
		Map<ARTResource, RDFResourceRolesEnum> artResource2Role = RoleRecognitionOrchestrator.getInstance()
				.computeRoleOf(getProject(), resource, resourceDescription,
						resourcesForComputingRole.toArray(new ARTResource[resourcesForComputingRole.size()]));
		
		subjectResource.setRole(artResource2Role.get(subjectResource.getARTNode().asResource()));
		
		// ************************************************
		// Step X: Prepare data for predicate objects lists

		HashMap<ARTURIResource, STRDFResource> art2STRDFPredicates = new HashMap<ARTURIResource, STRDFResource>();
		HashMultimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

		for (ARTStatement stmt : outlinks) {

			ARTURIResource predicate = stmt.getPredicate();
			boolean explicit = false;

			if (explicitStatements.contains(stmt)) {
				explicit = true;
			}

			STRDFNode stNodeObject = STRDFNodeFactory.createSTRDFNode(owlModel, stmt.getObject(), false,
					explicit, false);

			if (stNodeObject.isResource()) {
				STRDFResource stResourceObject = ((STRDFResource) stNodeObject);
				stResourceObject.setRendering(artResource2Rendering.get(stResourceObject.getARTNode()
						.asResource()));
				stResourceObject.setRole(artResource2Role.get(stResourceObject.getARTNode().asResource()));
			}

			resultPredicateObjectValues.put(predicate, stNodeObject);

			if (!art2STRDFPredicates.containsKey(predicate)) {
				art2STRDFPredicates.put(predicate, STRDFNodeFactory.createSTRDFURI(predicate,
						RDFResourceRolesEnum.property, false, owlModel.getQName(predicate.getURI())));
			}
		}

		// ******************************
		// Step X: Reorganize information

		// Produces the property-values forms

		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory.createPredicateObjectsList(
				art2STRDFPredicates, resultPredicateObjectValues);

		// ****************************************
		// Step X : Produces the OLD-style response
		XMLResponseREPLY response = servletUtilities.createReplyResponse("getResourceDescription",
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		// Append the subject resource

		Element resourceElement = XMLHelp.newElement(dataElement, "resource");
		RDFXMLHelp.addRDFNode(resourceElement, subjectResource);

		// Labeling properties

		Element lexicalizationsElement = XMLHelp.newElement(dataElement, "lexicalizations");
		RDFXMLHelp.addPredicateObjectList(lexicalizationsElement, labellingPredicateObjectsList);

		// Append the other properties

		Element propertiesElement = XMLHelp.newElement(dataElement, "properties");
		RDFXMLHelp.addPredicateObjectList(propertiesElement, predicateObjectsList);

		return response;
	}

	private void renderXLabels(ARTResource resource, boolean localResource,
			Map<ARTURIResource, STRDFURI> art2stxlabels) throws ModelAccessException,
			UnsupportedQueryLanguageException, MalformedQueryException, ModelCreationException,
			UnavailableResourceException, ProjectInconsistentException, QueryEvaluationException {

		OWLModel owlModel = getOWLModel();

		HashMultimap<DatasetMetadata, ARTURIResource> dataset2resources = HashMultimap.create();

		for (ARTURIResource xl : art2stxlabels.keySet()) {
			if (isLocalResource(getOWLModel(), xl)) {
				dataset2resources.put(null, xl);
			} else {
				DatasetMetadata datasetMeta = DatasetMetadataRepository.getInstance().findDatasetForResource(
						xl);

				if (datasetMeta != null && datasetMeta.getSparqlEndpoint() != null) {
					dataset2resources.put(datasetMeta, xl);
				}
			}
		}

		for (DatasetMetadata datasetMeta : dataset2resources.keySet()) {
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("select ?resource ?lexicalForm ?languageTag {\n");
			queryBuilder.append("   values(?resource){\n");

			for (ARTURIResource xl : dataset2resources.get(datasetMeta)) {
				queryBuilder.append("      (").append(RDFNodeSerializer.toNT(xl)).append(")\n");
			}

			queryBuilder.append("   }\n");
			queryBuilder
					.append("   ?resource <http://www.w3.org/2008/05/skos-xl#literalForm> ?literalForm . \n");
			queryBuilder.append("   bind(str(?literalForm) as ?lexicalForm) \n");
			queryBuilder.append("   bind(lang(?literalForm) as ?languageTag) \n");
			queryBuilder.append("}");

			String querySpec = queryBuilder.toString();
			TripleQueryModel queryModel;

			boolean mustCloseConnection = false;

			logger.info(querySpec);

			if (datasetMeta == null) {
				queryModel = owlModel;
			} else {
				queryModel = getCurrentModelFactory().loadTripleQueryHTTPConnection(
						datasetMeta.getSparqlEndpoint());
				mustCloseConnection = true;
			}

			logger.info("Query Model: " + queryModel);

			TupleQuery literalFormQuery = queryModel.createTupleQuery(QueryLanguage.SPARQL, querySpec, null);
			TupleBindingsIterator bindingsIt = literalFormQuery.evaluate(true);

			while (bindingsIt.streamOpen()) {
				TupleBindings bindings = bindingsIt.getNext();

				ARTURIResource anXlabel = bindings.getBoundValue("resource").asURIResource();
				String lexicalForm = bindings.getBoundValue("lexicalForm").asLiteral().getLabel();
				String languageTag = bindings.getBoundValue("languageTag").asLiteral().getLabel();

				logger.info("Bindings: " + bindings);

				STRDFURI stXLabel = art2stxlabels.get(anXlabel);
				if (stXLabel != null) {
					stXLabel.setRendering(RDFNodeSerializer.toNT(owlModel.createLiteral(lexicalForm,
							languageTag)));
				}
			}

			bindingsIt.close();

			if (mustCloseConnection) {
				((TripleQueryModelHTTPConnection) queryModel).disconnect();
			}
		}
	}

	private Collection<ARTStatement> remoteLookup(ARTResource resource) throws Exception {
		if (!resource.isURIResource()) {
			throw new Exception("Cannot lookup the resource: " + RDFNodeSerializer.toNT(resource)
					+ "\nNot a URI resource");
		}

		ARTURIResource uriResource = resource.asURIResource();

		DatasetMetadata datasetMetadata = DatasetMetadataRepository.getInstance().findDatasetForResource(
				uriResource);

		if (datasetMetadata == null || datasetMetadata.isDereferenceable()) {
			LinkedDataResolver ldResolver = getCurrentModelFactory().loadLinkedDataResolver();
			return ldResolver.lookup(uriResource);
		} else {
			TripleQueryModelHTTPConnection queryConnection = getCurrentModelFactory()
					.loadTripleQueryHTTPConnection(datasetMetadata.getSparqlEndpoint());
			GraphQuery describeQuery = queryConnection.createGraphQuery(QueryLanguage.SPARQL, "describe "
					+ RDFNodeSerializer.toNT(uriResource), null);

			Collection<ARTStatement> resourceDescription = RDFIterators.getSetFromIterator(describeQuery
					.evaluate(true));
			queryConnection.disconnect();

			return resourceDescription;
		}

	}

	private ModelFactory<?> getCurrentModelFactory() throws UnavailableResourceException,
			ProjectInconsistentException {
		return PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
	}

	// TODO: find better name??
	private boolean isLocalResource(RDFModel rdfModel, ARTResource resource)
			throws ModelAccessException {
		
		if (resource.isBlank()) {
			return true;
		}
		
		ARTURIResource uriResource = resource.asURIResource();
		
		ARTNamespaceIterator it = rdfModel.listNamespaces();

		try {
			while (it.streamOpen()) {
				ARTNamespace ns = it.getNext();

				if (ns.getName().equals(uriResource.getNamespace())) {
					return true;
				}
			}
		} finally {
			it.close();
		}

		return false;
	}

}
