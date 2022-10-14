package it.uniroma2.art.semanticturkey.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy.StatusFilter;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.TripleForSearch;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

public class ServiceForSearches {

	
//	final static public String START_SEARCH_MODE = "startsWith";
//	final static public String CONTAINS_SEARCH_MODE = "contains";
//	final static public String END_SEARCH_MODE = "endsWith";
//	final static public String EXACT_SEARCH_MODE = "exact";
	
	private boolean isClassWanted = false;
	private boolean isConceptWanted = false;
	private boolean isConceptSchemeWanted = false;
	private boolean isInstanceWanted = false;
	private boolean isPropertyWanted = false;
	private boolean isCollectionWanted = false;
	private boolean isDataRangeWanted = false;
	private boolean isLexiconWanted = false;
	private boolean isLexicalEntryWanted = false;
	
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
	
	public boolean isDataRagenWanted() {
		return isDataRangeWanted;
	}
	
	public boolean isLexiconWanted() {
		return isLexiconWanted;
	}
	
	public boolean isLexicalEntryWanted() {
		return isLexicalEntryWanted;
	}

	
	public String filterResourceTypeAndSchemeAndLexicons(String varResource, String varType, 
			List<IRI> schemes, String schemeFilter, IRI cls, List<IRI> lexicons){
		boolean otherWanted = false;
		String filterQuery = "";
		// @formatter:off
		if(isClassWanted){
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					"\nFILTER("+varType+" = "+NTriplesUtil.toNTriplesString(OWL.CLASS)+" || " +
							varType+" = "+NTriplesUtil.toNTriplesString(RDFS.CLASS)+" )" +
					"\n}";
			
			otherWanted = true;
		}
		if(isPropertyWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					"\n"+varType + " "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "+NTriplesUtil.toNTriplesString(RDF.PROPERTY)+" . " +
					"\n}";

			/*filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					"\nFILTER("+varType+ " = <"+RDF.PROPERTY.stringValue()+"> || "+
					varType+" = <"+OWL.OBJECTPROPERTY.stringValue()+"> || "+
					varType+" = <"+OWL.DATATYPEPROPERTY.stringValue()+"> || "+
					varType+" = <"+OWL.ANNOTATIONPROPERTY.stringValue()+"> || " +
					varType+" = <"+OWL.ONTOLOGYPROPERTY.stringValue()+"> )"+
					"\n}";
			*/

			otherWanted = true;
		}
		if(isConceptWanted){
			String schemeOrTopConcept="("+NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME)+"|"+NTriplesUtil.toNTriplesString(SKOS.TOP_CONCEPT_OF)+"|"
					+ "^"+NTriplesUtil.toNTriplesString(SKOS.HAS_TOP_CONCEPT)+")";
			
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					//consider the classes that are subclasses of SKOS.CONCEPT
						"\n"+varType+" "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "
								+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+" .";
					
					 //"\nFILTER("+varType+" = <"+SKOS.CONCEPT.stringValue()+">)";
			if(schemes!=null && schemes.size()==1){
				filterQuery += "\n"+varResource+" "+schemeOrTopConcept+" <"+schemes.get(0).stringValue()+"> ."+
						"\n"+varResource+" "+schemeOrTopConcept+" ?scheme .";
			} else if(schemes!=null && schemes.size()>1){
				if(schemeFilter.equals("and")){
					for(IRI scheme : schemes) {
						filterQuery += "\n" + varResource + " " + schemeOrTopConcept + " " + NTriplesUtil.toNTriplesString(scheme) + " .";
					}
				}
				//this part is included also in the "and" case, because the query needs the ?scheme variable
				filterQuery += "\n" + varResource + " " + schemeOrTopConcept + " ?scheme . " +
						filterWithOrValues(schemes, "?scheme");
			} else{ // schemes!=null
				//since no scheme restriction is passed, then also concepts not belonging to any concepts 
				//should be returned (this means to put the triples in an OPTIONAL) 
				filterQuery +="\nOPTIONAL{"+varResource+" "+schemeOrTopConcept+" ?scheme . }";
			}
			
			filterQuery += "\n}";
			
			otherWanted = true;
		}
		if(isConceptSchemeWanted){
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					//consider the classes that are subclasses of SKOS.CONCEPT_SCHEME
					"\n"+varType+" "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "
							+NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME)+" ." +
							//"\nFILTER("+varType+" = <"+SKOS.CONCEPT_SCHEME.stringValue()+">)";
							"\n}";
			
