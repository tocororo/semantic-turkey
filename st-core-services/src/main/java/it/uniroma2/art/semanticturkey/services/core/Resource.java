package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.filter.StatementWithAnyOfGivenSubjects_Predicate;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.LinkedDataResolver;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.constraints.Existing;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.AutoRendering;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.IOException;
import java.net.MalformedURLException;
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
public class Resource extends STServiceAdapter {

	// Temporarily disabled, since we still have not automatic handling of domain objects
	//@GenerateSTServiceController
	@AutoRendering
	public Collection<STRDFNode> getPropertyValues(@Existing ARTResource subject, ARTURIResource predicate)
			throws ModelAccessException {
		OWLModel model = getOWLModel();
		ARTResource[] graphs;
		graphs = getUserNamedGraphs();
		ARTNodeIterator it = model.listValuesOfSubjPredPair(subject, predicate, true, graphs);

		Collection<ARTNode> explicitValues = RDFIterators.getCollectionFromIterator(model
				.listValuesOfSubjPredPair(subject, predicate, false, getWorkingGraph()));

		Collection<STRDFNode> values = STRDFNodeFactory.createEmptyNodeCollection();
		while (it.streamOpen()) {
			ARTNode next = it.getNext();
			boolean explicit;
			if (explicitValues.contains(next))
				explicit = true;
			else
				explicit = false;
			values.add(STRDFNodeFactory.createSTRDFNode(model, next, true, explicit, false)); // disables
																								// rendering
		}
		it.close();

		return values;
	}

	@GenerateSTServiceController
	public Response getResourceDescription(ARTResource resource) throws ModelAccessException,
			UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException,
			UnavailableResourceException, ProjectInconsistentException, MalformedURLException, IOException {
		OWLModel owlModel = getOWLModel();
		ARTResource[] userNamedGraphs = getUserNamedGraphs();
		ARTResource workingGraph = getWorkingGraph();
		
		boolean localResource = isLocalResource(resource);

		Collection<ARTStatement> resourceDescription;
		Set<ARTStatement> explicitStatements;

		//***********************************************************************************
		// Retrieve the given resource description, and, if possibile, the explicit statements
		
		if (localResource) { // local resource
			
			// Description (possibly including implicit triples)
			GraphQuery query = owlModel.createGraphQuery("describe " + RDFNodeSerializer.toNT(resource));
			resourceDescription = RDFIterators.getCollectionFromIterator(query.evaluate(true));

			// Explicit statements (triples belonging to the current working graph).
			// We must use the API, since there is no standard way for mentioning the null context in a SPARQL query
			explicitStatements = RDFIterators.getSetFromIterator(owlModel.listStatements(resource,
					NodeFilters.ANY, NodeFilters.ANY, false, NodeFilters.MAINGRAPH));
	
		} else { // external resource
			
			// if the given resource is not a URI, then throw an exception
			if (!resource.isURIResource()) {
				throw new IllegalArgumentException("The given resource " + RDFNodeSerializer.toNT(resource)
						+ " is remote, and is not a URI. Therefore, it is impossible to dereference.");
			}


			LinkedDataResolver linkedDataResolver = createLinkedDataResolver();

			ARTURIResource uriResource = resource.asURIResource();

			// Description (possibly including inferred triples)
			resourceDescription = linkedDataResolver.lookup(uriResource);
			
			// Unable to distinguish explicit vs implicit statements
			explicitStatements = new HashSet<ARTStatement>(); // All statements are considered implicit
		}
		
		//**********************
		// Selects only outlinks
		Collection<ARTStatement> outlinks = Collections2.filter(resourceDescription, StatementWithAnyOfGivenSubjects_Predicate.getFilter(Arrays.asList(resource)));
		
		// Produces the property-values forms
		HashMap<ARTURIResource, STRDFResource> art2STRDFPredicates = new HashMap<ARTURIResource, STRDFResource>();
		HashMultimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();

		for (ARTStatement stmt : outlinks) {

			ARTURIResource predicate = stmt.getPredicate();
			boolean explicit = false;

			// System.out.println("explicit statements:\n" + explicitStatements);

			if (explicitStatements.contains(stmt)) {
				explicit = true;
			}
			
			resultPredicateObjectValues.put(predicate,
					STRDFNodeFactory.createSTRDFNode(owlModel, stmt.getObject(), false, explicit, false));

			art2STRDFPredicates.put(predicate, STRDFNodeFactory.createSTRDFURI(predicate, RDFResourceRolesEnum.property, false,
					owlModel.getQName(predicate.getURI())));
		}
		
		PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory.createPredicateObjectsList(art2STRDFPredicates, resultPredicateObjectValues);
		
		
		//********************************
		// Produces the OLD-style response
		XMLResponseREPLY response = servletUtilities.createReplyResponse("getResourceDescription", RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		
		Element propertiesElement = XMLHelp.newElement(dataElement, "Properties");
		RDFXMLHelp.addPredicateObjectList(propertiesElement, predicateObjectsList);
		
		return response;
	}

	/**
	 * Tests if the given <code>resource</code> is local or not. A resource is considered local iff it appears
	 * as subject of at least one triple in any of the graphs returned by {@link #getUserNamedGraphs()}.
	 * 
	 * @param resource
	 * @return
	 * @throws ModelAccessException
	 */
	private boolean isLocalResource(ARTResource resource) throws ModelAccessException {
		OWLModel owlModel = getOWLModel();
		ARTResource[] graphs = getUserNamedGraphs();

		return owlModel.hasTriple(resource, NodeFilters.ANY, NodeFilters.ANY, false, graphs);
	}

	/**
	 * Instantiate a resolver of Linked Data based on the RDF technologies used in the project.
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 */
	private LinkedDataResolver createLinkedDataResolver() throws UnavailableResourceException,
			ProjectInconsistentException {
		ModelFactory<?> fact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID())
				.createModelFactory();

		return fact.loadLinkedDataResolver();
	}
}
