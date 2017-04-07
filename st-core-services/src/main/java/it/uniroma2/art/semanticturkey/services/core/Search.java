package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.owlart.vocabulary.XmlSchema;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

@GenerateSTServiceController
@Validated
@Component
public class Search extends STServiceAdapterOLD {

	protected static Logger logger = LoggerFactory.getLogger(Search.class);
	
//	private static String CLASS_ROLE = "class";
//	private static String CONCEPT_ROLE = "concept";
//	private static String INSTANCE_ROLE = "instance";
	
	private static String START_SEARCH_MODE = "start";
	private static String CONTAINS_SEARCH_MODE = "contain";
	private static String END_SEARCH_MODE = "end";
	private static String EXACT_SEARCH_MODE = "exact";
	
	@GenerateSTServiceController
	public Response searchResource(String searchString, String [] rolesArray, boolean useLocalName, boolean useURI,
			String searchMode, @Optional String lang, @Optional ARTURIResource scheme) throws ModelUpdateException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException, QueryEvaluationException {
		
		boolean isClassWanted = false;
		boolean isConceptWanted = false;
		boolean isConceptSchemeWanted = false;
		boolean isInstanceWanted = false;
		boolean isPropertyWanted = false;
		boolean isCollectionWanted = false;
		
		String searchModeSelected = null;
		
		
		
		
		if(searchString.isEmpty()){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			response.setReplyMessage("the serchString cannot be empty");
			return response;
		}
		
		for(int i=0; i<rolesArray.length; ++i){
			if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.cls.name())){
				isClassWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.concept.name().toLowerCase())){
				isConceptWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.conceptScheme.name().toLowerCase())){
				isConceptSchemeWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.individual.name().toLowerCase())){
				isInstanceWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.property.name().toLowerCase())){
				isPropertyWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.skosCollection.name().toLowerCase())){
				isCollectionWanted = true;
			} 
		}
		//@formatter:off
		if(!isClassWanted && !isConceptWanted && !isConceptSchemeWanted && 
				!isInstanceWanted && !isPropertyWanted && !isCollectionWanted){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			response.setReplyMessage("the serch roles should be at least one of: "+
					RDFResourceRolesEnum.cls.name()+", "+
					RDFResourceRolesEnum.concept.name()+", "+
					RDFResourceRolesEnum.conceptScheme.name()+", "+
					RDFResourceRolesEnum.individual+", "+
					RDFResourceRolesEnum.property.name() +" or "+
					RDFResourceRolesEnum.skosCollection.name());
			return response;
		}
		//@formatter:on
		
		if(searchMode.toLowerCase().contains(START_SEARCH_MODE)){
			searchModeSelected = START_SEARCH_MODE;
		} else if(searchMode.toLowerCase().contains(CONTAINS_SEARCH_MODE)){
			searchModeSelected = CONTAINS_SEARCH_MODE;
		} else if(searchMode.toLowerCase().contains(END_SEARCH_MODE)){
			searchModeSelected = END_SEARCH_MODE;
		} else if(searchMode.toLowerCase().contains(EXACT_SEARCH_MODE)){
			searchModeSelected = EXACT_SEARCH_MODE;
		}
		
		if(searchModeSelected == null){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			response.setReplyMessage("the serch mode should be at one of: "+START_SEARCH_MODE+", "+
			CONTAINS_SEARCH_MODE+", "+END_SEARCH_MODE+" or "+EXACT_SEARCH_MODE);
			return response;
		}
		
		//TODO verify that in a SKOS it is possible to use a owl model
		OWLModel owlModel = getOWLModel();
		
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
			"\nWHERE{" +
			"\n{";
		//do a subquery to get the candidate resources
		query+="\nSELECT DISTINCT ?resource ?type" +
			"\nWHERE{" ;
		
		//Old version, prior to adding the otional scheme
		/*query+="\n?resource a ?type ."+
				addFilterForRsourseType("?type", isClassWanted, isInstanceWanted, isPropertyWanted, 
						isConceptWanted);*/
		
		query+=filterResourceTypeAndScheme("?resource", "?type", isClassWanted, isInstanceWanted, 
				isPropertyWanted, isConceptWanted, isConceptSchemeWanted, isCollectionWanted, scheme);
		
		
		query+="\n}" +
			"\n}";
			
		//now examine the rdf:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\nBIND(str(?resource) AS ?complURI)"+
					searchModePrepareQuery("?complURI", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchModePrepareQuery("?rdfsLabel", searchString, searchModeSelected) +
				"\n}";

		//if you are searching among concepts or collections, search in the skos:prefLabel/altLabel and 
		// skosxl:prefLabel/altLabel
		
		if(isConceptWanted || isConceptSchemeWanted || isCollectionWanted ){
			query+="\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOS.PREFLABEL+"> | <"+SKOS.ALTLABEL+">) ?skosLabel ."+
					searchModePrepareQuery("?skosLabel", searchString, searchModeSelected) +
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOSXL.PREFLABEL+"> | <"+SKOSXL.ALTLABEL+">) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERALFORM+"> ?literalForm ." +
					searchModePrepareQuery("?literalForm", searchString, searchModeSelected) +
					"\n}";
		}
		
		//if language was specified, try to take first the literalForm for that language and then,
		// if it has not such value, the skosLabel (always considering just the prefLabel)
		// the language is used only to return the concept/collection with the labels in the desired 
		// language and it is not used to filter a resource
		if((isConceptWanted || isConceptSchemeWanted || isCollectionWanted)&& lang!=null && lang.length()>0){
			query+="\nOPTIONAL" +
					"\n{" +
					"\n?resource <"+SKOSXL.PREFLABEL+"> ?skosPrefLabel ." +
					"\n?skosPrefLabel <"+SKOSXL.LITERALFORM+"> ?show ." +
					"\nFILTER(lang(?show) = \""+lang+"\")"+
					"\n}" +
					"\nOPTIONAL" +
					"\n{" +
					"\n?resource <"+SKOS.PREFLABEL+"> ?show ." +
					"\nFILTER(lang(?show) = \""+lang+"\")"+
					"\n}";
		}
		
		query+="\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		
		TupleQuery tupleQuery;
		tupleQuery = owlModel.createTupleQuery(query);
		TupleBindingsIterator tupleBindingsIterator = tupleQuery.evaluate(false);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Collection<STRDFURI> collection = STRDFNodeFactory.createEmptyURICollection();
		
		Map<String, ValueTypeAndShow> propertyMap = new HashMap<String, ValueTypeAndShow>();
		Map<String, ValueTypeAndShow> otherResourcesMap = new HashMap<String, ValueTypeAndShow>();
		
		
		while (tupleBindingsIterator.hasNext()) {
			TupleBindings tupleBindings = tupleBindingsIterator.next();
			ARTNode resourceURI = tupleBindings.getBinding("resource").getBoundValue();

			if (!resourceURI.isURIResource()) {
				continue;
			}

			// TODO, explicit set to true
			RDFResourceRolesEnum role = null;
			//since there are more than one element in the input role array, see the resource
			String type = tupleBindings.getBinding("type").getBoundValue().getNominalValue();
			
			role = getRoleFromType(type);
			
			if(role.equals(RDFResourceRolesEnum.cls)){
				if(otherResourcesMap.containsKey(resourceURI.asURIResource().getNominalValue())){
					//the class was already added
					continue;
				}
				//remove all the classes which belongs to xml/rdf/rdfs/owl to exclude from the results those
				// classes which are not visible in the class tree (as it is done in #ClsOld.getSubClasses since 
				// when the parent class is Owl:Thing the service filters out those classes with 
				// NoLanguageResourcePredicate)
				if(role.equals(RDFResourceRolesEnum.cls)){
					String resNamespace = resourceURI.asURIResource().getNamespace();
					if(resNamespace.equals(XmlSchema.NAMESPACE) || resNamespace.equals(RDF.NAMESPACE) 
							|| resNamespace.equals(RDFS.NAMESPACE) || resNamespace.equals(OWL.NAMESPACE) ){
						continue;
					}
				}
				String show = null;
				if(tupleBindings.hasBinding("show")){
					show = tupleBindings.getBinding("show").getBoundValue().getNominalValue();
				}
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow(resourceURI.asURIResource(), show, role);
				otherResourcesMap.put(resourceURI.asURIResource().getNominalValue(), valueTypeAndShow);
			} else if(role.equals(RDFResourceRolesEnum.individual)){
				//there a special section for the individual, since an individual can belong to more than a
				// class, so the result set could have more tuple regarding a single individual, this way
				// should speed up the process
				if(otherResourcesMap.containsKey(resourceURI.asURIResource().getNominalValue())){
					//the individual was already added
					continue;
				}
				String show = null;
				if(tupleBindings.hasBinding("show")){
					show = tupleBindings.getBinding("show").getBoundValue().getNominalValue();
				}
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow(resourceURI.asURIResource(), show, role);
				otherResourcesMap.put(resourceURI.asURIResource().getNominalValue(), valueTypeAndShow);
			} else if(role.equals(RDFResourceRolesEnum.property) || 
					role.equals(RDFResourceRolesEnum.annotationProperty) || 
					role.equals(RDFResourceRolesEnum.datatypeProperty) || 
					role.equals(RDFResourceRolesEnum.objectProperty) || 
					role.equals(RDFResourceRolesEnum.ontologyProperty) ) {
				//check if the property was already added before (with a different type)
				if(propertyMap.containsKey(resourceURI.asURIResource().getNominalValue())){
					ValueTypeAndShow prevValueTypeAndShow = propertyMap.get(resourceURI.asURIResource().getNominalValue());
					if(prevValueTypeAndShow.getRole().equals(RDFResourceRolesEnum.property)){
						//the previous value was property, now it has a different role, so replace the old 
						// one with the new one
						String show = null;
						if(tupleBindings.hasBinding("show")){
							show = tupleBindings.getBinding("show").getBoundValue().getNominalValue();
						}
						ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow(resourceURI.asURIResource(), show, role);
						propertyMap.put(resourceURI.asURIResource().getNominalValue(), valueTypeAndShow);
					}
				} else{
					//the property map did not have a previous value, so add this one without any checking
					String show = null;
					if(tupleBindings.hasBinding("show")){
						show = tupleBindings.getBinding("show").getBoundValue().getNominalValue();
					}
					ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow(resourceURI.asURIResource(), show, role);
					propertyMap.put(resourceURI.asURIResource().getNominalValue(), valueTypeAndShow);
				}
			} else{
				//it is a concept, a conceptScheme or a collection, just add it to the otherMap
				String show = null;
				if(tupleBindings.hasBinding("show")){
					show = tupleBindings.getBinding("show").getBoundValue().getNominalValue();
				}
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow(resourceURI.asURIResource(), show, role);
				otherResourcesMap.put(resourceURI.asURIResource().getNominalValue(), valueTypeAndShow);
			}
		}
		
		//now iterate over the 2 maps and construct the responses
		for(String key : otherResourcesMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = otherResourcesMap.get(key);
			if(valueTypeAndShow.isShowPresent() || !valueTypeAndShow.getShow().isEmpty()){
				collection.add(STRDFNodeFactory.createSTRDFURI(valueTypeAndShow.getResource().asURIResource(),
						valueTypeAndShow.getRole(), true, valueTypeAndShow.getShow()));
			} else{
				collection.add(STRDFNodeFactory.createSTRDFURI(owlModel, 
						valueTypeAndShow.getResource().asURIResource(), valueTypeAndShow.getRole(), 
						true, true));
			}
		}
		for(String key : propertyMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = propertyMap.get(key);
			if(valueTypeAndShow.isShowPresent() || !valueTypeAndShow.getShow().isEmpty()){
				collection.add(STRDFNodeFactory.createSTRDFURI(valueTypeAndShow.getResource().asURIResource(),
						valueTypeAndShow.getRole(), true, valueTypeAndShow.getShow()));
			} else{
				collection.add(STRDFNodeFactory.createSTRDFURI(owlModel, 
						valueTypeAndShow.getResource().asURIResource(), valueTypeAndShow.getRole(), 
						true, true));
			}
		}
		
		RDFXMLHelp.addRDFNodes(dataElement, collection);

		return response;
	}
	
	
	private class ValueTypeAndShow{
		ARTResource resource  = null;
		String show = null;
		RDFResourceRolesEnum role = null;
		
		public ValueTypeAndShow(ARTResource resource, String show, RDFResourceRolesEnum role) {
			this.resource = resource;
			this.show = show;
			this.role = role;
		}

		public ARTResource getResource() {
			return resource;
		}

		public String getShow() {
			return show;
		}

		public RDFResourceRolesEnum getRole() {
			return role;
		}
		
		public boolean isShowPresent(){
			if(show != null){
				return true;
			}
			return false;
		}
		
	}
	
	
	@GenerateSTServiceController
	public Response searchInstancesOfClass(ARTURIResource cls, String searchString, boolean useLocalName, 
			boolean useURI, String searchMode, @Optional String lang) 
					throws ModelUpdateException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException, QueryEvaluationException {
		
		String searchModeSelected = null;
		
		
		if(searchString.isEmpty()){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			response.setReplyMessage("the serchString cannot be empty");
			return response;
		}

		if(searchMode.toLowerCase().contains(START_SEARCH_MODE)){
			searchModeSelected = START_SEARCH_MODE;
		} else if(searchMode.toLowerCase().contains(CONTAINS_SEARCH_MODE)){
			searchModeSelected = CONTAINS_SEARCH_MODE;
		} else if(searchMode.toLowerCase().contains(END_SEARCH_MODE)){
			searchModeSelected = END_SEARCH_MODE;
		} else if(searchMode.toLowerCase().contains(EXACT_SEARCH_MODE)){
			searchModeSelected = EXACT_SEARCH_MODE;
		}
		
		if(searchModeSelected == null){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			response.setReplyMessage("the serch mode should be at one of: "+START_SEARCH_MODE+", "+
			CONTAINS_SEARCH_MODE+", "+END_SEARCH_MODE+" or "+EXACT_SEARCH_MODE);
			return response;
		}
		
		OWLModel owlModel = getOWLModel();
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
			"\nWHERE{" +
			"\n{";
		//do a subquery to get the candidate resources
		query+="\nSELECT DISTINCT ?resource" +
			"\nWHERE{" + 
			"\n ?resource a <"+cls.getNominalValue()+"> . " +
			"\n}" +
			"\n}";
			
		//now examine the rdf:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\nBIND(str(?resource) AS ?complURI)"+
					searchModePrepareQuery("?complURI", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchModePrepareQuery("?rdfsLabel", searchString, searchModeSelected) +
				"\n}";

		
		query+="\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		
		TupleQuery tupleQuery;
		tupleQuery = owlModel.createTupleQuery(query);
		TupleBindingsIterator tupleBindingsIterator = tupleQuery.evaluate(false);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		// Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		Collection<STRDFURI> collection = STRDFNodeFactory.createEmptyURICollection();
		List<String> addedIndividualList = new ArrayList<String>();
		List<String> addedClassList = new ArrayList<String>();
		while (tupleBindingsIterator.hasNext()) {
			TupleBindings tupleBindings = tupleBindingsIterator.next();
			ARTNode resourceURI = tupleBindings.getBinding("resource").getBoundValue();

			if (!resourceURI.isURIResource()) {
				continue;
			}

			RDFResourceRolesEnum role = getRoleFromType(cls.getURI());
			if(addedIndividualList.contains(resourceURI.asURIResource().getNominalValue())){
				//the individual was already added
				continue;
			}
			addedIndividualList.add(resourceURI.asURIResource().getNominalValue());
			
			
			
			
			//if a show was found, use it instead of the automatically retrieve one (the qname)
			String show = null;
			if(tupleBindings.hasBinding("show")){
				show = tupleBindings.getBinding("show").getBoundValue().getNominalValue();
				if(show.length()>0){
					collection.add(STRDFNodeFactory.createSTRDFURI(resourceURI.asURIResource(), role, 
							true, show));
				}
			}
			else{
				collection.add(STRDFNodeFactory.createSTRDFURI(owlModel, resourceURI.asURIResource(), role, 
						true, true));
			}
			
		}
		RDFXMLHelp.addRDFNodes(dataElement, collection);

		return response;
	}

	@GenerateSTServiceController
	public Response getPathFromRoot(String role, String resourceURI, @Optional String schemeURI)
			throws InvalidParameterException{
		
		OWLModel owlModel = getOWLModel();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		ARTURIResource inputResource = owlModel.createURIResource(resourceURI);
		
		try {
			
			String query = null;
			String superResourceVar = null, superSuperResourceVar = null;
			if(role.toLowerCase().equals(RDFResourceRolesEnum.concept.name().toLowerCase())){
				superResourceVar = "broader";
				superSuperResourceVar = "broaderOfBroader";
				//@formatter:off
				query = "SELECT DISTINCT ?broader ?broaderOfBroader ?isTopConcept ?isTop" + 
						"\nWHERE{" +
						"\n{" + 
						"\n<" + resourceURI + "> (<" + SKOS.BROADER + "> | ^<"+SKOS.NARROWER+"> )+ ?broader .";
				if (schemeURI != null) {
					query += "\n?broader <" + SKOS.INSCHEME + "> <" + schemeURI + "> ."+
							"\nOPTIONAL{" +
							"\nBIND (\"true\" AS ?isTopConcept)" +
							"\n?broader (<"+SKOS.TOPCONCEPTOF+"> | ^<"+SKOS.HASTOPCONCEPT+">) <"+schemeURI+"> ." +
							"\n}";
				}
				query += "\nOPTIONAL{" +
						"\n?broader (<" + SKOS.BROADER + "> | ^<"+SKOS.NARROWER+">) ?broaderOfBroader .";
				if (schemeURI != null) {
					query += "\n?broaderOfBroader <" + SKOS.INSCHEME + "> <" + schemeURI + "> . ";
				}
				query +="\n}" + 
						"\n}" +
						"\nUNION" +
						"\n{";
				//this union is used when the first part does not return anything, so when the desired concept
				// does not have any broader, but it is defined as topConcept (to either a specified scheme or
				// to at least one)
				query+= "\n<" + resourceURI + "> a <"+SKOS.CONCEPT+"> .";
				if(schemeURI != null){
						query+="\n<"+resourceURI+"> " +
								"(<"+SKOS.TOPCONCEPTOF+"> | ^<"+SKOS.HASTOPCONCEPT+">) <"+schemeURI+">";
				} else{
					query+="\n<"+resourceURI+"> " +
							"(<"+SKOS.TOPCONCEPTOF+"> | ^<"+SKOS.HASTOPCONCEPT+">) _:b1";
				}
				query+="\nBIND(\"true\" AS ?isTop )" +
						"\n}";
						
				// this using, used only when no scheme is selected, is used when the concept does not have any
				// brader and it is not topConcept of any scheme
				if(schemeURI == null){
					query+="\nUNION" +
							"\n{" +
							"\n<" + resourceURI + "> a <"+SKOS.CONCEPT+"> ." +
							"\nFILTER(NOT EXISTS{<"+resourceURI+"> "
									+ "(<"+SKOS.BROADER+"> | ^<"+SKOS.NARROWER+">) ?genericConcept })" +
							"\nFILTER (NOT EXISTS{ <"+resourceURI+"> "
									+ "(<"+SKOS.TOPCONCEPTOF+"> | ^<"+SKOS.HASTOPCONCEPT+"> ) ?genericScheme})" +
							"\nBIND(\"true\" AS ?isTop )" +
							"\n}";
				}
						
						
				query+="\n}";
				//@formatter:on
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.property.name().toLowerCase())){
				superResourceVar = "superProperty";
				superSuperResourceVar = "superSuperProperty";
				//@formatter:off
				query = "SELECT DISTINCT ?superProperty ?superSuperProperty ?isTop" + 
						"\nWHERE{" + 
						"\n{" + 
						"\n<" + resourceURI + "> <" + RDFS.SUBPROPERTYOF + ">+ ?superProperty ." +
						"\nOPTIONAL{" +
						"\n?superProperty <" + RDFS.SUBPROPERTYOF + "> ?superSuperProperty ." +
						"\n}" + 
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n<"+resourceURI+"> a ?type ." +
						"\nFILTER( " +
						"?type = <"+RDF.PROPERTY+"> || " +
						"?type = <"+OWL.OBJECTPROPERTY+"> || " +
						"?type = <"+OWL.DATATYPEPROPERTY+"> || " +
						"?type = <"+OWL.ANNOTATIONPROPERTY+"> || " +
						"?type = <"+OWL.ONTOLOGYPROPERTY+"> )" +
						"\nFILTER NOT EXISTS{<"+resourceURI+"> <"+RDFS.SUBPROPERTYOF+"> _:b1}" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";
				//@formatter:on
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.cls.name().toLowerCase())) {
				superResourceVar = "superClass";
				superSuperResourceVar = "superSuperClass";
				//@formatter:off
				query = "SELECT DISTINCT ?superClass ?superSuperClass ?isTop" + 
						"\nWHERE{" + 
						"\n{" + 
						"\n<" + resourceURI + "> <" + RDFS.SUBCLASSOF + ">+ ?superClass ." + 
						"\nOPTIONAL{" +
						"\n?superClass <" + RDFS.SUBCLASSOF + "> ?superSuperClass ." +
						"\n}" + 
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n<"+resourceURI+"> a <"+OWL.CLASS+">." +
						"\nFILTER NOT EXISTS{<"+resourceURI+"> <"+RDFS.SUBCLASSOF+"> _:b1}" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";
				//@formatter:on
				/*
				 // old version, now skosCollection and skosOrderedCollection are managed together
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.skosCollection.name().toLowerCase())){
				superResourceVar = "superCollection";
				superSuperResourceVar = "superSuperCollection";
				//@formatter:off
				query = "SELECT DISTINCT ?superCollection ?superSuperCollection ?isTop" +
						"\nWHERE {"+
						"\n{"+
						"\n?superCollection <"+SKOS.MEMBER+">+ <"+resourceURI+"> ." +
						"\nOPTIONAL {"+
						"?superSuperCollection <"+SKOS.MEMBER+"> ?superCollection ." +
						"\n}" +
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n<"+resourceURI+"> a <"+SKOS.COLLECTION+"> ." +
						"\nFILTER NOT EXISTS{ _:b1 <"+SKOS.MEMBER+"> <"+resourceURI+"> }" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";
				//@formatter:on
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.skosOrderedCollection.name().toLowerCase())){
				//@formatter:off
				query = "SELECT DISTINCT ?superCollection ?superSuperCollection ?isTop" +
						"\nWHERE {"+
						"\n{"+
						"\n?superCollection (<"+SKOS.MEMBERLIST+">/<"+RDF.REST+">* /<"+RDF.FIRST+">)+ <"+resourceURI+"> ." +
						"\nOPTIONAL {"+
						"?superSuperCollection (<"+SKOS.MEMBERLIST+">/<"+RDF.REST+">* /<"+RDF.FIRST+">) ?superCollection ." +
						"\n}" +
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n<"+resourceURI+"> a <"+SKOS.ORDEREDCOLLECTION+"> ." +
						"\nFILTER NOT EXISTS{ _:b1 (<"+SKOS.MEMBERLIST+">/<"+RDF.REST+">* /<"+RDF.FIRST+">) <"+resourceURI+"> }" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";
				//@formatter:on
			*/
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.skosCollection.name().toLowerCase())){
				superResourceVar = "superCollection";
				superSuperResourceVar = "superSuperCollection";
				String complexPropPath = "(<"+SKOS.MEMBER+"> | (<"+SKOS.MEMBERLIST+">/<"+RDF.REST+">*/<"+RDF.FIRST+">))";
				//@formatter:off
				query = "SELECT DISTINCT ?superCollection ?superSuperCollection ?isTop" +
						"\nWHERE {"+
						"\n{"+
						"\n?superCollection "+complexPropPath+"+ <"+resourceURI+"> ." +
						"\nOPTIONAL {"+
						"?superSuperCollection "+complexPropPath+" ?superCollection ." +
						"\n}" +
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n<"+resourceURI+"> a ?type ." +
						"\nFILTER(?type = <"+SKOS.COLLECTION+"> ||  ?type = <"+SKOS.ORDEREDCOLLECTION+"> )"+
						"\nFILTER NOT EXISTS{ _:b1 "+complexPropPath+" <"+resourceURI+"> }" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";
				//@formatter:on
			} else {
				throw new IllegalArgumentException("Invalid input role: "+role);
			}
			logger.debug("query: " + query);
			TupleQuery tupleQuery = owlModel.createTupleQuery(query);

			TupleBindingsIterator iter = tupleQuery.evaluate(false);
			Map<String, ResourceForHierarchy> resourceToResourceForHierarchyMap = 
					new HashMap<String, ResourceForHierarchy>();
			boolean isTopResource = false; 
			while (iter.hasNext()) {
				TupleBindings tupleBindings = iter.next();
				if(tupleBindings.hasBinding(superResourceVar)){
					ARTNode superArtNode = tupleBindings.getBinding(superResourceVar).getBoundValue();
					boolean isResNotURI = false;
					String superResourceShow = null;
					String superResourceId; 
					if(superArtNode.isURIResource()){
						superResourceId = superArtNode.getNominalValue();
						superResourceShow = superArtNode.asURIResource().getLocalName();
					} else { // BNode or Literal 
						superResourceId = "NOT URI "+superArtNode.getNominalValue();
						isResNotURI = true;
					} 
					
					String superSuperResourceId = null;
					String superSuperResourceShow = null;
					boolean isSuperResABNode = false;
					if (tupleBindings.hasBinding(superSuperResourceVar)) {
						ARTNode superSuperResNode = tupleBindings.getBinding(superSuperResourceVar)
								.getBoundValue();
						if(superSuperResNode.isURIResource()){
							superSuperResourceId = superSuperResNode.getNominalValue();
							superSuperResourceShow = superSuperResNode.asURIResource().getLocalName();
						} else { // BNode or Literal
							superSuperResourceId = "NOT URI "+superSuperResNode.getNominalValue();
							isSuperResABNode = true;
						}
					}
					if (!resourceToResourceForHierarchyMap.containsKey(superResourceId)) {
						resourceToResourceForHierarchyMap.put(superResourceId, new ResourceForHierarchy(
								superResourceId, superResourceShow, isResNotURI));
					}
					if (!tupleBindings.hasBinding("isTopConcept")) { // use only for concept
						resourceToResourceForHierarchyMap.get(superResourceId).setTopConcept(false);
					}
					
					if (superSuperResourceId != null) {
						if (!resourceToResourceForHierarchyMap.containsKey(superSuperResourceId)) {
							resourceToResourceForHierarchyMap.put(superSuperResourceId, 
									new ResourceForHierarchy(superSuperResourceId, superSuperResourceShow, 
											isSuperResABNode));
						}
						ResourceForHierarchy resourceForHierarchy = resourceToResourceForHierarchyMap
								.get(superSuperResourceId);
						resourceForHierarchy.addSubResource(superResourceId);
						
						resourceToResourceForHierarchyMap.get(superResourceId).setHasNoSuperResource(false);
					}
				}
				if(tupleBindings.hasBinding("isTop")){
					isTopResource = true;
				}
				
			}
			iter.close();
			
			
			//iterate over the resoruceToResourceForHierarchyMap and look for the topConcept
			//and construct a list of list containing all the possible paths
			// exclude all the path having at least one elment which is not a URI (so a BNode or a Literal)
			List<List<String>> pathList = new ArrayList<List<String>>();
			for(ResourceForHierarchy resourceForHierarchy : resourceToResourceForHierarchyMap.values()){
				if(!resourceForHierarchy.hasNoSuperResource){
					//since it has at least one superElement (superClass, broader concept or superProperty)
					// it cannot be the first element of a path
					continue;
				}
				if(role.toLowerCase().equals(RDFResourceRolesEnum.concept.name())){
					//the role is a concept, so check it an input scheme was passed, if so, if it is not a 
					// top concept (for that particular scheme) then pass to the next concept
					if(schemeURI!=null && !resourceForHierarchy.isTopConcept){
						continue;
					}
				}
				List<String> currentList = new ArrayList<String>();
				currentList.add(resourceForHierarchy.getResource());
				getSubResourcesListUsingResourceFroHierarchy(resourceForHierarchy, currentList, pathList, 
						resourceToResourceForHierarchyMap);
			}
			
			//now construct the response
			//to order the path (from the shortest to the longest) first find the maximum length
			int maxLength = -1;
			for(List<String> path : pathList){
				int currentLength = path.size();
				if(maxLength==-1 || maxLength<currentLength){
					maxLength = currentLength;
				}
			}
			Element pathCollection = XMLHelp.newElement(dataElement, "collection");
			//if it is explicitly a topResource or if no path is returned while there was at least one 
			// result from the SPARQL quey (this mean that all the paths contained at least one non-URI resource)
			if(isTopResource || (pathList.isEmpty() && !resourceToResourceForHierarchyMap.isEmpty() )){
				//the input resource is a top resource for its role (concept, class or property)
				Element pathElem = XMLHelp.newElement(pathCollection, "path");
				pathElem.setAttribute("length", "0");
				Element pathInnerCollection = XMLHelp.newElement(pathElem, "collection");
				Element concElem = XMLHelp.newElement(pathInnerCollection, "uri");
				concElem.setAttribute("role", role);
				concElem.setAttribute("show", inputResource.getLocalName());
				concElem.setTextContent(inputResource.getURI());
			}
			for(int currentLength=1; currentLength<=maxLength; ++currentLength){
				for(List<String> path : pathList){
					if(currentLength != path.size()){
						//it is not the right interation to add this path
						continue;
					}
					Element pathElem = XMLHelp.newElement(pathCollection, "path");
					pathElem.setAttribute("length", path.size()+"");
					Element pathInnerCollection = XMLHelp.newElement(pathElem, "collection");
					for(String conceptInPath : path){
						Element concElem = XMLHelp.newElement(pathInnerCollection, "uri");
						concElem.setAttribute("role", role);
						concElem.setAttribute("show", resourceToResourceForHierarchyMap.get(conceptInPath)
								.getShow());
						concElem.setTextContent(resourceToResourceForHierarchyMap.get(conceptInPath)
								.getResource());
					}
					//add, at the end, the input concept
					Element concElem = XMLHelp.newElement(pathInnerCollection, "uri");
					concElem.setAttribute("role", role);
					concElem.setAttribute("show", inputResource.getLocalName());
					concElem.setTextContent(inputResource.getURI());
				}
			}
			
			return response;
		} catch (ModelAccessException e) {
			return logAndSendException(e);
		} catch (UnsupportedQueryLanguageException e) {
			return logAndSendException(e);
		} catch (MalformedQueryException e) {
			return logAndSendException(e);
		} catch (QueryEvaluationException e) {
			return logAndSendException(e);
		}
	}
	
	