			otherWanted = true;
		}
		if(isCollectionWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					 "\nFILTER("+varType+" = <"+SKOS.COLLECTION.stringValue()+"> || " +
					 		 varType+" = <"+SKOS.ORDERED_COLLECTION.stringValue()+"> )" +
					 "\n}";
			
			otherWanted = true;
		}
		if(isInstanceWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			if(otherWanted || cls==null ) {
				filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
						//"\n?type a <"+OWL.CLASS.stringValue()+"> . "+
						"\nFILTER("+varType+" != owl:Class && "+varType+" != rdfs:Class)" +
						"\n}";
				
				otherWanted = true;
			} else {
				//since only individuals are wanted and cls is not null, filter the individuals just for the 
				// desired class
				filterQuery += "\n{\n"+varResource+" a <"+cls.stringValue()+"> . } ";
				otherWanted = true;
			}
		}
		if(isDataRangeWanted) {
			if(otherWanted){
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					//consider the classes that are subclasses of RDFS.DATATYPE
					"\n"+varType+" "+ NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "
							+NTriplesUtil.toNTriplesString(RDFS.DATATYPE)+" ." +
							"\n}";
			
			otherWanted = true;
		}
		if(isLexiconWanted) {
			if(otherWanted) {
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					//consider the classes that are subclasses of LIME.LECIXON
					"\n"+varType+" "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "
							+NTriplesUtil.toNTriplesString(LIME.LEXICON)+" ." +
					 //"\nFILTER("+varType+" = <"+LIME.LEXICON.stringValue()+">)" +
					 "\n}";
			otherWanted = true;
		}
		if(isLexicalEntryWanted) {
			if(otherWanted) {
				filterQuery += "\nUNION ";
			}
			filterQuery += "\n{\n"+varResource+" a "+varType+" . " +
					//consider the classes that are subclasses of ONTOLEX.LEXICAL_ENTRY
					"\n"+varType+" "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "
							+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICAL_ENTRY)+" ." ;
					//"\nFILTER("+varType+" = <"+ONTOLEX.LEXICAL_ENTRY.stringValue()+">)";
			if(lexicons!=null && lexicons.size()==1) {
				filterQuery+="\n<"+lexicons.get(0).stringValue()+"> <"+LIME.ENTRY.stringValue()+"> "+varResource+" .";
			} else if(lexicons!=null && lexicons.size()>1) {
				filterQuery+="\n?filterLexicon <"+LIME.ENTRY.stringValue()+"> "+varResource+" ."+
						filterWithOrValues(lexicons, "?filterLexicon");
			}
			//add the index to which this lexical entry belong to
			filterQuery+="\n"+varResource+" <"+ONTOLEX.CANONICAL_FORM.stringValue()+"> ?canonicalForm ."+
					"\n?canonicalForm <"+ONTOLEX.WRITTEN_REP+"> ?writtenRep ." +
					getFirstLetterForLiteral("?writtenRep", "?index");
			filterQuery+="\n}";
			
			otherWanted = true;
		}
		// @formatter:on
		
		return filterQuery;
	}
	
	public static String getFirstLetterForLiteral(String varInput, String varOutput) {
		String query;
		query = "\nBIND(STR(LCASE(SUBSTR("+varInput+", 1, 2))) AS "+varOutput+")";
		return query;
	}
	
	/*public static String addShowPart(String variable, String[] langArray, Project project){
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
	}*/
	

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
				} else {
					first = false;
				} 
				query += "lang("+variable+") = \""+lang+"\"";
			}
			query += ")";
		}
		return query;
	}
	
	public static String filterWithOrValues(List<IRI> iriList, String variable){
		if(!variable.startsWith("?")){
			variable+="?"+variable;
		}
		List<List<IRI>> iriListList = new ArrayList<>();
		iriListList.add(iriList);
		String queryFilter = "\nFILTER (";
		boolean first=true;
		for(IRI iri : iriList) {
			if(!first) {
				queryFilter+= " || ";
			} else {
				first = false;
			}
			queryFilter+=variable +" = "+NTriplesUtil.toNTriplesString(iri);
		}
		
		queryFilter+=")";
		
		return queryFilter;
	}
	
	
	 public static <T extends Value> String filterWithOrOfAndValues(String variable, IRI pred, List<List<T>> iriListList) {
		return filterWithOrOfAndValues(variable, NTriplesUtil.toNTriplesString(pred), iriListList);
	}
	
	public static <T extends Value> String filterWithOrOfAndValues(String variable, String predInNTForm,
			List<List<T>> valueListList) {
		String queryPart = "";
		if(valueListList==null || valueListList.size()==0) {
			//the list of list is null or empty, so just return an empty string
			return queryPart;
		}
		
		/*if(!pred.startsWith("<")) {
			pred = "<"+pred+">";
		}*/
		
		boolean first=true;
		for(List<T> andValueList : valueListList) {
			if(!first) {
				queryPart+="\nUNION";
			} else {
				first = false;
			}
			queryPart+="\n{";
			for(T value : andValueList) {
				queryPart+="\n"+variable+" "+predInNTForm+" "+NTriplesUtil.toNTriplesString(value)+" .";			
			}
			queryPart+="\n}";
			
		}
		
		return queryPart;
	}

	public static String filterWithOrOfAndPairValues(List<Pair<IRI, List<Value>>> valueListPairList,
			String variable, String suffix, boolean reverse) {
		if(!variable.startsWith("?")){
			variable+="?"+variable;
		}
		String queryPart="";
		int count = 1;
		for(Pair<IRI, List<Value>> iriListValuePair : valueListPairList) {
			String otherVar = variable+"_"+suffix+""+(count++);
			IRI pred = iriListValuePair.getFirst();
			List<Value> valueList = iriListValuePair.getSecond();
			if(reverse) {
				queryPart+="\n"+otherVar+" "+NTriplesUtil.toNTriplesString(pred)+" "+variable+" .";
			} else {
				queryPart+="\n"+variable+" "+NTriplesUtil.toNTriplesString(pred)+" "+otherVar+" .";
			}
			boolean first = true;
			queryPart+="\nFILTER(";
			for(Value value : valueList) {
				if(!first) {
					queryPart+=" || ";
				} else {
					first = false;
				}
				queryPart+=otherVar+" = "+NTriplesUtil.toNTriplesString(value);
			}
			queryPart+=")";
		}
		return queryPart;
	}
	
	public static boolean isSpecialCaseXLabel(List<List<IRI>> clsListList) {
		// check if any of the IRI in clsListList is SKOSXL.Label
		if (clsListList==null || clsListList.isEmpty()) {
			return false;
		}
		for (List<IRI> clsList : clsListList) {
			for (IRI cls : clsList) {
				if(cls.equals(SKOSXL.LABEL)) {
					return true;
				}
			}
		}

		return  false;
	}

	public static String getResourceshavingTypes(List<List<IRI>> typesListOfList, String varToUse, 
			boolean searchInSubTypes, boolean includeNonDirect) {
		String query = "\nSELECT "+varToUse
				+"\nWHERE{";

		String predForBelonging;
		if (includeNonDirect) {
			predForBelonging = "a/<http://www.w3.org/2000/01/rdf-schema#subClassOf>*";
		} else {
			predForBelonging = "a";
		}

		if (typesListOfList == null || typesListOfList.size() ==0) {
			query+="\n"+varToUse+" "+predForBelonging+" ?genericType .";
		}else if(typesListOfList.size()==1 && typesListOfList.get(0).size()==1) {
			IRI type = typesListOfList.get(0).get(0);
			if(searchInSubTypes) {
				query+="\n?genericSubType" +NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* " +NTriplesUtil.toNTriplesString(type)+" ." +
						"\n"+varToUse+" "+predForBelonging+" ?genericSubType .";
			} else {
				query+="\n"+varToUse+" "+predForBelonging+" "+NTriplesUtil.toNTriplesString(type)+" .";
			}
		} else {
			//the input type list of list is more complicate than a single value, so behave according 
			// (an OR of AND)
			if(searchInSubTypes) {
				query+=filterWithOrOfAndValues("?genericSubType", 
						NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"*", typesListOfList) +
					"\n"+varToUse+" "+predForBelonging+" ?genericSubType .";
			} else {
				query+=filterWithOrOfAndValues(varToUse, predForBelonging, typesListOfList);
			}
		}
		query+= "\n}";
		//@formatter:on
		
		return query;
	}
	
	public static String getPrefixes() {
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nPREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"\nPREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
				"\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> ";
		return query;
	}
	
	public void checksPreQuery(String searchString, String [] rolesArray, SearchMode searchMode, 
			boolean searchStringCanBeNull) 
			throws IllegalStateException{
		
		if(!searchStringCanBeNull && (searchString ==null || searchString.isEmpty()) ){
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
				} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.dataRange.name().toLowerCase())){
					isDataRangeWanted = true;
				} else if(rolesArray[i].toLowerCase().equals(RDFResourceRole.limeLexicon.name().toLowerCase())) {
					isLexiconWanted = true;
				} else if (rolesArray[i].toLowerCase().equals(RDFResourceRole.ontolexLexicalEntry.name().toLowerCase())){
					isLexicalEntryWanted = true;
				}
			}	
		}
		//@formatter:off
		if(rolesArray!= null && !isClassWanted && !isConceptWanted && !isConceptSchemeWanted && 
				!isInstanceWanted && !isPropertyWanted && !isCollectionWanted && !isDataRangeWanted && 
				!isLexiconWanted && !isLexicalEntryWanted){
			
			String msg = "the search roles should be at least one of: "+
					RDFResourceRole.cls.name()+", "+
					RDFResourceRole.concept.name()+", "+
					RDFResourceRole.conceptScheme.name()+", "+
					RDFResourceRole.individual+", "+
					RDFResourceRole.property.name() +", "+
					RDFResourceRole.skosCollection.name() +", "+
					RDFResourceRole.dataRange.name() +", "+
					RDFResourceRole.limeLexicon + " or "+
					RDFResourceRole.ontolexLexicalEntry;
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException(msg);
			
		}
		//@formatter:on
		
		//if(searchMode != SearchMode.startsWith && searchMode != SearchMode.contains && 
		//		searchMode != SearchMode.exact && searchMode != SearchMode.endsWith){
		if(!searchStringCanBeNull  && searchMode == null) {
			String msg = "the search mode should be one of: "+ SearchMode.startsWith +", "+
					SearchMode.contains +", "+ SearchMode.endsWith +", "+ SearchMode.exact +
					" or "+SearchMode.fuzzy;
			//TODO change the exception (previously was a fail)
			throw new IllegalArgumentException(msg);
		}
		
