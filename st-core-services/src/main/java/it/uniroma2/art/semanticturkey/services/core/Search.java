package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.model.ARTNode;
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
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
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
public class Search extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Search.class);
	
//	private static String CLASS_ROLE = "class";
//	private static String CONCEPT_ROLE = "concept";
//	private static String INSTANCE_ROLE = "instance";
	
	private static String START_SEARCH_MODE = "start";
	private static String CONTAINS_SEARCH_MODE = "contain";
	private static String END_SEARCH_MODE = "end";
	private static String EXACT_SEARCH_MODE = "exact";
	
	@GenerateSTServiceController
	public Response searchResource(String searchString, String [] rolesArray, boolean useLocalName,
			String searchMode, @Optional String lang, @Optional ARTURIResource scheme) throws ModelUpdateException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException, QueryEvaluationException {
		
		boolean isClassWanted = false;
		boolean isConceptWanted = false;
		boolean isInstanceWanted = false;
		boolean isPropertyWanted = false;
		
		String searchModeSelected = null;
		
		
		
		
		if(searchString.isEmpty()){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			Element dataElement = response.getDataElement();
			dataElement.setTextContent("the serchString cannot be empty");
			return response;
		}
		
		for(int i=0; i<rolesArray.length; ++i){
			if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.cls.name())){
				isClassWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.concept.name())){
				isConceptWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.individual.name())){
				isInstanceWanted = true;
			} else if(rolesArray[i].toLowerCase().equals(RDFResourceRolesEnum.property.name())){
				isPropertyWanted = true;
			}
		}
		//@formatter:off
		if(!isClassWanted && !isConceptWanted && !isInstanceWanted && !isPropertyWanted){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.fail);
			Element dataElement = response.getDataElement();
			dataElement.setTextContent("the serch roles should be at least one of: "+
					RDFResourceRolesEnum.cls.name()+", "+
					RDFResourceRolesEnum.concept.name()+", "+
					RDFResourceRolesEnum.individual+" or "+
					RDFResourceRolesEnum.property.name());
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
			Element dataElement = response.getDataElement();
			dataElement.setTextContent("the serch mode should be at one of: "+START_SEARCH_MODE+", "+
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
				isPropertyWanted, isConceptWanted, scheme);
		
		
		query+="\n}" +
			"\n}";
			
		
		//now examine the rdf:label and/or skos:xlabel/skosxl:label
		//see if the localName should be used in the query or not
		if(useLocalName){
			query+="\n{" +
					"\nBIND(REPLACE(str(?resource), '^.*(#|/)', \"\") AS ?localName)"+
					searchModePrepareQuery("?localName", searchString, searchModeSelected) +
					"\n}"+
					"\nUNION";
		}
		
		//search in the rdfs:label
		query+="\n{" +
				"\n?resource <"+RDFS.LABEL+"> ?rdfsLabel ." +
				searchModePrepareQuery("?rdfsLabel", searchString, searchModeSelected) +
				"\n}";

		//if you are searching among concepts, search in the skos:prefLabel/altLabel and 
		// skosxl:prefLabel/altLabel
		
		if(isConceptWanted){
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
		if(lang!=null && lang.length()>0){
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
		// Element collectionElem = XMLHelp.newElement(dataElement, "collection");
		Collection<STRDFURI> collection = STRDFNodeFactory.createEmptyURICollection();
		List<String> addedIndividualList = new ArrayList<String>();
		while (tupleBindingsIterator.hasNext()) {
			TupleBindings tupleBindings = tupleBindingsIterator.next();
			ARTNode resourceURI = tupleBindings.getBinding("resource").getBoundValue();

			if (!resourceURI.isURIResource()) {
				continue;
			}

			// TODO, explicit set to true
			RDFResourceRolesEnum role = null;
			//since there are more than one element in the input role array, see the resonce
			String type = tupleBindings.getBinding("type").getBoundValue().getNominalValue();
			if(type.equals(OWL.CLASS)){
				role = RDFResourceRolesEnum.cls;
			} else if(type.equals(RDF.PROPERTY)){
				role = RDFResourceRolesEnum.property;
			} else if(type.equals(OWL.OBJECTPROPERTY)){
				role = RDFResourceRolesEnum.objectProperty;
			} else if(type.equals(OWL.DATATYPEPROPERTY)){
				role = RDFResourceRolesEnum.datatypeProperty;
			} else if(type.equals(OWL.ANNOTATIONPROPERTY)){
				role = RDFResourceRolesEnum.annotationProperty;
			} else if(type.equals(OWL.ONTOLOGYPROPERTY)){
				role = RDFResourceRolesEnum.ontologyProperty;
			}  else if(type.equals(SKOS.CONCEPT)){
				role = RDFResourceRolesEnum.concept;
			} else{
				role = RDFResourceRolesEnum.individual;
				if(addedIndividualList.contains(resourceURI.asURIResource().getNominalValue())){
					//the individual was already added
					continue;
				}
				addedIndividualList.add(resourceURI.asURIResource().getNominalValue());
			} 
			
			
			
			//if a show was found, use it instead of the automatically retrieve one (the qnome)
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
			
			/*if(show!= null && show.length()>0){
				collection.add(STRDFNodeFactory.createSTRDFURI(resourceURI.asURIResource(), role, 
						true, show));
			} else {
				collection.add(STRDFNodeFactory.createSTRDFURI(owlModel, resourceURI.asURIResource(), role, 
						true, true));
			}*/
			
			
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
			if(role.toLowerCase().equals(RDFResourceRolesEnum.concept.name())){
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
						"\n{" +
						"\n<" + resourceURI + "> a <"+SKOS.CONCEPT+"> .";
				if(schemeURI != null){
						query+="\n<"+resourceURI+"> " +
								"(<"+SKOS.TOPCONCEPTOF+"> | ^<"+SKOS.HASTOPCONCEPT+">) <"+schemeURI+">";
				} else{
					query+="\n<"+resourceURI+"> " +
							"(<"+SKOS.TOPCONCEPTOF+"> | ^<"+SKOS.HASTOPCONCEPT+">) _:b1";
				}
				query+="\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";
				//@formatter:on
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.property.name())){
				superResourceVar = "superProperty";
				superSuperResourceVar = "superSuperProperty";
				//@formatter:off
				query = "SELECT DISTINCT ?superProperty ?superSuperProperty ?isTop" + 
						"\nWHERE{" + 
						"\n{" + 
						"\n<" + resourceURI + "> <" + RDFS.SUBPROPERTYOF + ">+ ?superProperty .";
				query += "\nOPTIONAL{" +
						"\n?superProperty <" + RDFS.SUBPROPERTYOF + "> ?superSuperProperty .";
				query +="\n}" + 
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
			} else if(role.toLowerCase().equals(RDFResourceRolesEnum.cls.name())) {
				superResourceVar = "superClass";
				superSuperResourceVar = "superSuperClass";
				//@formatter:off
				query = "SELECT DISTINCT ?superClass ?superSuperClass ?isTop" + 
						"\nWHERE{" + 
						"\n{" + 
						"\n<" + resourceURI + "> <" + RDFS.SUBCLASSOF + ">+ ?superClass .";
				query += "\nOPTIONAL{" +
						"\n?superClass <" + RDFS.SUBCLASSOF + "> ?superSuperClass .";
				query +="\n}" + 
						"\n}" +
						"\nUNION" +
						"\n{" +
						"\n<"+resourceURI+"> a <"+OWL.CLASS+">." +
						"\nFILTER NOT EXISTS{<"+resourceURI+"> <"+RDFS.SUBCLASSOF+"> _:b1}" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}" +
						"\n}";;
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
					ARTURIResource superResource = tupleBindings.getBinding(superResourceVar).getBoundValue()
							.asURIResource();
					String superResourceURI = superResource.getURI();
					String superResourceShow = superResource.getLocalName();
					ARTURIResource superSuperResource = null;
					String superSuperResourceURI = null;
					String superSuperResourceShow = null;
					if (tupleBindings.hasBinding(superSuperResourceVar)) {
						superSuperResource = tupleBindings.getBinding(superSuperResourceVar).getBoundValue()
								.asURIResource();
						superSuperResourceURI = superSuperResource.getURI();
						superSuperResourceShow = superSuperResource.getLocalName();
					}
					if (!resourceToResourceForHierarchyMap.containsKey(superResourceURI)) {
						resourceToResourceForHierarchyMap.put(superResourceURI, new ResourceForHierarchy(
								superResourceURI, superResourceShow));
					}
					if (!tupleBindings.hasBinding("isTopConcept")) { // use only for concept
						resourceToResourceForHierarchyMap.get(superResourceURI).setTopConcept(false);
					}
					
					if (superSuperResource != null) {
						if (!resourceToResourceForHierarchyMap.containsKey(superSuperResourceURI)) {
							resourceToResourceForHierarchyMap.put(superSuperResourceURI, 
									new ResourceForHierarchy(superSuperResourceURI, superSuperResourceShow));
						}
						ResourceForHierarchy resourceForHierarchy = resourceToResourceForHierarchyMap
								.get(superSuperResourceURI);
						resourceForHierarchy.addSubResource(superResourceURI);
						
						resourceToResourceForHierarchyMap.get(superResourceURI).setHasNoSuperResource(false);
					}
				}
				if(tupleBindings.hasBinding("isTop")){
					isTopResource = true;
				}
				
			}
			iter.close();
			
			
			//itertate over the resoruceToResourceForHierarchyMap and look for the topConcept
			//and construct a list of list containg all the possible paths
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
			//to order the path (from the shortest to the longest) first find the maximum lenght
			int maxLength = -1;
			for(List<String> path : pathList){
				int currentLength = path.size();
				if(maxLength==-1 || maxLength<currentLength){
					maxLength = currentLength;
				}
			}
			Element pathCollection = XMLHelp.newElement(dataElement, "collection");
			if(isTopResource){
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
	
	
	private String addFilterForRsourseType(String variable, boolean isClassWanted, 
			boolean isInstanceWanted, boolean isPropertyWanted, boolean isConceptWanted) {
		boolean otherWanted = false;
		String filterQuery = "\nFILTER( ";
		if(isClassWanted){
			filterQuery += variable+" = <"+OWL.CLASS+">"; 
			otherWanted = true;
		}
		if(isPropertyWanted){
			if(otherWanted){
				filterQuery += " || ";
			}
			otherWanted = true;
			//@formatter:off
			filterQuery += variable+ " = <"+RDF.PROPERTY+"> || "+
					variable+" = <"+OWL.OBJECTPROPERTY+"> || "+
					variable+" = <"+OWL.DATATYPEPROPERTY+"> || "+
					variable+" = <"+OWL.ANNOTATIONPROPERTY+"> || " +
					variable+" = <"+OWL.ONTOLOGYPROPERTY+"> ";
			//@formatter:on
		}
		if(isConceptWanted){
			if(otherWanted){
				filterQuery += " || ";
			}
			otherWanted = true;
			filterQuery += variable+" = <"+SKOS.CONCEPT+">";
		}
		if(isInstanceWanted){
			if(otherWanted){
				filterQuery += " || ( ";
			}
			//@formatter:off
			filterQuery+="EXISTS{"+variable+" a <"+OWL.CLASS+">}";
			
			//old version
			/*filterQuery+=variable+"!= <"+OWL.CLASS+"> && "+
					variable+"!=<"+RDFS.CLASS+"> && "+
					variable+"!=<"+RDFS.RESOURCE+"> && "+
					variable+"!=<"+RDF.PROPERTY+"> && "+
					variable+"!=<"+OWL.OBJECTPROPERTY+"> && "+
					variable+"!=<"+OWL.DATATYPEPROPERTY+"> && "+
					variable+"!=<"+OWL.ANNOTATIONPROPERTY+"> && "+
					variable+"!=<"+OWL.ONTOLOGYPROPERTY+"> && "+
					variable+"!=<"+SKOS.CONCEPT+"> && "+
					variable+"!=<"+SKOS.CONCEPTSCHEME+"> && "+
					variable+"!=<"+SKOSXL.LABEL+">";*/
			//@formatter:on
			if(otherWanted){
				filterQuery += " ) ";
			}
			otherWanted = true;
		}
		
		filterQuery += ")";
		return filterQuery;
	}
	
	
	private String filterResourceTypeAndScheme(String resource, String type, boolean isClassWanted, 
			boolean isInstanceWanted, boolean isPropertyWanted, boolean isConceptWanted, 
			ARTURIResource scheme){
		boolean otherWanted = false;
		String filterQuery = "";
		
		if(isClassWanted){
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\nFILTER("+type+" = <"+OWL.CLASS+">)" +
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
		if(isInstanceWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+resource+" a "+type+" . " +
					"\n?type a ?classType ." +
					"FILTER (EXISTS{?classType a <"+OWL.CLASS+">})"+
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
		
		public ResourceForHierarchy(String resource, String show) {
			this.resource = resource;
			this.show = show;
			isTopConcept = true;
			hasNoSuperResource = true;
			subResourcesList = new ArrayList<String>();
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
	
	
}
