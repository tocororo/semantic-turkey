package it.uniroma2.art.semanticturkey.services.core.search;

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
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.user.UsersManager;

public class ServiceForSearches {

	
	final static public String START_SEARCH_MODE = "start";
	final static public String CONTAINS_SEARCH_MODE = "contain";
	final static public String END_SEARCH_MODE = "end";
	final static public String EXACT_SEARCH_MODE = "exact";
	
	private boolean isClassWanted = false;
	private boolean isConceptWanted = false;
	private boolean isConceptSchemeWanted = false;
	private boolean isInstanceWanted = false;
	private boolean isPropertyWanted = false;
	private boolean isCollectionWanted = false;
	
	String [] langArray;
	
	public boolean isClassWanted() {
		return isClassWanted;
	}

	public boolean isConceptWanted() {
		return isConceptWanted;
	}

	public boolean isConceptSchemeWanted() {
		return isConceptSchemeWanted;
	}

	public boolean isInstanceWanted() {
		return isInstanceWanted;
	}

	public boolean isPropertyWanted() {
		return isPropertyWanted;
	}

	public boolean isCollectionWanted() {
		return isCollectionWanted;
	}

	public String [] getLangArray(){
		return langArray;
	}
	
	public String filterResourceTypeAndScheme(String resource, String type, boolean isClassWanted, 
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
	
	public static String addShowPart(String variable, String[] langArray, Project project){
		//according to the Lexicalization Model, prepare the show considering one of the following properties:
		// - rdfs:label (Project.RDFS_LEXICALIZATION_MODEL)
		// - skos:prefLabel (Project.SKOS_LEXICALIZATION_MODEL)
		// - skosxl:prefLabel -> skosxl:literalForm (Project.SKOSXL_LEXICALIZATION_MODEL)
		IRI lexModel = project.getLexicalizationModel();
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
	

	private static String filterAccordingToLanguage(String variable, String[] languages){
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
	
	public static String filterWithOrValues(List<IRI> IRIList, String variable){
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
			schemesInFilter+=variable+"="+NTriplesUtil.toNTriplesString(iri);
		}
		schemesInFilter+= ") \n";
		return schemesInFilter;
	}
	
	public String checksPreQuery(String searchString, String [] rolesArray, String searchMode, 
			Project project) throws IllegalStateException, STPropertyAccessException{
		//it can be null, * or a list of languages
		String languagesPropValue = STPropertiesManager.getProjectPreference(
			       STPropertiesManager.PREF_LANGUAGES, project, UsersManager.getLoggedUser(), RenderingEngine.class.getName());
		if(languagesPropValue == null){
			langArray = new String[]{"*"};
		} else{
			langArray = languagesPropValue.split(",");
		}
		
		String searchModeSelected = null;
		
		if(searchString.isEmpty()){
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException("the serchString cannot be empty");
		}
		
		if(rolesArray!=null){
			for(int i=0; i<rolesArray.length; ++i){
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
		}
		//@formatter:off
		if(rolesArray!= null && !isClassWanted && !isConceptWanted && !isConceptSchemeWanted && 
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
		
		return searchModeSelected;
	}
	
	public Collection<AnnotatedValue<Resource>> executeGenericSearchQuery(String query, Resource[] namedGraphs,
			RepositoryConnection repositoryConnection){
		
		Collection<AnnotatedValue<Resource>> results = new ArrayList<AnnotatedValue<Resource>>();
		
		TupleQuery tupleQuery;
		tupleQuery = repositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
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
	
	public Collection<String> executeGenericSearchQueryForStringList(String query, Resource[] namedGraphs,
			RepositoryConnection repositoryConnection){
		Collection<String> results = new ArrayList<>();
		
		TupleQuery tupleQuery;
		tupleQuery = repositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		for(Resource namedGraph : namedGraphs){
			if(namedGraph instanceof IRI){
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);
		
		TupleQueryResult tupleBindingsIterator = tupleQuery.evaluate();
		
		while (tupleBindingsIterator.hasNext()) {
			//if it has the value for the variable label, take that value and ignore the value for
			// resource, otherwise take the value for resource
			BindingSet tupleBindings = tupleBindingsIterator.next();
			String result = null;
			if(tupleBindings.hasBinding("label")){
				Literal label = (Literal) tupleBindings.getBinding("label").getValue();
				result = label.getLabel();
				if(label.getLanguage().isPresent()){
					result += label.getLanguage().get();
				}
			} else{
				Value value = tupleBindings.getBinding("resource").getValue();
				if(value instanceof IRI){
					result = ((IRI)value).getLocalName();
				}
			}
			if(result!= null && !results.contains(result)){
				results.add(result);
			}
		}
		
		return results;
	}
	
	public Collection<AnnotatedValue<Resource>> executeInstancesSearchQuery(String query, Resource[] namedGraphs,
			RepositoryConnection repositoryConnection ){
		Collection<AnnotatedValue<Resource>> results = new ArrayList<AnnotatedValue<Resource>>();
		
		TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
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
}

