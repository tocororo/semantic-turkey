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

import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.filter.StatementWithAnyOfGivenSubjects_Predicate;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.LinkedDataResolver;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.TripleQueryModelHTTPConnection;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
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
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;

@GenerateSTServiceController
@Validated
@Component
public class ResourceView extends STServiceAdapter {

	@GenerateSTServiceController
	public Response getResourceView(ARTResource resource) throws Exception {
		OWLModel owlModel = getOWLModel();
		ARTResource[] userNamedGraphs = getUserNamedGraphs();
		ARTResource workingGraph = getWorkingGraph();

		boolean localResource = owlModel.existsResource(resource, userNamedGraphs);

		// ***********************************************************************************
		// Retrieve the given resource description, and, if possible, the explicit statements

		Collection<ARTStatement> resourceDescription;
		Set<ARTStatement> explicitStatements;

		if (localResource) { // local resource
			// Statements about the resource (both explicit and implicit ones)
			GraphQuery describeQuery = owlModel.createGraphQuery("describe "
					+ RDFNodeSerializer.toNT(resource));
			resourceDescription = RDFIterators.getCollectionFromIterator(describeQuery.evaluate(true));

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

		// **********************
		// Selects only outlinks
		Collection<ARTStatement> outlinks = Collections2.filter(resourceDescription,
				StatementWithAnyOfGivenSubjects_Predicate.getFilter(Arrays.asList(resource)));

		// Produces the property-values forms
		HashMap<ARTURIResource, STRDFResource> art2STRDFPredicates = new HashMap<ARTURIResource, STRDFResource>();
		HashMultimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

		for (ARTStatement stmt : outlinks) {

			ARTURIResource predicate = stmt.getPredicate();
			boolean explicit = false;

			if (explicitStatements.contains(stmt)) {
				explicit = true;
			}

			resultPredicateObjectValues.put(predicate,
					STRDFNodeFactory.createSTRDFNode(owlModel, stmt.getObject(), false, explicit, false));

			art2STRDFPredicates.put(
					predicate,
					STRDFNodeFactory.createSTRDFURI(predicate, RDFResourceRolesEnum.property, false,
							owlModel.getQName(predicate.getURI())));
		}

		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory.createPredicateObjectsList(
				art2STRDFPredicates, resultPredicateObjectValues);

		// ********************************
		// Produces the OLD-style response
		XMLResponseREPLY response = servletUtilities.createReplyResponse("getResourceDescription",
				RepliesStatus.ok);
		 Element dataElement = response.getDataElement();
		
		 Element propertiesElement = XMLHelp.newElement(dataElement, "Properties");
		 RDFXMLHelp.addPredicateObjectList(propertiesElement, predicateObjectsList);

		return response;
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

			Collection<ARTStatement> resourceDescription = RDFIterators
					.getCollectionFromIterator(describeQuery.evaluate(true));
			queryConnection.disconnect();

			return resourceDescription;
		}

	}

	private ModelFactory<?> getCurrentModelFactory() throws UnavailableResourceException,
			ProjectInconsistentException {
		return PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
	}

}
