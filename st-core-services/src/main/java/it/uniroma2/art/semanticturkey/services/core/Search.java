package it.uniroma2.art.semanticturkey.services.core;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.search.ServiceForSearches;

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
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, searchMode, 
				getProject());
		
		//create the query to be executed for the search
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?type ?show"+ 
			"\nWHERE{"; // +
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);
		
		//now examine the rdfs:label and/or skos:xlabel/skosxl:label
		//see if the localName and/or URI should be used in the query or not
		
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		
		//check if the request want to search in the complete URI
		if(useURI){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the completeURI is not computed
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
				ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), getProject())+
				"\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		
		return serviceForSearches.executeGenericSearchQuery(query, getUserNamedGraphs(), getManagedConnection());
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchStringList(String searchString, @Optional String [] rolesArray,  boolean useLocalName,
			String searchMode, @Optional List<IRI> schemes) throws IllegalStateException, STPropertyAccessException  {
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, 
				searchMode, getProject());
		
		//@formatter:off
		String query = "SELECT DISTINCT ?resource ?label"+ 
			"\nWHERE{";
		
		//get the candidate resources
		query+=serviceForSearches.filterResourceTypeAndScheme("?resource", "?type", serviceForSearches.isClassWanted(), 
				serviceForSearches.isInstanceWanted(), serviceForSearches.isPropertyWanted(), 
				serviceForSearches.isConceptWanted(), serviceForSearches.isConceptSchemeWanted(), 
				serviceForSearches.isCollectionWanted(), schemes);
		
		//check if the request want to search in the local name
		if(useLocalName){
			query+="\n{" +
					"\n?resource a ?type . " + // otherwise the localName is not computed
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		//if the user specify a role, then get the resource associated to the label, since it will be use 
		// later to filter the results
		if(rolesArray!=null && rolesArray.length>0){
			//search in the rdfs:label
			query+="\n{" +
					"\n?resource <"+RDFS.LABEL+"> ?label ." +
					searchModePrepareQuery("?label", searchString, searchModeSelected) +
					"\n}"+
			//search in skos:prefLabel and skos:altLabel
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOS.PREF_LABEL.stringValue()+"> | <"+SKOS.ALT_LABEL.stringValue()+">) ?label ."+
					searchModePrepareQuery("?label", searchString, searchModeSelected) +
					"\n}" +
			//search in skosxl:prefLabel->skosxl:literalForm and skosxl:altLabel->skosxl:literalForm
					"\nUNION" +
					"\n{" +
					"\n?resource (<"+SKOSXL.PREF_LABEL.stringValue()+"> | <"+SKOSXL.ALT_LABEL.stringValue()+">) ?skosxlLabel ." +
					"\n?skosxlLabel <"+SKOSXL.LITERAL_FORM.stringValue()+"> ?label ." +
					searchModePrepareQuery("?label", searchString, searchModeSelected) +
					"\n}";		
		}
		
		query+="\n}";
		//@formatter:on

		logger.debug("query = "+query);
		
		return serviceForSearches.executeGenericSearchQueryForStringList(query, getUserNamedGraphs(), getManagedConnection());
	}
	
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(IRI cls, String searchString, boolean useLocalName, 
			boolean useURI, String searchMode, @Optional String lang) throws IllegalStateException, STPropertyAccessException {
		
		ServiceForSearches serviceForSearches = new ServiceForSearches();
		
		String []rolesArray = {RDFResourceRole.individual.name()};
		String searchModeSelected = serviceForSearches.checksPreQuery(searchString, rolesArray, 
				searchMode, getProject());
		
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
		query+=ServiceForSearches.addShowPart("?show", serviceForSearches.getLangArray(), getProject());
		
		query+="\n}";
		//@formatter:on
		
		logger.debug("query = "+query);
		
		return serviceForSearches.executeInstancesSearchQuery(query, getUserNamedGraphs(), getManagedConnection());
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
			} else if(schemesIRI != null &&schemesIRI.size()>1){
				query += "\n?broader " + inSchemeOrTopConcept + " ?scheme1 ."+
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme1") +
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) ?scheme2 ." +
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme2") +
						"\n}";
			}
			else if(schemesIRI==null || schemesIRI.size()==0) { //the schemes is wither null or an empty list
				//check if the selected broader has no brother itself, in this case it is consider a topConcept
				query +="\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\nFILTER NOT EXISTS{ " +
						"\n?broader (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+">) ?broaderOfBroader ."+
						"}"+
						"\n}";
			}
			query += "\nOPTIONAL{" +
					"\n?broader (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+">) ?broaderOfBroader .";
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> . ";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " ?scheme3 . "+
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme3");
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
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme4");
			} else{
				query+="\n<"+resourceURI.stringValue()+"> " +
						"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) _:b1";
			}
			query+="\nBIND(\"true\" AS ?isTop )" +
					"\n}";
					
			// this using, used only when no scheme is selected, is used when the concept does not have any
			// broader and it is not topConcept of any scheme
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
				//the role is a concept, so check if an input scheme was passed, if so, if it is not a 
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
	
	
	
}
