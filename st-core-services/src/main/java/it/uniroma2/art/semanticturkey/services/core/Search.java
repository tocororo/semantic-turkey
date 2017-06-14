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

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

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
			String searchMode, @Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException  {
		
		//it can be null, * or a list of languages
		String languagesPropValue = STPropertiesManager.getProjectPreference(
			       STPropertiesManager.PROP_LANGUAGES, getProject(), UsersManager.getLoggedUser(), RenderingEngine.class.getName());
		String [] langArray;
		if(languagesPropValue == null){
			langArray = new String[]{"*"};
		} else{
			langArray = languagesPropValue.split(",");
		}
		
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
			if(rolesArray[i].toLowerCase().equals(RDFResourceRole.cls.name())){
				isClassWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.concept.name().toLowerCase())){
				isConceptWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.conceptScheme.name().toLowerCase())){
				isConceptSchemeWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.individual.name().toLowerCase())){
				isInstanceWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.property.name().toLowerCase())){
				isPropertyWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.skosCollection.name().toLowerCase())){
				isCollectionWanted = true;
			} 
		}
		//@formatter:off
		if(!isClassWanted && !isConceptWanted && !isConceptSchemeWanted && 
				!isInstanceWanted && !isPropertyWanted && !isCollectionWanted){
			
			String msg = "the serch roles should be at least one of: "+
					RDFResourceRole.cls.name()+", "+
					RDFResourceRole.concept.name()+", "+
					RDFResourceRole.conceptScheme.name()+", "+
					RDFResourceRole.individual+", "+
					RDFResourceRole.property.name() +" or "+
					RDFResourceRole.skosCollection.name();
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
			
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
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
				"\n}"+
		//search in skos:prefLabel and skos:altLabel
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?skosLabel ."+
				searchModePrepareQuery("?skosLabel", searchString, searchModeSelected) +
				"\n}" +
				//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
				"\nUNION" +
				"\n{" +
				"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
				"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?literalForm ." +
				searchModePrepareQuery("?literalForm", searchString, searchModeSelected) +
				"\n}"+
		//add the show part according to the Lexicalization Model
				addShowPart("?show", langArray)+
				"\n}";
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

			RDFResourceRole role = null;
			boolean isProp = false;
			//since there are more than one element in the input role array, see the resource
			String type = tupleBindings.getBinding("type").getValue().stringValue();
			
			role = getRoleFromType(type);
			
			if(role.equals(RDFResourceRole.cls)){
				//remove all the classes which belongs to xml/rdf/rdfs/owl to exclude from the results those
				// classes which are not visible in the class tree (as it is done in #ClsOld.getSubClasses since 
				// when the parent class is Owl:Thing the service filters out those classes with 
				// NoLanguageResourcePredicate)
				String resNamespace = value.stringValue();
				if(resNamespace.equals(XMLSchema.NAMESPACE) || resNamespace.equals(RDF.NAMESPACE) 
						|| resNamespace.equals(RDFS.NAMESPACE) || resNamespace.equals(OWL.NAMESPACE) ){
					continue;
				}
				if(!otherResourcesMap.containsKey(value.stringValue())){
					ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, role);
					otherResourcesMap.put(value.stringValue(), valueTypeAndShow);
				}
				
			} else if(role.equals(RDFResourceRole.individual)){
				//there a special section for the individual, since an individual can belong to more than a
				// class, so the result set could have more tuple regarding a single individual, this way
				// should speed up the process
				if(!otherResourcesMap.containsKey(value.stringValue())){
					ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, role);
					otherResourcesMap.put(value.stringValue(), valueTypeAndShow);
				}
			} else if(role.equals(RDFResourceRole.property) || 
					role.equals(RDFResourceRole.annotationProperty) || 
					role.equals(RDFResourceRole.datatypeProperty) || 
					role.equals(RDFResourceRole.objectProperty) || 
					role.equals(RDFResourceRole.ontologyProperty) ) {
				isProp = true;
				//check if the property was already added before (with a different type)
				if(propertyMap.containsKey(value.stringValue())){
					ValueTypeAndShow prevValueTypeAndShow = propertyMap.get(value.stringValue());
					if(prevValueTypeAndShow.getRole().equals(RDFResourceRole.property)){
						//the previous value was property, now it has a different role, so replace the old 
						// one with the new one
						ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, role);
						propertyMap.put(value.stringValue(), valueTypeAndShow);
					}
				} else{
					//the property map did not have a previous value, so add this one without any checking
					ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, role);
					propertyMap.put(value.stringValue(), valueTypeAndShow);
				}
			} else{
				//it is a concept, a conceptScheme or a collection, just add it to the otherMap
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) value, role);
				otherResourcesMap.put(value.stringValue(), valueTypeAndShow);
			}
			
			if(tupleBindings.hasBinding("show")){
				Value showRes = tupleBindings.getBinding("show").getValue();
				if(showRes instanceof Literal){
					//check if the show belong to a property or to another type
					if(isProp){
						if(!propertyMap.get(value.stringValue()).hasShowValue((Literal) showRes)) {
							propertyMap.get(value.stringValue()).addShow((Literal) showRes);
						}
					} else{
						//is not a property
						if(!otherResourcesMap.get(value.stringValue()).hasShowValue((Literal) showRes)){
							otherResourcesMap.get(value.stringValue()).addShow((Literal) showRes);
						}
					}
				}
			}
		}
		
		//now iterate over the 2 maps and construct the responses
		for(String key : otherResourcesMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = otherResourcesMap.get(key);
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(valueTypeAndShow.getResource());
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("role", valueTypeAndShow.getRole().name());
			if(valueTypeAndShow.isShowPresent()){
				annotatedValue.setAttribute("show", valueTypeAndShow.getShowAsString());
			} 
			results.add(annotatedValue);
		}
		for(String key : propertyMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = propertyMap.get(key);
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(valueTypeAndShow.getResource());
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("role", valueTypeAndShow.getRole().name());
			if(valueTypeAndShow.isShowPresent() ){
				annotatedValue.setAttribute("show", valueTypeAndShow.getShowAsString());
			} 
			results.add(annotatedValue);
		}
		return results;
	}
	
	
	private class ValueTypeAndShow{
		IRI resource  = null;
		//String show = null;
		List<Literal> showList = null;
		RDFResourceRole role = null;
		
		public ValueTypeAndShow(IRI resource, RDFResourceRole role) {
			this.resource = resource;
			this.role = role;
			this.showList = new ArrayList<Literal>();
		}
		
		public void addShow(Literal show){
			if(!showList.contains(show)){
				showList.add(show);
			}
		}
		
		public void addShowList(List<Literal> showList){
			for(Literal literal : showList){
				if(!this.showList.contains(literal)){
					this.showList.add(literal);
				}
			}
		}

		public IRI getResource() {
			return resource;
		}

		
		public boolean hasShowValue(Literal show){
			return showList.contains(show);
		}
		
		public List<Literal> getShowList() {
			return showList;
		}
		
		public String getShowAsString(){
			boolean first = true;
			String showAsString = "";
			for(Literal literal : showList){
				if(!first){
					showAsString+=", ";
				}
				showAsString+=literal.getLabel();
				if(literal.getLanguage().isPresent()){
					showAsString+=" ("+literal.getLanguage().get()+")";
				}
				first = false;
			}
			return showAsString;
		}
		

		public RDFResourceRole getRole() {
			return role;
		}
		
		public boolean isShowPresent(){
			if(!showList.isEmpty()){
				return true;
			}
			return false;
		}
		
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(IRI cls, String searchString, boolean useLocalName, 
			boolean useURI, String searchMode, @Optional String lang) throws IllegalStateException, STPropertyAccessException {
		
		//it can be null, * or a list of languages
		String languagesPropValue = STPropertiesManager.getProjectPreference(
			       STPropertiesManager.PROP_LANGUAGES, getProject(), UsersManager.getLoggedUser(), RenderingEngine.class.getName());
		String [] langArray;
		if(languagesPropValue == null){
			langArray = new String[]{"*"};
		} else{
			langArray = languagesPropValue.split(",");
		}
		
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
		query+="\nSELECT DISTINCT ?resource ?type" +
			"\nWHERE{" + 
			"\n ?resource a <"+cls.stringValue()+"> . " +
			"\n ?resource a ?type . " +
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

		
		//add the show part in SPARQL query
		query+=addShowPart("?show", langArray);
		
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
		//List<String> addedIndividualList = new ArrayList<String>();
		Map<String, ValueTypeAndShow> individualsMap = new HashMap<String, ValueTypeAndShow>();
		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			Value resourceURI = bindingSet.getValue("resource");

			if (!(resourceURI instanceof IRI)) {
				continue;
			}
			
			if(!individualsMap.containsKey(resourceURI.stringValue())){
				String type = bindingSet.getBinding("type").getValue().stringValue();
				ValueTypeAndShow valueTypeAndShow = new ValueTypeAndShow((IRI) resourceURI, getRoleFromType(type));
				individualsMap.put(resourceURI.stringValue(), valueTypeAndShow);
			}
			
			if(bindingSet.hasBinding("show")){
				Value showRes = bindingSet.getValue("show");
				if(showRes instanceof Literal){
					if(!individualsMap.get(resourceURI.stringValue()).hasShowValue((Literal) showRes)){
						individualsMap.get(resourceURI.stringValue()).addShow((Literal) showRes);
					}
				}
			}
		}
		
		for(String key : individualsMap.keySet()){
			ValueTypeAndShow valueTypeAndShow = individualsMap.get(key);
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(valueTypeAndShow.getResource());
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("role", valueTypeAndShow.getRole().name());
			if(valueTypeAndShow.isShowPresent()){
				annotatedValue.setAttribute("show", valueTypeAndShow.getShowAsString());
			} 
			results.add(annotatedValue);
		}
		
		
		return results;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resourceURI)+ ')', 'R')")
	public Collection<AnnotatedValue<Resource>> getPathFromRoot(String role, IRI resourceURI, @Optional List<IRI> schemesIRI)
			throws InvalidParameterException{
		
		//ARTURIResource inputResource = owlModel.createURIResource(resourceURI);
		
		String query = null;
		String superResourceVar = null, superSuperResourceVar = null;
		if(role.toLowerCase().equals(RDFResourceRole.concept.name().toLowerCase())){
			superResourceVar = "broader";
			superSuperResourceVar = "broaderOfBroader";
			String inSchemeOrTopConcept = "<"+SKOS.IN_SCHEME.stringValue()+">|<"+SKOS.TOP_CONCEPT_OF+">";
			//@formatter:off
			query = "SELECT DISTINCT ?broader ?broaderOfBroader ?isTopConcept ?isTop" + 
					"\nWHERE{" +
					"\n{" + 
					"\n<" + resourceURI.stringValue() + "> (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+"> )+ ?broader .";
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> ."+
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) <"+schemesIRI.get(0).stringValue()+"> ." +
						"\n}";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query += "\n?broader " + inSchemeOrTopConcept + " ?scheme1 ."+
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
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> . ";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " ?scheme3 . "+
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
		} else if(role.toLowerCase().equals(RDFResourceRole.property.name().toLowerCase())){
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
		} else if(role.toLowerCase().equals(RDFResourceRole.cls.name().toLowerCase())) {
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
		} else if(role.toLowerCase().equals(RDFResourceRole.skosCollection.name().toLowerCase())){
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
			if(role.toLowerCase().equals(RDFResourceRole.concept.name())){
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
		// result from the SPARQL query (this mean that all the paths contained at least one non-URI resource)
		if(isTopResource || (pathList.isEmpty() && !resourceToResourceForHierarchyMap.isEmpty() )){
			//the input resource is a top resource for its role (concept, class or property)
			pathFound = true;
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>((IRI)resourceURI);
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("show", resourceURI.getLocalName());
			results.add(annotatedValue);
		}
		
		
		for(int currentLength=1; currentLength<=maxLength && !pathFound; ++currentLength){
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
	
	private String addShowPart(String variable, String[] langArray){
		//according to the Lexicalization Model, prepare the show considering one of the following properties:
		// - rdfs:label (Project.RDFS_LEXICALIZATION_MODEL)
		// - skos:prefLabel (Project.SKOS_LEXICALIZATION_MODEL)
		// - skosxl:prefLabel -> skosxl:literalForm (Project.SKOSXL_LEXICALIZATION_MODEL)
		IRI lexModel = getProject().getLexicalizationModel();
		String query="\nOPTIONAL" +
				"\n{";
		if(lexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)){
			query+="\n?resource "+NTriplesUtil.toNTriplesString(RDFS.LABEL)+" ?show .";
		} else if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			query+="\n?resource "+NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL)+" ?show .";
		} else if(lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)){
			query+="\n?resource "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?skosPrefLabel ." +
					"\n?skosPrefLabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) +" ?show .";
		}
		query+=filterAccordingToLanguage("?show", langArray) +
				"\n}";
		
		return query;
	}
	
	
	private String filterAccordingToLanguage(String variable, String[] languages){
		String query = "";
		if(languages.length==1 && languages[0].equals("*")){
			//all languages are selected, so no filter should apply, do nothing
		} else{
			query = "FILTER(";
			boolean first = true;
			for(String lang : languages){
				//iterate over the languages
				if(!first){
					query += " || ";
				} query += "lang("+variable+") = \""+lang+"\"";
				first = false;
			}
			query += ")";
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
	
	private RDFResourceRole getRoleFromType(String typeURI){
		RDFResourceRole role;
		if(typeURI.equals(OWL.CLASS.stringValue()) || typeURI.equals(RDFS.CLASS.stringValue()) ){
			role = RDFResourceRole.cls;
		} else if(typeURI.equals(RDF.PROPERTY.stringValue())){
			role = RDFResourceRole.property;
		} else if(typeURI.equals(OWL.OBJECTPROPERTY.stringValue())){
			role = RDFResourceRole.objectProperty;
		} else if(typeURI.equals(OWL.DATATYPEPROPERTY.stringValue())){
			role = RDFResourceRole.datatypeProperty;
		} else if(typeURI.equals(OWL.ANNOTATIONPROPERTY.stringValue())){
			role = RDFResourceRole.annotationProperty;
		} else if(typeURI.equals(OWL.ONTOLOGYPROPERTY.stringValue())){
			role = RDFResourceRole.ontologyProperty;
		}  else if(typeURI.equals(SKOS.CONCEPT.stringValue())){
			role = RDFResourceRole.concept;
		} else if(typeURI.equals(SKOS.COLLECTION.stringValue())){
			role = RDFResourceRole.skosCollection;
		} else if(typeURI.equals(SKOS.ORDERED_COLLECTION.stringValue())){
			role = RDFResourceRole.skosOrderedCollection;
		} else if(typeURI.equals(SKOSXL.LABEL.stringValue())){
			role = RDFResourceRole.xLabel;
		} else if(typeURI.equals(SKOS.CONCEPT_SCHEME.stringValue())){
			role = RDFResourceRole.conceptScheme;
		} else {
			role = RDFResourceRole.individual;
		} 
		
		return role;
	}
	
}