//		return searchModeSelected;
	}
	
	public static String prepareQueryWithStatusOutgoingIngoing(StatusFilter statusFilter,
			List<Pair<IRI, List<Value>>> outgoingLinks,
			List<TripleForSearch<IRI, String, SearchMode>> outgoingSearch,
			List<Pair<IRI, List<Value>>> ingoingLinks, SearchStrategy searchStrategy, String baseURI,
			boolean includeLocales) {
		String query = "";
		//@formatter:off
		
		//the part relative to the status
		if(statusFilter!=null) {
			if(statusFilter.equals(StatusFilter.ANYTHING)) {
				//do nothing in this case
			} else if(statusFilter.equals(StatusFilter.NOT_DEPRECATED)) {
				//check that the resource is not marked as deprecated
				query += "\nFILTER NOT EXISTS{" +
						"\n{?resource "+NTriplesUtil.toNTriplesString(OWL.DEPRECATED)+" true }" +
						"\nUNION"+
						"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDCLASS)+" }" +
						"\nUNION"+
						"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDPROPERTY)+" }" +
						"\n}";
						
			} else if(statusFilter.equals(StatusFilter.ONLY_DEPRECATED)) {
				//check that the resource is marked as deprecated
				query += 
					"\n{?resource "+NTriplesUtil.toNTriplesString(OWL.DEPRECATED)+" true }" +
					"\nUNION"+
					"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDCLASS)+" }" +
					"\nUNION"+
					"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDPROPERTY)+" }";
			} else if(statusFilter.equals(StatusFilter.UNDER_VALIDATION)) {
				//check that in the validation graph there is the triple 
				// ?resource a ?type
				IRI validationGraph = (IRI) VALIDATION.stagingAddGraph(SimpleValueFactory.getInstance()
						.createIRI(baseURI));
				query+="\nGRAPH "+NTriplesUtil.toNTriplesString(validationGraph)+" { "+
						"?resource a ?type_for_validation ." +
						"}";
			} else if(statusFilter.equals(StatusFilter.UNDER_VALIDATION_FOR_DEPRECATION)) {
				//check that in the validation graph the resource is marked as deprecated
				IRI validationGraph = (IRI) VALIDATION.stagingAddGraph(SimpleValueFactory.getInstance()
						.createIRI(baseURI));
				String valGraph = NTriplesUtil.toNTriplesString(validationGraph);
				query +="\n{GRAPH "+valGraph+"{?resource "+NTriplesUtil.toNTriplesString(OWL.DEPRECATED)+" true }}" +
						"\nUNION"+
						"\n{GRAPH "+valGraph+"{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDCLASS)+" }}" +
						"\nUNION"+
						"\n{GRAPH "+valGraph+"{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDPROPERTY)+" }}";
			}
		}
		
		//the outgoingLinks part
		if(outgoingLinks!=null && outgoingLinks.size()>0) {
			query += ServiceForSearches.filterWithOrOfAndPairValues(outgoingLinks, "?resource", "out", false);
		}
		//the outgoingSearch part
		int cont=1;
		if(outgoingSearch!=null && outgoingSearch.size()>0) {
			String valueOfProp = "?valueOfProp_"+cont;
			for(TripleForSearch<IRI, String, SearchMode> tripleForSearch : outgoingSearch) {
				//be careful that the tripleForSearch.getPredicate() can be null, so in that case, use a variable
				String predValueOrVar;
				if(tripleForSearch.getPredicate() == null) {
					predValueOrVar = "?inputPredNull"+cont;
				} else {
					predValueOrVar = NTriplesUtil.toNTriplesString(tripleForSearch.getPredicate());
				}
				query += "\n?resource "+predValueOrVar+" "+valueOfProp+" ." +
						searchModePrepareQueryNoIndexes(valueOfProp, tripleForSearch.getSearchString(),
								tripleForSearch.getMode());
			}
		}

		//the ingoingLinks part	
		if(ingoingLinks!=null && ingoingLinks.size()>0) {
			query += ServiceForSearches.filterWithOrOfAndPairValues(ingoingLinks, "?resource", "in", true);
		}
		
		//@formatter:on
		
		return query;
	}

	// used by prepareQueryWithStatusOutgoingIngoing when outgoingSearch is not empty/null. It does not use any index,
	// just plain old regex
	private static String searchModePrepareQueryNoIndexes(String variable, String value, SearchMode searchMode){
		String query ="";

		if(searchMode == SearchMode.startsWith){
			query="\nFILTER regex(str("+variable+"), '^"+value+"', 'i')";
		} else if(searchMode == SearchMode.endsWith){
			query="\nFILTER regex(str("+variable+"), '"+value+"$', 'i')";
		} else if(searchMode == SearchMode.contains){
			query="\nFILTER regex(str("+variable+"), '"+value+"', 'i')";
		} else if(searchMode == SearchMode.fuzzy){
			//List<String> wordForNoIndex = ServiceForSearches.wordsForFuzzySearch(value, ".", false);
			List<String> wordForNoIndex = ServiceForSearches.wordsForFuzzySearch(value, ".", true,
					false, searchMode);
			String wordForNoIndexAsString = ServiceForSearches.listToStringForQuery(wordForNoIndex, "^", "$");
			query += "\nFILTER regex(str("+variable+"), \""+wordForNoIndexAsString+"\", 'i')";
		} else { // searchMode.equals(exact)
			query="\nFILTER regex(str("+variable+"), '^"+value+"$', 'i')";
		}

		return query;
	}
	
	public static List<String> wordsForFuzzySearch(String text, String replaceChar, boolean escapeForRegex,
												   boolean escapeForLucene, SearchMode searchMode){
		List<String> wordsList = new ArrayList<>();
		wordsList.add(replaceChar+escapeOrNot(text, escapeForRegex, escapeForLucene, searchMode));
		for(int i=0; i<text.length(); ++i) {
			wordsList.add(escapeOrNot(text.substring(0, i), escapeForRegex, escapeForLucene, searchMode)+replaceChar+
					escapeOrNot(text.substring(i+1), escapeForRegex, escapeForLucene, searchMode));
		}
		wordsList.add(escapeOrNot(text, escapeForRegex, escapeForLucene, searchMode)+replaceChar);
		return wordsList;
	}
	
	private static String escapeOrNot(String text, boolean escapeForRegex, boolean escapeForLucene,
									  SearchMode searchMode) {
		if(escapeForRegex) {
			return ServiceForSearches.escapeStringForRegexInSPARQL(text);
		} else if (escapeForLucene){
			return ServiceForSearches.normalizeStringForLuceneIndex(text, searchMode);
		}
		return text;
	}

	public static String normalizeStringForLuceneIndex(String inputString, SearchMode searchMode) {
		String outputString = inputString;

		//@formatter:off
		//replace all punctuation character except for the underscore <code>_<code>
		//replace all \ and - with a whitespace
		//replace the ' with \'
		outputString = outputString.replaceAll("\\p{Punct}&&[^_]", " ")
				.replace("\\", " ")
				.replace("\'", "\\'")
				.replace("-", " ")
				.trim();
		//@formatter:on

		// replace the '(' with '\\('
		outputString = outputString.replaceAll("\\(", "\\(");
		// replace the ')' with '\\)'
		outputString = outputString.replaceAll("\\)", "\\)");


		//if the search mode is not exactMatch, the starting and ending . should be replaced with (.)
		if(!searchMode.equals(SearchMode.exact)){
			if(outputString.startsWith(".")){
				outputString = "(.)"+outputString.substring(1);
			}
			if(outputString.endsWith(".")){
				outputString = outputString.substring(0, outputString.length()-1)+"(.)";
			}
		}
		return outputString;
	}
	
	public static String listToStringForQuery(List<String> wordsList, String startSymbol, String endSymbol) {
		String textForQuery="";
		boolean first = true;
		for(int i=0; i<wordsList.size(); ++i) {
			if(!first) {
				textForQuery+="|";
			}
			else {
				first = false;
			}
			textForQuery+="("+startSymbol+wordsList.get(i)+endSymbol+")";
		}
		return textForQuery;
	}
	
	public static String escapeStringForRegexInSPARQL(String input) {
		String output;
		output = Pattern.quote(input);
		output = output.replace("\\", "\\\\");
		output = output.replace("\'", "\\'");
		
		return output;
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
				/*if(label.getLanguage().isPresent()){
					result += "@"+label.getLanguage().get();
				}*/
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
		} else if(typeURI.equals(RDFS.DATATYPE.stringValue())){
			role = RDFResourceRole.dataRange;
		} else if(typeURI.equals(LIME.LEXICON.stringValue())) {
			role = RDFResourceRole.limeLexicon;
		} else if(typeURI.equals(LIME.LEXICAL_ENTRIES.stringValue())) {
			role = RDFResourceRole.ontolexLexicalEntry;
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
		List<IRI> schemeList = null;
		
		public ValueTypeAndShow(IRI resource, RDFResourceRole role) {
			this.resource = resource;
			this.role = role;
			this.showList = new ArrayList<Literal>();
			this.schemeList = new ArrayList<IRI>();
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
		
		public void addScheme(IRI scheme){
			if(!schemeList.contains(scheme)){
				schemeList.add(scheme);
			}
		}
		
		public void addSchemeList(List<IRI> schemeList){
			for(IRI scheme : schemeList){
				if(!schemeList.contains(scheme)){
					this.schemeList.add(scheme);
				}
			}
		}
		

		public IRI getResource() {
			return resource;
		}
		
		public boolean hasShowValue(Literal show){
			return showList.contains(show);
		}
		
		public boolean hasScheme(IRI scheme){
			return schemeList.contains(scheme);
		}
		
		public List<Literal> getShowList() {
			return showList;
		}
		
		public List<IRI> getSchemeList(){
			return schemeList;
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
		
		public String getSchemesAsString(){
			boolean first = true;
			String schemesAsString = "";
			for(IRI scheme : schemeList){
				if(!first){
					schemesAsString+=",";
				}
				first=false;
				schemesAsString+=scheme.stringValue();
			}
			return schemesAsString;
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
		
		public boolean isSchemePresent(){
			if(!schemeList.isEmpty()){
				return true;
			}
			return false;
		}
	}

	

	public static String getUriStartFromQname(String qnameToCheck, Map<String, String> prefixToNamespaceMap){
		String prefix="", localNamePart="";
		if(qnameToCheck.contains(":") ){
			//it has the :, so it could be a qname, check if the : are used only once
			String[] qnameArray = qnameToCheck.split(":");
			if(qnameArray.length>2){
				//there are more : , so return the input text
				return qnameToCheck;
			} else  if(qnameArray.length == 2){ // prefix:localNamePart
				prefix = qnameArray[0];
				localNamePart = qnameArray[1];

			} else if(qnameArray.length == 1) { // prefix:
				prefix = qnameArray[0];
				localNamePart = "";
			} else if(qnameArray.length == 0) { // :
				prefix = "";
				localNamePart = "";
			}

			if(prefixToNamespaceMap.containsKey(prefix)){
				return prefixToNamespaceMap.get(prefix)+localNamePart;
			} else {
				//the prefixToNamespaceMap it does not have that prefix, so return the input text
				return qnameToCheck;
			}

		}
		//it is not a qname, so return the input text
		return qnameToCheck;
	}

	public static String prepareLangFilter( List <String> langs, String variable, boolean includeLocales){
		//if at least one language is specified, then filter the results of the label having such language
		String query="";
		if(langs!=null && langs.size()>0) {
			boolean first=true;
			query+="\nFILTER(";
			for(String lang : langs) {
				if(!first) {
					query+=" || ";
				}
				first=false;
				if(includeLocales) {
					query+="regex(lang("+variable+"), '^"+lang+"','i')";
				} else {
					query+="regex(lang("+variable+"), '^"+lang+"$','i')";
				}
			}
			query+=")";
		}
		return query;

	}

}