//	private String addFilterForRsourseType(String variable, boolean isClassWanted, 
//			boolean isInstanceWanted, boolean isPropertyWanted, boolean isConceptWanted) {
//		boolean otherWanted = false;
//		String filterQuery = "\nFILTER( ";
//		if(isClassWanted){
//			filterQuery += variable+" = <"+OWL.CLASS+">"; 
//			otherWanted = true;
//		}
//		if(isPropertyWanted){
//			if(otherWanted){
//				filterQuery += " || ";
//			}
//			otherWanted = true;
//			//@formatter:off
//			filterQuery += variable+ " = <"+RDF.PROPERTY+"> || "+
//					variable+" = <"+OWL.OBJECTPROPERTY+"> || "+
//					variable+" = <"+OWL.DATATYPEPROPERTY+"> || "+
//					variable+" = <"+OWL.ANNOTATIONPROPERTY+"> || " +
//					variable+" = <"+OWL.ONTOLOGYPROPERTY+"> ";
//			//@formatter:on
//		}
//		if(isConceptWanted){
//			if(otherWanted){
//				filterQuery += " || ";
//			}
//			otherWanted = true;
//			filterQuery += variable+" = <"+SKOS.CONCEPT+">";
//		}
//		if(isInstanceWanted){
//			if(otherWanted){
//				filterQuery += " || ( ";
//			}
//			//@formatter:off
//			filterQuery+="EXISTS{"+variable+" a <"+OWL.CLASS+">}";
//			
//			//old version
//			/*filterQuery+=variable+"!= <"+OWL.CLASS+"> && "+
//					variable+"!=<"+RDFS.CLASS+"> && "+
//					variable+"!=<"+RDFS.RESOURCE+"> && "+
//					variable+"!=<"+RDF.PROPERTY+"> && "+
//					variable+"!=<"+OWL.OBJECTPROPERTY+"> && "+
//					variable+"!=<"+OWL.DATATYPEPROPERTY+"> && "+
//					variable+"!=<"+OWL.ANNOTATIONPROPERTY+"> && "+
//					variable+"!=<"+OWL.ONTOLOGYPROPERTY+"> && "+
//					variable+"!=<"+SKOS.CONCEPT+"> && "+
//					variable+"!=<"+SKOS.CONCEPTSCHEME+"> && "+
//					variable+"!=<"+SKOSXL.LABEL+">";*/
//			//@formatter:on
//			if(otherWanted){
//				filterQuery += " ) ";
//			}
//			otherWanted = true;
//		}
//		
//		filterQuery += ")";
//		return filterQuery;
//	}
	
	
	private String filterResourceTypeAndScheme(String resource, String type, boolean isClassWanted, 
			boolean isInstanceWanted, boolean isPropertyWanted, boolean isConceptWanted, 
			boolean isConceptSchemeWanted, boolean isCollectionWanted, ARTURIResource scheme){
		boolean otherWanted = false;
		String filterQuery = "";
		
		if(isClassWanted){
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\nFILTER("+type+" = <"+OWL.CLASS+"> || " +
							type+" = <"+RDFS.CLASS+"> )" +
					"\n}";
			
			otherWanted = true;
		}
		if(isPropertyWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\nFILTER("+type+ " = <"+RDF.PROPERTY+"> || "+
					type+" = <"+OWL.OBJECTPROPERTY+"> || "+
					type+" = <"+OWL.DATATYPEPROPERTY+"> || "+
					type+" = <"+OWL.ANNOTATIONPROPERTY+"> || " +
					type+" = <"+OWL.ONTOLOGYPROPERTY+"> )"+
					"\n}";
			otherWanted = true;
		}
		if(isConceptWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					 "\nFILTER("+type+" = <"+SKOS.CONCEPT+">)";
			if(scheme!=null && scheme.getURI().length()>0){
				filterQuery += "\n"+resource+" <"+SKOS.INSCHEME+"> <"+scheme.getURI()+"> .";
			}
			
			filterQuery += "\n}";
			
			otherWanted = true;
		}
		if(isConceptSchemeWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					 "\nFILTER("+type+" = <"+SKOS.CONCEPTSCHEME+">)";
			
			filterQuery += "\n}";
			
			otherWanted = true;
		}
		if(isCollectionWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					 "\nFILTER("+type+" = <"+SKOS.COLLECTION+"> || " +
					 		 type+" = <"+SKOS.ORDEREDCOLLECTION+"> )" +
					 "\n}";
			
			otherWanted = true;
		}
		if(isInstanceWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\n?type a <"+OWL.CLASS+"> . "+
					//"\n?type a ?classType ." +
					//"\nFILTER (EXISTS{?classType a <"+OWL.CLASS+">})"+
					"\n}";
				
				otherWanted = true;
		}
		
		return filterQuery;
	}
	

	private String searchModePrepareQuery(String variable, String value, String searchMode){
		String query ="";
		
		if(searchMode.equals(START_SEARCH_MODE)){
			query="\nFILTER regex(str("+variable+"), '^"+value+"', 'i')";
		} else if(searchMode.equals(END_SEARCH_MODE)){
			query="\nFILTER regex(str("+variable+"), '"+value+"$', 'i')";
		} else if(searchMode.equals(CONTAINS_SEARCH_MODE)){
			query="\nFILTER regex(str("+variable+"), '"+value+"', 'i')";
		} else { // searchMode.equals(contains)
			query="\nFILTER regex(str("+variable+"), '"+value+"', 'i')";
		}
		
		return query;
	}
	
	private void getSubResourcesListUsingResourceFroHierarchy(ResourceForHierarchy resource, 
			List<String> currentPathList, List<List<String>> pathList, 
			Map<String, ResourceForHierarchy> resourceToResourceForHierarchyMap ){
		List<String> subResourceList = resource.getSubResourcesList();
		
		if(resource.isNotURI){
			//since this resource is not a URI, then the path to which this resource belong to must not be 
			// consider, so, do not add to the possible paths
			return;
		}
		
		if(subResourceList.isEmpty()) {
			pathList.add(currentPathList);
		} else{
			for(String subResource : subResourceList){
				List<String> updatedPath = new ArrayList<String>(currentPathList);
				if(updatedPath.contains(subResource)){
					//this element already exist in the path, it is a cycle, so, skip this element
					continue;
				}
				updatedPath.add(subResource);
				// check if the subResource has no element above (broader or superClass), this mean that it 
				// is the last element in the path
				if(resourceToResourceForHierarchyMap.get(subResource).getSubResourcesList().isEmpty()){
					pathList.add(updatedPath);
					continue;
				}
				ResourceForHierarchy updatedResourceForHierarchy = resourceToResourceForHierarchyMap
						.get(subResource);
				getSubResourcesListUsingResourceFroHierarchy(updatedResourceForHierarchy, updatedPath, 
						pathList, resourceToResourceForHierarchyMap);
		}
		}
	}
	
	private class ResourceForHierarchy {
		private boolean isTopConcept; // used only for concept
		private boolean hasNoSuperResource;
		private List<String>subResourcesList;
		private String resource;
		private String show;
		private boolean isNotURI;
		
		public ResourceForHierarchy(String resource, String show, boolean isNotURI) {
			this.resource = resource;
			this.show = show;
			this.isNotURI = isNotURI;
			isTopConcept = true;
			hasNoSuperResource = true;
			subResourcesList = new ArrayList<String>();
		}
		
		public boolean isNotURI(){
			return isNotURI;
		}
		
		public String getResource(){
			return resource;
		}
		
		public String getShow(){
			return show;
		}

		public boolean isTopConcept() {
			return isTopConcept;
		}

		public void setTopConcept(boolean isTopConcept) {
			this.isTopConcept = isTopConcept;
		}

		
		public boolean hasNoSuperResource() {
			return hasNoSuperResource;
		}

		public void setHasNoSuperResource(boolean haNoSuperResource) {
			this.hasNoSuperResource = haNoSuperResource;
		}

		public List<String> getSubResourcesList() {
			return subResourcesList;
		}
		
		public void addSubResource(String subResource){
			if(!subResourcesList.contains(subResource)){
				subResourcesList.add(subResource);
			}
		}
	}
	
	private RDFResourceRolesEnum getRoleFromType(String typeURI){
		RDFResourceRolesEnum role;
		if(typeURI.equals(OWL.CLASS) || typeURI.equals(RDFS.CLASS) ){
			role = RDFResourceRolesEnum.cls;
		} else if(typeURI.equals(RDF.PROPERTY)){
			role = RDFResourceRolesEnum.property;
		} else if(typeURI.equals(OWL.OBJECTPROPERTY)){
			role = RDFResourceRolesEnum.objectProperty;
		} else if(typeURI.equals(OWL.DATATYPEPROPERTY)){
			role = RDFResourceRolesEnum.datatypeProperty;
		} else if(typeURI.equals(OWL.ANNOTATIONPROPERTY)){
			role = RDFResourceRolesEnum.annotationProperty;
		} else if(typeURI.equals(OWL.ONTOLOGYPROPERTY)){
			role = RDFResourceRolesEnum.ontologyProperty;
		}  else if(typeURI.equals(SKOS.CONCEPT)){
			role = RDFResourceRolesEnum.concept;
		} else if(typeURI.equals(SKOS.COLLECTION)){
			role = RDFResourceRolesEnum.skosCollection;
		} else if(typeURI.equals(SKOS.ORDEREDCOLLECTION)){
			role = RDFResourceRolesEnum.skosOrderedCollection;
		} else if(typeURI.equals(SKOSXL.LABEL)){
			role = RDFResourceRolesEnum.xLabel;
		} else if(typeURI.equals(SKOS.CONCEPTSCHEME)){
			role = RDFResourceRolesEnum.conceptScheme;
		} else {
			role = RDFResourceRolesEnum.individual;
		} 
		
		return role;
	}
	
}
