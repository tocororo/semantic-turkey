package it.uniroma2.art.semanticturkey.services.core;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@STService
public class Search extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Search.class);
	
//	private static String CLASS_ROLE = "class";
//	private static String CONCEPT_ROLE = "concept";
//	private static String INSTANCE_ROLE = "instance";
	
	private static String START_SEARCH_MODE = "start";
	private static String CONTAINS_SEARCH_MODE = "contain";
	private static String END_SEARCH_MODE = "end";
	private static String EXACT_SEARCH_MODE = "exact";
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResource(String searchString, String [] rolesArray, boolean useLocalName, boolean useURI,
			String searchMode, @Optional String lang, @Optional List<IRI> schemes)  {
		
		boolean isClassWanted = false;
		boolean isConceptWanted = false;
		boolean isConceptSchemeWanted = false;
		boolean isInstanceWanted = false;
		boolean isPropertyWanted = false;
		boolean isCollectionWanted = false;
		
		String searchModeSelected = null;
		
		
		Collection<AnnotatedValue<Resource>> results = new ArrayList<AnnotatedValue<Resource>>();
		
		if(searchString.isEmpty()){
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException("the serchString cannot be empty");
		}
		
		for(int i=0; i<rolesArray.length; ++i){
			//TODO remove the RDFResourceRolesEnum from the owlart and find an equivalent in RDF4J
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
			
			String msg = "the serch roles should be at least one of: "+
					RDFResourceRolesEnum.cls.name()+", "+
					RDFResourceRolesEnum.concept.name()+", "+
					RDFResourceRolesEnum.conceptScheme.name()+", "+
					RDFResourceRolesEnum.individual+", "+
					RDFResourceRolesEnum.property.name() +" or "+
					RDFResourceRolesEnum.skosCollection.name();
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException(msg);
			
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
			String msg = "the serch mode should be at one of: "+START_SEARCH_MODE+", "+
			CONTAINS_SEARCH_MODE+", "+END_SEARCH_MODE+" or "+EXACT_SEARCH_MODE;
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException(msg);
		}
		
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
				isPropertyWanted, isConceptWanted, isConceptSchemeWanted, isCollectionWanted, schemes);
		
		
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
					"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?skosLabel ."+
					searchModePrepareQuery("?skosLabel", searchString, searchModeSelected) +
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?literalForm ." +
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
					"\n?resource <"+SKOSXL.PREF_LABEL.stringValue()+"> ?skosPrefLabel ." +
					"\n?skosPrefLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?show ." +
					"\nFILTER(lang(?show) = \""+lang+"\")"+
					"\n}" +
					"\nOPTIONAL" +
					"\n{" +
					"\n?resource <"+SKOS.PREF_LABEL.stringValue()+"> ?show ." +
					"\nFILTER(lang(?show) = \""+lang+"\")"+
					"\n}";
		}
		
		query+="\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		
		TupleQuery tupleQuery;
		tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		Resource[] namedGraphs = getUserNamedGraphs();
		for(Resource namedGraph : namedGraphs){
			if(namedGraph instanceof IRI){
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);

		
		TupleQueryResult tupleBindingsIterator = tupleQuery.evaluate();
		
		Map<String, ValueTypeAndShow> propertyMap = new HashMap<String, ValueTypeAndShow>();
		Map<String, ValueTypeAndShow> otherResourcesMap = new HashMap<String, ValueTypeAndShow>();
		
		
		while (tupleBindingsIterator.hasNext()) {
			BindingSet tupleBindings = tupleBindingsIterator.next();
			Value value = tupleBindings.getBinding("resource").getValue();

			if (!(value instanceof IRI)) {
				continue;
			}

			// TODO, explicit set to true
			RDFResourceRolesEnum role = null;
			//since there are more than one element in the input role array, see the resource
			String type = tupleBindings.getBinding("type").getValue().stringValue();
			
			role = getRoleFromType(type);
			
			String show = null;
			if(tupleBindings.hasBinding("show")){
				Value showRes = tupleBindings.getBinding("show").getValue();
				show = showRes.stringValue();
				if(showRes instanceof Literal && ((Literal)showRes).getLanguage().isPresent()){
					show += " ("+((Literal)showRes).getLanguage().get()+")";
				}
			}
			
			if(role.equals(RDFResourceRolesEnum.cls)){
				if(otherResourcesMap.containsKey(value.stringValue())){
					//the class was already added
					continue;
				}
				//remove all the classes which belongs to xml/rdf/rdfs/owl to exclude from the results those
				// classes which are not visible in the class tree (as it is done in #ClsOld.getSubClasses since 
				// when the parent class is Owl:Thing the service filters out those classes with 
				// NoLanguageResourcePredicate)
				if(role.equals(RDFResourceRolesEnum.cls)){
					String resNamespace = value.stringValue();
					if(resNamespace.equals(XMLSchema.NAMESPACE) || resNamespace.equals(RDF.NAMESPACE) 
							|| resNamespace.equals(RDFS.NAMESPACE) || resNamespace.equals(OWL.NAMESPACE) ){
						continue;
					}
				}
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, show, role);
				otherResourcesMap.put(value.stringValue(), valueTypeAndShow);
			} else if(role.equals(RDFResourceRolesEnum.individual)){
				//there a special section for the individual, since an individual can belong to more than a
				// class, so the result set could have more tuple regarding a single individual, this way
				// should speed up the process
				if(otherResourcesMap.containsKey(value.stringValue())){
					//the individual was already added
					continue;
				}
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, show, role);
				otherResourcesMap.put(value.stringValue(), valueTypeAndShow);
			} else if(role.equals(RDFResourceRolesEnum.property) || 
					role.equals(RDFResourceRolesEnum.annotationProperty) || 
					role.equals(RDFResourceRolesEnum.datatypeProperty) || 
					role.equals(RDFResourceRolesEnum.objectProperty) || 
					role.equals(RDFResourceRolesEnum.ontologyProperty) ) {
				//check if the property was already added before (with a different type)
				if(propertyMap.containsKey(value.stringValue())){
					ValueTypeAndShow prevValueTypeAndShow = propertyMap.get(value.stringValue());
					if(prevValueTypeAndShow.getRole().equals(RDFResourceRolesEnum.property)){
						//the previous value was property, now it has a different role, so replace the old 
						// one with the new one
						ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, show, role);
						propertyMap.put(value.stringValue(), valueTypeAndShow);
					}
				} else{
					//the property map did not have a previous value, so add this one without any checking
					ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, show, role);
					propertyMap.put(value.stringValue(), valueTypeAndShow);
				}
			} else{
				//it is a concept, a conceptScheme or a collection, just add it to the otherMap
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, show, role);
				otherResourcesMap.put(value.stringValue(), valueTypeAndShow);
			}
		}
		
		//now iterate over the 2 maps and construct the responses
		for(String key : otherResourcesMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = otherResourcesMap.get(key);
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(valueTypeAndShow.getResource());
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("role", valueTypeAndShow.getRole().name());
			if(valueTypeAndShow.isShowPresent() && !valueTypeAndShow.getShow().isEmpty()){
				annotatedValue.setAttribute("show", valueTypeAndShow.getShow());
			} 
			results.add(annotatedValue);
		}
		for(String key : propertyMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = propertyMap.get(key);
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(valueTypeAndShow.getResource());
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("role", valueTypeAndShow.getRole().name());
			if(valueTypeAndShow.isShowPresent() && !valueTypeAndShow.getShow().isEmpty()){
				annotatedValue.setAttribute("show", valueTypeAndShow.getShow());
			} 
			results.add(annotatedValue);
		}
		return results;
	}
	
	
	private class ValueTypeAndShow{
		IRI resource  = null;
		String show = null;
		RDFResourceRolesEnum role = null;
		
		public ValueTypeAndShow(IRI resource, String show, RDFResourceRolesEnum role) {
			this.resource = resource;
			this.show = show;
			this.role = role;
		}

		public IRI getResource() {
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
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(IRI cls, String searchString, boolean useLocalName, 
			boolean useURI, String searchMode, @Optional String lang) {
		
		Collection<AnnotatedValue<Resource>> results = new ArrayList<AnnotatedValue<Resource>>();
		
		String searchModeSelected = null;
		
		
		if(searchString.isEmpty()){
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException("the serchString cannot be empty");
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
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException("the serchString cannot be empty");
		}
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
			"\nWHERE{" +
			"\n{";
		//do a subquery to get the candidate resources
		query+="\nSELECT DISTINCT ?resource" +
			"\nWHERE{" + 
			"\n ?resource a <"+cls.stringValue()+"> . " +
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
		
		TupleQuery tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		Resource[] namedGraphs = getUserNamedGraphs();
		for(Resource namedGraph : namedGraphs){
			if(namedGraph instanceof IRI){
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);
		
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		// Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		List<String> addedIndividualList = new ArrayList<String>();
		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			Value resourceURI = bindingSet.getBinding("resource").getValue();

			if (!(resourceURI instanceof IRI)) {
				continue;
			}

			if(addedIndividualList.contains(resourceURI.stringValue())){
				//the individual was already added
				continue;
			}
			addedIndividualList.add(resourceURI.stringValue());
			
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>((IRI)resourceURI);
			annotatedValue.setAttribute("explicit", true);
			
			String show = null;
			if(bindingSet.hasBinding("show")){
				Value showRes = bindingSet.getBinding("show").getValue();
				show = showRes.stringValue();
				if(showRes instanceof Literal && ((Literal)showRes).getLanguage().isPresent()){
					show += " ("+((Literal)showRes).getLanguage().get()+")";
				}
				annotatedValue.setAttribute("show", show);
			}
			results.add(annotatedValue);
			
		}
		return results;
	}

	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getPathFromRoot(String role, IRI resourceURI, @Optional List<IRI> schemesIRI)
			throws InvalidParameterException{
		
		//ARTURIResource inputResource = owlModel.createURIResource(resourceURI);
		
		String query = null;
		String superResourceVar = null, superSuperResourceVar = null;
		if(role.toLowerCase().equals(RDFResourceRolesEnum.concept.name().toLowerCase())){
			superResourceVar = "broader";
			superSuperResourceVar = "broaderOfBroader";
			//@formatter:off
			query = "SELECT DISTINCT ?broader ?broaderOfBroader ?isTopConcept ?isTop" + 
					"\nWHERE{" +
					"\n{" + 
					"\n<" + resourceURI.stringValue() + "> (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+"> )+ ?broader .";
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broader <" + SKOS.IN_SCHEME.stringValue() + "> <" + schemesIRI.get(0).stringValue() + "> ."+
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) <"+schemesIRI.get(0).stringValue()+"> ." +
						"\n}";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query += "\n?broader <" + SKOS.IN_SCHEME.stringValue() + "> ?scheme1 ."+
						filterWithOrValues(schemesIRI, "?scheme1") +
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) ?scheme2 ." +
						filterWithOrValues(schemesIRI, "?scheme2") +
						"\n}";
			}
			query += "\nOPTIONAL{" +
					"\n?broader (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+">) ?broaderOfBroader .";
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broaderOfBroader <" + SKOS.IN_SCHEME.stringValue() + "> <" + schemesIRI.get(0).stringValue() + "> . ";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query += "\n?broaderOfBroader <" + SKOS.IN_SCHEME.stringValue() + "> ?scheme3 . "+
				filterWithOrValues(schemesIRI, "?scheme3");
			}
			query +="\n}" + 
					"\n}" +
					"\nUNION" +
					"\n{";
			//this union is used when the first part does not return anything, so when the desired concept
			// does not have any broader, but it is defined as topConcept (to either a specified scheme or
			// to at least one)
			query+= "\n<" + resourceURI.stringValue() + "> a <"+SKOS.CONCEPT+"> .";
			if(schemesIRI != null && schemesIRI.size()==1){
					query+="\n<"+resourceURI.stringValue()+"> " +
							"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) <"+schemesIRI.get(0).stringValue()+"> .";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query+="\n<"+resourceURI.stringValue()+"> " +
						"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) ?scheme4 ."+
						filterWithOrValues(schemesIRI, "?scheme4");
			} else{
				query+="\n<"+resourceURI.stringValue()+"> " +
						"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) _:b1";
			}
			query+="\nBIND(\"true\" AS ?isTop )" +
					"\n}";
					
			// this using, used only when no scheme is selected, is used when the concept does not have any
			// brader and it is not topConcept of any scheme
			if(schemesIRI == null){
				query+="\nUNION" +
						"\n{" +
						"\n<" + resourceURI.stringValue() + "> a <"+SKOS.CONCEPT+"> ." +
						"\nFILTER(NOT EXISTS{<"+resourceURI.stringValue()+"> "
								+ "(<"+SKOS.BROADER+"> | ^<"+SKOS.NARROWER+">) ?genericConcept })" +
						"\nFILTER (NOT EXISTS{ <"+resourceURI.stringValue()+"> "
								+ "(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+"> ) ?genericScheme})" +
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
					"\n<" + resourceURI.stringValue() + "> <" + RDFS.SUBPROPERTYOF.stringValue() + ">+ ?superProperty ." +
					"\nOPTIONAL{" +
					"\n?superProperty <" + RDFS.SUBPROPERTYOF.stringValue() + "> ?superSuperProperty ." +
					"\n}" + 
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI.stringValue()+"> a ?type ." +
					"\nFILTER( " +
					"?type = <"+RDF.PROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.OBJECTPROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.DATATYPEPROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.ANNOTATIONPROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.ONTOLOGYPROPERTY.stringValue()+"> )" +
					"\nFILTER NOT EXISTS{<"+resourceURI.stringValue()+"> <"+RDFS.SUBPROPERTYOF.stringValue()+"> _:b1}" +
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
					"\n<" + resourceURI.stringValue() + "> <" + RDFS.SUBCLASSOF.stringValue() + ">+ ?superClass ." + 
					"\nOPTIONAL{" +
					"\n?superClass <" + RDFS.SUBCLASSOF.stringValue() + "> ?superSuperClass ." +
					"\n}" + 
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI.stringValue()+"> a <"+OWL.CLASS.stringValue()+">." +
					"\nFILTER NOT EXISTS{<"+resourceURI.stringValue()+"> <"+RDFS.SUBCLASSOF.stringValue()+"> _:b1}" +
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
			String complexPropPath = "(<"+SKOS.MEMBER.stringValue()+"> | (<"+SKOS.MEMBER_LIST.stringValue()+">/<"+RDF.REST.stringValue()+">*/<"+RDF.FIRST.stringValue()+">))";
			//@formatter:off
			query = "SELECT DISTINCT ?superCollection ?superSuperCollection ?isTop" +
					"\nWHERE {"+
					"\n{"+
					"\n?superCollection "+complexPropPath+"+ <"+resourceURI.stringValue()+"> ." +
					"\nOPTIONAL {"+
					"?superSuperCollection "+complexPropPath+" ?superCollection ." +
					"\n}" +
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI.stringValue()+"> a ?type ." +
					"\nFILTER(?type = <"+SKOS.COLLECTION.stringValue()+"> ||  ?type = <"+SKOS.ORDERED_COLLECTION.stringValue()+"> )"+
					"\nFILTER NOT EXISTS{ _:b1 "+complexPropPath+" <"+resourceURI.stringValue()+"> }" +
					"\nBIND(\"true\" AS ?isTop )" +
					"\n}" +
					"\n}";
			//@formatter:on
		} else {
			throw new IllegalArgumentException("Invalid input role: "+role);
		}
		logger.debug("query: " + query);
		
		TupleQuery tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		Resource[] namedGraphs = getUserNamedGraphs();
		for(Resource namedGraph : namedGraphs){
			if(namedGraph instanceof IRI){
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);

		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		Map<String, ResourceForHierarchy> resourceToResourceForHierarchyMap = 
				new HashMap<String, ResourceForHierarchy>();
		boolean isTopResource = false; 
		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			if(bindingSet.hasBinding(superResourceVar)){
				Value superArtNode = bindingSet.getBinding(superResourceVar).getValue();
				boolean isResNotURI = false;
				String superResourceShow = null;
				String superResourceId; 
				if(superArtNode instanceof IRI){
					superResourceId = superArtNode.stringValue();
					superResourceShow = ((IRI) superArtNode).getLocalName();
				} else { // BNode or Literal 
					superResourceId = "NOT URI "+superArtNode.stringValue();
					isResNotURI = true;
				} 
				
				String superSuperResourceId = null;
				String superSuperResourceShow = null;
				Value superSuperResNode = null;
				boolean isSuperResABNode = false;
				if (bindingSet.hasBinding(superSuperResourceVar)) {
					superSuperResNode = bindingSet.getBinding(superSuperResourceVar).getValue();
					if(superSuperResNode instanceof IRI){
						superSuperResourceId = superSuperResNode.stringValue();
						superSuperResourceShow = ((IRI) superSuperResNode).getLocalName();
					} else { // BNode or Literal
						superSuperResourceId = "NOT URI "+superSuperResNode.stringValue();
						isSuperResABNode = true;
					}
				}
				if (!resourceToResourceForHierarchyMap.containsKey(superResourceId)) {
					resourceToResourceForHierarchyMap.put(superResourceId, new ResourceForHierarchy(
							superArtNode, superResourceShow, isResNotURI));
				}
				if (!bindingSet.hasBinding("isTopConcept")) { // use only for concept
					resourceToResourceForHierarchyMap.get(superResourceId).setTopConcept(false);
				}
				
				if (superSuperResNode != null) {
					if (!resourceToResourceForHierarchyMap.containsKey(superSuperResourceId)) {
						resourceToResourceForHierarchyMap.put(superSuperResourceId, 
								new ResourceForHierarchy(superSuperResNode, superSuperResourceShow, 
										isSuperResABNode));
					}
					ResourceForHierarchy resourceForHierarchy = resourceToResourceForHierarchyMap
							.get(superSuperResourceId);
					resourceForHierarchy.addSubResource(superResourceId);
					
					resourceToResourceForHierarchyMap.get(superResourceId).setHasNoSuperResource(false);
				}
			}
			if(bindingSet.hasBinding("isTop")){
				isTopResource = true;
			}
			
		}
		tupleQueryResult.close();
		
		
		//iterate over the resoruceToResourceForHierarchyMap and look for the topConcept
		//and construct a list of list containing all the possible paths
		// exclude all the path having at least one element which is not a URI (so a BNode or a Literal)
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
				if(schemesIRI!=null && !resourceForHierarchy.isTopConcept){
					continue;
				}
			}
			List<String> currentList = new ArrayList<String>();
			currentList.add(resourceForHierarchy.getValue().stringValue());
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
		
		boolean pathFound = false;
		Collection<AnnotatedValue<Resource>> results = new ArrayList<AnnotatedValue<Resource>>();
		
		//if it is explicitly a topResource or if no path is returned while there was at least one 
		// result from the SPARQL quey (this mean that all the paths contained at least one non-URI resource)
		if(isTopResource || (pathList.isEmpty() && !resourceToResourceForHierarchyMap.isEmpty() )){
			//the input resource is a top resource for its role (concept, class or property)
			pathFound = true;
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>((IRI)resourceURI);
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("show", resourceURI.getLocalName());
			results.add(annotatedValue);
		}
		
		
		for(int currentLength=1; currentLength<=maxLength && pathFound; ++currentLength){
			for(List<String> path : pathList){
				if(currentLength != path.size()){
					//it is not the right iteration to add this path
					continue;
				}
				pathFound = true;
				
				for(String conceptInPath : path){
					AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(
							(Resource) resourceToResourceForHierarchyMap.get(conceptInPath).getValue());
					annotatedValue.setAttribute("explicit", true);
					annotatedValue.setAttribute("show", resourceToResourceForHierarchyMap.get(conceptInPath)
							.getShow());
					results.add(annotatedValue);
				}
				//add, at the end, the input concept
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>((IRI)resourceURI);
				annotatedValue.setAttribute("explicit", true);
				annotatedValue.setAttribute("show", resourceURI.getLocalName());
				results.add(annotatedValue);
				
			}
		}
		
		return results;
	}
	
	
	private String filterWithOrValues(List<IRI> IRIList, String variable){
		if(!variable.startsWith("?")){
			variable+="?"+variable;
		}
		String schemesInFilter = "\nFILTER (";
		boolean first=true;
		for(IRI iri : IRIList){
			if(!first){
				schemesInFilter+=" || ";
			}
			first=false;
			schemesInFilter+="variable="+NTriplesUtil.toNTriplesString(iri);
		}
		schemesInFilter+= ") \n";
		return schemesInFilter;
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
			boolean isConceptSchemeWanted, boolean isCollectionWanted, List<IRI> schemes){
		boolean otherWanted = false;
		String filterQuery = "";
		
		if(isClassWanted){
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\nFILTER("+type+" = <"+OWL.CLASS.stringValue()+"> || " +
							type+" = <"+RDFS.CLASS.stringValue()+"> )" +
					"\n}";
			
			otherWanted = true;
		}
		if(isPropertyWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\nFILTER("+type+ " = <"+RDF.PROPERTY.stringValue()+"> || "+
					type+" = <"+OWL.OBJECTPROPERTY.stringValue()+"> || "+
					type+" = <"+OWL.DATATYPEPROPERTY.stringValue()+"> || "+
					type+" = <"+OWL.ANNOTATIONPROPERTY.stringValue()+"> || " +
					type+" = <"+OWL.ONTOLOGYPROPERTY.stringValue()+"> )"+
					"\n}";
			otherWanted = true;
		}
		if(isConceptWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					 "\nFILTER("+type+" = <"+SKOS.CONCEPT.stringValue()+">)";
			if(schemes!=null && schemes.size()==1){
				filterQuery += "\n"+resource+" <"+SKOS.IN_SCHEME.stringValue()+"> <"+schemes.get(0).stringValue()+"> .";
			} else if(schemes!=null && schemes.size()>1){
				filterQuery += "\n"+resource+" <"+SKOS.IN_SCHEME.stringValue()+"> ?scheme0 . "+
						filterWithOrValues(schemes, "?scheme0");
			}
			
			filterQuery += "\n}";
			
			otherWanted = true;
		}
		if(isConceptSchemeWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					 "\nFILTER("+type+" = <"+SKOS.CONCEPT_SCHEME.stringValue()+">)";
			
			filterQuery += "\n}";
			
			otherWanted = true;
		}
		if(isCollectionWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					 "\nFILTER("+type+" = <"+SKOS.COLLECTION.stringValue()+"> || " +
					 		 type+" = <"+SKOS.ORDERED_COLLECTION.stringValue()+"> )" +
					 "\n}";
			
			otherWanted = true;
		}
		if(isInstanceWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\n?type a <"+OWL.CLASS.stringValue()+"> . "+
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
		//private String resourceString;
		private Value value;
		private String show;
		private boolean isNotURI;
		
		public ResourceForHierarchy(Value value, String show, boolean isNotURI) {
			//this.resourceString = resource.stringValue();
			this.value = value;
			this.show = show;
			this.isNotURI = isNotURI;
			isTopConcept = true;
			hasNoSuperResource = true;
			subResourcesList = new ArrayList<String>();
		}
		
		public boolean isNotURI(){
			return isNotURI;
		}
		
		/*public String getResourceString(){
			return resourceString;
		}*/
		
		public Value getValue(){
			return value;
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
		if(typeURI.equals(OWL.CLASS.stringValue()) || typeURI.equals(RDFS.CLASS.stringValue()) ){
			role = RDFResourceRolesEnum.cls;
		} else if(typeURI.equals(RDF.PROPERTY.stringValue())){
			role = RDFResourceRolesEnum.property;
		} else if(typeURI.equals(OWL.OBJECTPROPERTY.stringValue())){
			role = RDFResourceRolesEnum.objectProperty;
		} else if(typeURI.equals(OWL.DATATYPEPROPERTY.stringValue())){
			role = RDFResourceRolesEnum.datatypeProperty;
		} else if(typeURI.equals(OWL.ANNOTATIONPROPERTY.stringValue())){
			role = RDFResourceRolesEnum.annotationProperty;
		} else if(typeURI.equals(OWL.ONTOLOGYPROPERTY.stringValue())){
			role = RDFResourceRolesEnum.ontologyProperty;
		}  else if(typeURI.equals(SKOS.CONCEPT.stringValue())){
			role = RDFResourceRolesEnum.concept;
		} else if(typeURI.equals(SKOS.COLLECTION.stringValue())){
			role = RDFResourceRolesEnum.skosCollection;
		} else if(typeURI.equals(SKOS.ORDERED_COLLECTION.stringValue())){
			role = RDFResourceRolesEnum.skosOrderedCollection;
		} else if(typeURI.equals(SKOSXL.LABEL.stringValue())){
			role = RDFResourceRolesEnum.xLabel;
		} else if(typeURI.equals(SKOS.CONCEPT_SCHEME.stringValue())){
			role = RDFResourceRolesEnum.conceptScheme;
		} else {
			role = RDFResourceRolesEnum.individual;
		} 
		
		return role;
	}
	
}
