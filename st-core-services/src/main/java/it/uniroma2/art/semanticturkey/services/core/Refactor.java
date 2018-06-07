package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.AlreadyExistingLiteralFormForResourceException;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingLiteralFormForResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;
import it.uniroma2.art.semanticturkey.utilities.TurtleHelp;

/**
 * This class provides services to convert SKOS to SKOSXL data and SKOSXL to SKOS.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */

@STService
public class Refactor extends STServiceAdapter  {
	
	private static Logger logger = LoggerFactory.getLogger(Refactor.class);
	
	@Autowired
	private CustomFormManager cfManager;
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#oldResource)+ ')', 'U')")
	public void changeResourceURI(@LocallyDefined IRI oldResource, IRI newResource) throws DuplicatedResourceException {
		RepositoryConnection conn = getManagedConnection();
		//check if a resource with the new IRI already exists
		// @formatter:off
		String query = 
				"ASK {								\n" +
				"	BIND (%graph_uri% as ?graph) 	\n" +
				"	BIND (%res_uri% as ?res)	 	\n" +
				"	GRAPH ?graph {					\n" +
				"		{ ?res ?p1 ?o1 . } 			\n" + //as subject
				"		UNION 						\n" +
				"		{ ?s2 ?res ?o2 . } 			\n" + //as predicate
				"		UNION 						\n" +
				"		{ ?s3 ?p3 ?res . } 			\n" + //as object
				"	}								\n" +
				"}";
		// @formatter:on
		query = query.replace("%graph_uri%", NTriplesUtil.toNTriplesString(getWorkingGraph()));
		query = query.replace("%res_uri%", NTriplesUtil.toNTriplesString(newResource));
		BooleanQuery bq = conn.prepareBooleanQuery(query);
		boolean existing = bq.evaluate();
		if (existing) {
			throw new DuplicatedResourceException("Could not rename resource: "
					+ oldResource.stringValue() + " to: " + newResource.stringValue()
					+ " because a resource with this name already exists");
		}
		// @formatter:off
		query =	"DELETE { 							\n" +
				"	GRAPH ?graph { 					\n" + 
				"		?oldRes ?p1 ?o1 .			\n" +
				"		?s2 ?oldRes ?o2 .			\n" +
				"		?s3 ?p3 ?oldRes .			\n" +
				"	} 								\n" +
				"} 									\n" +
				"INSERT {							\n" +
				"	GRAPH ?graph { 					\n" + 
				"		?newRes ?p1 ?o1 .			\n" +
				"		?s2 ?newRes ?o2 .			\n" +
				"		?s3 ?p3 ?newRes .			\n" +
				"	} 								\n" +
				"} 									\n" +
				"WHERE { 							\n" +
				"	BIND (%graph_uri% as ?graph)	\n" +
				"	BIND (%oldRes_uri% as ?oldRes)	\n" +
				"	BIND (%newRes_uri% as ?newRes)	\n" +
				"	GRAPH ?graph { 					\n" + 
				"		{ ?oldRes ?p1 ?o1 . }		\n" +
				"		UNION						\n" +
				"		{ ?s2 ?oldRes ?o2 . }		\n" +
				"		UNION						\n" +
				"		{ ?s3 ?p3 ?oldRes . }		\n" +
				"	} 								\n" +
				"}";
		// @formatter:on
		query = query.replace("%graph_uri%", NTriplesUtil.toNTriplesString(getWorkingGraph()));
		query = query.replace("%oldRes_uri%", NTriplesUtil.toNTriplesString(oldResource));
		query = query.replace("%newRes_uri%", NTriplesUtil.toNTriplesString(newResource));
		Update update = conn.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * 
	 * TODO this doesn't work at the moment, 
	 * it doesn't update correctly the working graph and the baseURI in the project object
	 * 
	 * Replace the <code>sourceBaseURI</code> with the <code>targetBaseURI</code>.
	 * If <code>sourceBaseURI</code> is not provided, replace the default baseURI.
	 * @param sourceBaseURI
	 * @param targetBaseURI
	 * @throws ProjectUpdateException 
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf', 'CRUD')")
	public void replaceBaseURI(@Optional IRI sourceBaseURI, IRI targetBaseURI) throws ProjectUpdateException {
		//get the source baseURI
		String source;
		String oldWorkingGraph = NTriplesUtil.toNTriplesString(getWorkingGraph());
		if (sourceBaseURI == null) {
			source = getProject().getBaseURI();
		} else {
			source = sourceBaseURI.stringValue();
		}
		//get the target baseURI
		String target = targetBaseURI.stringValue();
		if(source.equals(target)){
			//the new baseURI is the same as the old one, so just return and do nothing
			return;
		}
		//if the source baseURI is the default one, set the new baseURI of the project
		if (source.equals(getProject().getBaseURI())) {
			getProject().getNewOntologyManager().setBaseURI(target);
			getProject().setBaseURI(target);
			//TODO update working graph
		}
		// @formatter:off
		String query =
				"DELETE {																					\n" +
				"	GRAPH ?oldGraph {																		\n" +
				"		?oldS ?oldP ?oldO .																	\n" +
				"	}																						\n" +
				"}																							\n" +
				"INSERT {																					\n" +
				"	GRAPH ?newGraph {																		\n" +
				"		?newS ?newP ?newO .																	\n" +
				"	}																						\n" +
				"}																							\n" +
				"WHERE {																					\n" +
				"	BIND(%oldWorkingGraph% AS ?oldGraph)													\n" +
				"	BIND(%newWorkingGraph% AS ?newGraph)													\n" +
				"	GRAPH ?oldGraph {																		\n" +
				"		?oldS ?oldP ?oldO .																	\n" +
				"	}																						\n" +
				//get the base uri of ?oldS, ?oldP and oldO
				"	BIND (REPLACE(STR(?oldS), '(^.*(#|/))([^#|^/]*)', '$1') AS ?oldBaseUriS)				\n" + 
    			"	BIND (REPLACE(STR(?oldP), '(^.*(#|/))([^#|^/]*)', '$1') AS ?oldBaseUriP)				\n" +
    			"	BIND (REPLACE(STR(?oldO), '(^.*(#|/))([^#|^/]*)', '$1') AS ?oldBaseUriO)				\n" +
    			
    			//create the new triple, one resource at a time (if is has the oldBaseURI replace it with the
    			// new one, otherwise just copy the old value)
    			"BIND(																						\n "+
    			"	IF(?oldBaseUriS = '%oldBaseURI%',														\n" +
    			"		URI(REPLACE(STR(?oldS), '(^.*(#|/))([^#|^/]*)', '%newBaseURI%$3') ), 				\n" +
    			"		?oldS) 																				\n "+
    			"	AS ?newS) \n" +
    			"BIND(																						\n "+
    			"	IF(?oldBaseUriP = '%oldBaseURI%',														\n" +
    			"		URI(REPLACE(STR(?oldP), '(^.*(#|/))([^#|^/]*)', '%newBaseURI%$3') ), 				\n" +
    			"		?oldP) 																				\n "+
    			"	AS ?newP) \n" +
    			"BIND(																						\n "+
    			"	IF(?oldBaseUriO = '%oldBaseURI%',														\n" +
    			"		URI(REPLACE(STR(?oldO), '(^.*(#|/))([^#|^/]*)', '%newBaseURI%$3') ), 				\n" +
    			"		?oldO) 																				\n "+
    			"	AS ?newO) \n" +
    			"}";
		// @formatter:on
		if(!source.endsWith("#") || !source.endsWith("/")){
			//add / or # at the end of source
			source+="#";
		}
		query = query.replace("%oldBaseURI%", source);
		if(!target.endsWith("#") || !target.endsWith("/")){
			target+="#";
			//TODO add / or # at the end of target
		}
		query = query.replace("%newBaseURI%", target);
		query = query.replace("%oldWorkingGraph%", oldWorkingGraph); 
		query = query.replace("%newWorkingGraph%", NTriplesUtil.toNTriplesString(getWorkingGraph())); 
		
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(query);
		update.execute();
	}
	
	/**
	 * Moves the content of the default graph to a graph named after the base URI of the current project. This
	 * method clears the default graph and preserves (by default) the information already contained in the
	 * destination graph.
	 * 
	 * @param clearDestinationGraph
	 *            Specifies whether the destination graph is cleared before the insert of triples from the
	 *            default graph
	 * @return
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf', 'CRUD')")
	public void migrateDefaultGraphToBaseURIGraph(@Optional(defaultValue = "false") boolean clearDestinationGraph) {
		String updateSpec;
		if (clearDestinationGraph) {
			updateSpec = "MOVE DEFAULT TO GRAPH %destinationGraph%";
		} else {
			updateSpec = "ADD DEFAULT TO GRAPH %destinationGraph% ; DROP DEFAULT";
		}
		updateSpec = updateSpec.replace("%destinationGraph%", NTriplesUtil.toNTriplesString(getWorkingGraph()));
		RepositoryConnection conn = getManagedConnection();
		Update update = conn.prepareUpdate(updateSpec);
		update.execute();
	}
	

	/**
	 * it refactor SKOS data into SKOSXL
	 * @return
	 * @throws URIGenerationException 
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(lexicalization)', 'CD')")
	public void SKOStoSKOSXL(boolean reifyNotes) throws URIGenerationException{
		logger.debug("request to refactor SKOS data to SKOSXL");
		
		List<ConceptLabelValueGraph> conceptLabelValueGraphList = new ArrayList<ConceptLabelValueGraph>();
		List<ConceptNoteValueGraph> conceptNoteValueGraphList = new ArrayList<ConceptNoteValueGraph>();
		
		IRI workingGraph = (IRI)getWorkingGraph();
		String workingGraphString = SPARQLHelp.toSPARQL(workingGraph);
		
		//first of all, take the SKOS data you need to refactor
		
		//get all the info regarding the labels(pref/alt/hidden)
		// @formatter:off
		String selectQuery = 
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
				"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" + 
				
				"SELECT ?concept ?propLabel ?value ?graph\n" +
				"WHERE {\n" +
				"GRAPH "+workingGraphString+" {?concept ?propLabel ?value . " +
				"FILTER(?propLabel = skos:prefLabel || ?propLabel = skos:altLabel || ?propLabel = skos:hiddenLabel) \n" +
				"}" +
				"}";
		// @formatter:on
		
		TupleQuery select = getManagedConnection().prepareTupleQuery(selectQuery);
		
		SimpleDataset dataset = new SimpleDataset();
		
		List<Resource> contextList = QueryResults.asList(getManagedConnection().getContextIDs());
		//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
		dataset.addNamedGraph(workingGraph);
		
		select.setDataset(dataset);
		
		//execute the query
		TupleQueryResult tupleQueryResult = select.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			IRI concept = (IRI) bindingSet.getBinding("concept").getValue();
			IRI labelType = (IRI) bindingSet.getBinding("propLabel").getValue();
			Literal value = (Literal) bindingSet.getBinding("value").getValue();
			//IRI graph = (IRI) bindingSet.getBinding("graph").getValue();
			conceptLabelValueGraphList.add(new ConceptLabelValueGraph(concept, labelType, value, workingGraph));
			
		}
		tupleQueryResult.close();
		
		if(reifyNotes){
			//get all the info regarding the notes (and its subproperties)
			// @formatter:off
			selectQuery = 
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
					"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" + 
					
					"SELECT ?concept ?propNote ?value ?graph\n" +
					"WHERE {\n" +
					"?propNote rdfs:subPropertyOf* skos:note . \n" + 
					"GRAPH "+workingGraphString+" {?concept ?propNote ?value . }" +
					"}";
			// @formatter:on
			select = getManagedConnection().prepareTupleQuery(selectQuery);
			
			dataset = new SimpleDataset();
			
			contextList = QueryResults.asList(getManagedConnection().getContextIDs());
			for(Resource iri : contextList){
				if(iri instanceof IRI){
					//add the defaults graphs (used outside the GRAPH sections of the query)
					dataset.addDefaultGraph((IRI) iri);
				}
			}
			//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
			dataset.addNamedGraph(workingGraph);
			select.setDataset(dataset);
			
			//execute the query
			tupleQueryResult = select.evaluate();
			while(tupleQueryResult.hasNext()){
				BindingSet bindingSet = tupleQueryResult.next();
				IRI concept = (IRI) bindingSet.getBinding("concept").getValue();
				IRI noteType = (IRI) bindingSet.getBinding("propNote").getValue();
				Literal value = (Literal) bindingSet.getBinding("value").getValue();
				//IRI graph = (IRI) bindingSet.getBinding("graph").getValue();
				conceptNoteValueGraphList.add(new ConceptNoteValueGraph(concept, noteType, value, workingGraph));
			}
			tupleQueryResult.close();
		}
		
		//now iterate over the two lists (labels and note if reifyNotes is set to true) to construct the necessary SKOSXL construct
		String updateQuery;
		//first the label list
		for(ConceptLabelValueGraph conceptLabelValueGraph : conceptLabelValueGraphList){
			IRI concept = conceptLabelValueGraph.getConcept();
			IRI labelType = conceptLabelValueGraph.getLabelType();
			Literal value = conceptLabelValueGraph.getValue();
			IRI graph = conceptLabelValueGraph.getGraph();
			
			Map<String, Value> valueMapping = new HashMap<String, Value>();
			valueMapping.put(URIGenerator.Parameters.lexicalForm, value);
			valueMapping.put(URIGenerator.Parameters.lexicalizedResource, concept);
			Value skosxlLabelType = null;
			if(labelType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL)){
				skosxlLabelType = SKOSXL.PREF_LABEL;
			} else if(labelType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.ALT_LABEL)){
				skosxlLabelType = SKOSXL.ALT_LABEL;
			} else{ // hidden label
				skosxlLabelType = SKOSXL.HIDDEN_LABEL;
			}
			valueMapping.put(URIGenerator.Parameters.lexicalizationProperty, skosxlLabelType);
			IRI newIRIForLabel = generateIRI(URIGenerator.Roles.xLabel, valueMapping );
			//now add the new xlabel and remove the old data regarding SKOS
			String graphString = SPARQLHelp.toSPARQL(graph); 
			String conceptString = SPARQLHelp.toSPARQL(concept);
			String labelTypeString = SPARQLHelp.toSPARQL(labelType);
			String skoxlLabelTypeString = SPARQLHelp.toSPARQL(skosxlLabelType);
			String valueString = SPARQLHelp.toSPARQL(value);
					
			String newIRIForLabelString = SPARQLHelp.toSPARQL(newIRIForLabel);//"<"+newIRIForLabel.stringValue()+">";
			// @formatter:off
			updateQuery =
					"DELETE DATA {\n" +
					"GRAPH "+graphString+" {"+conceptString+" "+labelTypeString+" "+valueString+" }" +
					"}; \n" +
					"INSERT DATA {\n" +
					"GRAPH "+graphString+" { \n" +
					conceptString+" "+skoxlLabelTypeString+" "+newIRIForLabelString+" .\n" +
					newIRIForLabelString +" <"+RDF.TYPE.stringValue()+"> <"+SKOSXL.LABEL.stringValue()+"> .\n" + 
					newIRIForLabelString+" <"+SKOSXL.LITERAL_FORM.stringValue()+"> "+valueString+" . \n" +
					"}\n" +
					"}";
			// @formatter:on
			Update update = getManagedConnection().prepareUpdate(updateQuery);
			
			dataset = new SimpleDataset();
			
			contextList = QueryResults.asList(getManagedConnection().getContextIDs());
			//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
			dataset.addNamedGraph(workingGraph);
			update.setDataset(dataset);
			//execute the UPDATE
			update.execute();
			
		}
		
		if(reifyNotes){
			//now the notes list
			for(ConceptNoteValueGraph conceptNoteValueGraph : conceptNoteValueGraphList){
				IRI concept = conceptNoteValueGraph.getConcept();
				IRI noteType = conceptNoteValueGraph.getNoteType();
				Literal value = conceptNoteValueGraph.getValue();
				IRI graph = conceptNoteValueGraph.getGraph();
				
				Map<String, Value> valueMapping = new HashMap<String, Value>();
				valueMapping.put(URIGenerator.Parameters.lexicalForm, value);
				valueMapping.put(URIGenerator.Parameters.lexicalizedResource, concept);
				valueMapping.put(URIGenerator.Parameters.noteProperty, noteType);
				IRI newIRIForNote = generateIRI(URIGenerator.Roles.xNote, valueMapping );
				//now add the new xNote and remove the old data regarding SKOS
				String graphString = SPARQLHelp.toSPARQL(graph); 
				String conceptString = SPARQLHelp.toSPARQL(concept);
				String noteTypeString = SPARQLHelp.toSPARQL(noteType);
				String valueString = SPARQLHelp.toSPARQL(value); 
						
				String newIRIForNoteString = SPARQLHelp.toSPARQL(newIRIForNote); 
				// @formatter:off
				updateQuery =
						"DELETE DATA {\n" +
						"GRAPH "+graphString+" {"+conceptString+" "+noteTypeString+" "+valueString+" }" +
						"}; \n" +
						"INSERT DATA {\n" +
						"GRAPH "+graphString+" { \n" +
						conceptString+" "+noteTypeString+" "+newIRIForNoteString+" .\n" +
						newIRIForNoteString+" <"+RDF.VALUE.stringValue()+"> "+valueString+" . \n" +
						"}\n" +
						"}";
				// @formatter:on
				Update update = getManagedConnection().prepareUpdate(updateQuery);
				
				dataset = new SimpleDataset();
				
				contextList = QueryResults.asList(getManagedConnection().getContextIDs());
				//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
				dataset.addNamedGraph(workingGraph);
				update.setDataset(dataset);
				//execute the UPDATE
				update.execute();
			}
		}
	}
	
	/**
	 * it refactor SKOSXL data into SKOS
	 * @return
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(lexicalization)', 'CD')")
	public void SKOSXLtoSKOS(boolean flattenNotes){
		logger.debug("request to refactor SKOSXL data to SKOS");
		
		IRI workingGraph = (IRI) getWorkingGraph();
		String workingGraphString = SPARQLHelp.toSPARQL(workingGraph);
		
		// @formatter:off
		String queryUpdate = 
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
				"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" + 
				//remove the SKOSXL data
				"DELETE { \n";
				//the part relative to skos:note (and its sub properties)
		if(flattenNotes){
			queryUpdate += "GRAPH "+workingGraphString+" {?concept ?propNote ?reifiedNote . \n" +
			"?reifiedNote rdf:value ?value . } \n";
		}
				///the related to the labels (Pref, Alt and Hidden)
		queryUpdate += "GRAPH "+workingGraphString+" {?genericSubj ?genericProp1 ?label . \n " +
				"?label ?genericProp2 ?genericObj . }\n" +
				
				"} \n " +
				
				//insert the SKOS data
				"INSERT { \n" +
				"GRAPH "+workingGraphString+"{?concept skos:prefLabel ?prefLabelLitForm . } \n " +
				"GRAPH "+workingGraphString+"{?concept skos:altLabel ?altLabelLitForm . } \n" +
				"GRAPH "+workingGraphString+"{?concept skos:hiddenLabel ?hiddenLabelLitForm . } \n";
		if(flattenNotes){
			queryUpdate += "GRAPH "+workingGraphString+"{?concept ?propNote ?value .} \n";
		}
		queryUpdate += "} \n" +
				//get the data in SKOSXL to be transformed in SKOS
				"WHERE {\n" +
			
				//the labels part (prefLabel, altLabel, hiddenLabel)
				"{ GRAPH "+workingGraphString+"{?concept skosxl:prefLabel ?label . \n" +
				"?label skosxl:literalForm ?prefLabelLitForm . \n" +
				"?genericSubj ?genericProp1 ?label . \n" +
				"?label ?genericProp2 ?genericObj . } \n" +
				"}\n" +
				"UNION \n" +
				"{ GRAPH "+workingGraphString+"{?concept skosxl:altLabel ?label . \n" +
				"?label skosxl:literalForm ?altLabelLitForm . \n" +
				"?genericSubj ?genericProp1 ?label . \n" +
				"?label ?genericProp2 ?genericObj . } \n" +
				"}\n" +
				"UNION \n" +
				"{ GRAPH "+workingGraphString+"{?concept skosxl:hiddenLabel ?label . \n" +
				"?label skosxl:literalForm ?hiddenLabelLitForm . \n" +
				"?genericSubj ?genericProp1 ?label . \n" +
				"?label ?genericProp2 ?genericObj . } \n" +
				"}\n";

		if(flattenNotes){
				//the notes (and its sub properties) part
			queryUpdate += "UNION \n"+
				"{?propNote rdfs:subPropertyOf* skos:note . \n" + 
				"GRAPH "+workingGraphString+"{?concept ?propNote ?reifiedNote . \n" + 
				"?reifiedNote rdf:value ?value .} \n" +
				"} \n";
		}
				
		queryUpdate += "}";
		// @formatter:on
		
		Update update = getManagedConnection().prepareUpdate(queryUpdate);
		
		SimpleDataset dataset = new SimpleDataset();
		
		List<Resource> contextList = QueryResults.asList(getManagedConnection().getContextIDs());
		for(Resource iri : contextList){
			if(iri instanceof IRI){
				//add the defaults graphs (used outside the GRAPH sections of the query)
				dataset.addDefaultGraph((IRI) iri);
				//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
				//dataset.addNamedGraph((IRI) iri);
			}
		}
		//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
		dataset.addNamedGraph(workingGraph);
		update.setDataset(dataset);
		
		//execute the query
		update.execute();
	}
	
	
	/**
	 * a refactoring service for moving xLabels to new concepts ( ST-498 ) 
	 * @return the newCoceptIRI as an AnnotatedValue
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'C')")
	public AnnotatedValue<IRI> spawnNewConceptFromLabel(
			@Optional @NotLocallyDefined IRI newConcept, 
			@LocallyDefined Resource xLabel,
			@Optional @LocallyDefined IRI oldConcept,
			@Optional @LocallyDefined @Selection Resource broaderConcept, 
			@LocallyDefinedResources List<IRI> conceptSchemes,
			@Optional CustomFormValue customFormValue)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, 
					CODAException, NonExistingLiteralFormForResourceException{
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		RepositoryConnection repoConnection = getManagedConnection();
		
		IRI newConceptIRI = null;
		//get the label from the input xLabel
		RepositoryResult<Statement> repositoryResult = repoConnection.getStatements(xLabel, SKOSXL.LITERAL_FORM, null, getUserNamedGraphs());
		Model tempModel = QueryResults.asModel(repositoryResult);
		if(!Models.objectLiteral(tempModel).isPresent()){
			throw new NonExistingLiteralFormForResourceException(xLabel);
		}
		Literal label = Models.objectLiteral(tempModel).get();
		if (newConcept == null) {
			for(IRI conceptScheme : conceptSchemes){
				newConceptIRI = generateConceptIRI(label, Arrays.asList(conceptScheme));
			}
		} else {
			newConceptIRI = newConcept;
		}
		//add the new concept (just generated using the passed xLabel or the IRI passed by the user) 
		modelAdditions.add(newConceptIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT);
		
		//add the passed xLabel to the new concept as skosxl:prefLabel
		modelAdditions.add(newConceptIRI, SKOSXL.PREF_LABEL, xLabel);
		
		//add the new concept to the desired scheme
		for(IRI conceptScheme : conceptSchemes){
			modelAdditions.add(newConceptIRI, SKOS.IN_SCHEME, conceptScheme);
		}
		if (broaderConcept != null) {
			modelAdditions.add(newConceptIRI, SKOS.BROADER, broaderConcept);
		} else {
			for(IRI conceptScheme : conceptSchemes){
				modelAdditions.add(newConceptIRI, SKOS.TOP_CONCEPT_OF, conceptScheme);
			}
		}

		//find out what property exists between the passed xLabel and the old concept
		//oldConcept can be null
		
		String query = "SELECT ?concept ?predicate \n"+
					"WHERE{ \n"+
					"?concept ?predicate "+NTriplesUtil.toNTriplesString(xLabel)+" .\n "+
					"?concept a "+NTriplesUtil.toNTriplesString(SKOS.CONCEPT)+
					"}";
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		if(oldConcept != null){
			tupleQuery.setBinding("concept", oldConcept);
		}
		SimpleDataset dataset = new SimpleDataset();
		//add the default named graphs 
		for(Resource graph : getUserNamedGraphs()){
			if(graph instanceof IRI){
				dataset.addDefaultGraph((IRI)graph);
			}
		}
		tupleQuery.setDataset(dataset);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			IRI conceptIRI = (IRI) bindingSet.getBinding("concept").getValue();
			IRI predicate = (IRI) bindingSet.getBinding("predicate").getValue();
			modelRemovals.add(conceptIRI, predicate, xLabel);
		}
		tupleQueryResult.close();
		
		//repositoryResult = repoConnection.getStatements(oldConcept, null, xLabel, getUserNamedGraphs());
		/*tempModel = QueryResults.asModel(repositoryResult);
		if(!Models.predicate(tempModel).isPresent()){
			throw new NonExistingPredicateBetweenResourcesExpcetion(oldConcept, xLabel);
		}
		IRI predicate = Models.predicate(tempModel).get();
		modelRemovals.add(oldConcept, predicate, xLabel);*/
		
		//CustomForm further info
		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newConceptIRI.stringValue());
			if (xLabel != null) {
				stdForm.addFormEntry(StandardForm.Prompt.xLabel, xLabel.stringValue());
				stdForm.addFormEntry(StandardForm.Prompt.lexicalForm, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm, customFormValue.getUserPromptMap(), stdForm);
		}
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newConceptIRI);
		annotatedValue.setAttribute("role", RDFResourceRole.concept.name());
		//TODO compute show
		return annotatedValue; 
	}
	
	/**
	 * a refactoring service for moving xLabels to an existing concept ( ST-498 )
	 * @param sourceResource
	 * @param predicate
	 * @param xLabel
	 * @param targetResource
	 * @param force set to true to create a new prefLabel for the targetResource even if this creates a conflict 
	 * with another prefLabel belonging to a third resource
	 * @return the newCoceptIRI as an AnnotatedValue
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#sourceResource)+ ', lexicalization)', 'CD')")
	public void moveXLabelToResource(
			@LocallyDefined Resource sourceResource, IRI predicate, @LocallyDefined Resource xLabel,
			@LocallyDefined Resource targetResource,@Optional(defaultValue = "false") Boolean force)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, 
					CODAException, NonExistingLiteralFormForResourceException, AlreadyExistingLiteralFormForResourceException{
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		RepositoryConnection repoConnection = getManagedConnection();
		
		//first found out the predicate used in the sourceResource
		IRI oldPredicate = (IRI)repoConnection.getStatements(sourceResource, null, xLabel).next().getPredicate();
		
		//if the new predicate is skosxl:prefLabel (and the old one is not skosxl:prefLabel) check that
		// there are no other concept having the label associated to this xLabel
		if(predicate.equals(SKOSXL.PREF_LABEL) && !oldPredicate.equals(SKOSXL.PREF_LABEL)) {
			String query = "SELECT ?otherConcept ?otherXLabel ?literalForm" +
					"\nWHERE{" +
					"\n?xLabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?literalForm ."+
					"\n?otherConcept "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?otherXLabel ."+
					"\n?otherXLabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?literalForm ." +
					"\n}";
			TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("xLabel", xLabel);
			try(TupleQueryResult  tupleQueryResult = tupleQuery.evaluate()){
				while(tupleQueryResult.hasNext()) {
					BindingSet bindingSet = tupleQueryResult.next();
					Value otherConcept = bindingSet.getValue("otherConcept");
					Value literal = bindingSet.getValue("literalForm");
					if(!force) {
						String text = "Label " + NTriplesUtil.toNTriplesString(literal) + " cannot be"
								+ " moved as skoxl:prefLabel for " + NTriplesUtil.toNTriplesString(targetResource) 
								+ " since already exists a resource ("+ NTriplesUtil.toNTriplesString(otherConcept) 
								+ ") with the same label as skosxl:prefLabel";
						throw new AlreadyExistingLiteralFormForResourceException(text);
					}
				}
			}
		}
		
		//if the new predicate is skosxl:prefLabel and if the new concept already has a prefLabel with the same 
		// language two thing can happen:
		// - if force is false, then an exception is thrown 
		// - if force is true, then transform the already existing xlabel (the current skosxl:prefLabel) into a skosxl:altLabel
		if(predicate.equals(SKOSXL.PREF_LABEL)) {
			String query = "SELECT ?otherXLabel ?literalForm1" +
					"\nWHERE{" +
					"\n?xLabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?literalForm1 ."+
					"\n"+NTriplesUtil.toNTriplesString(targetResource)+" "+NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL)+" ?otherXLabel ."+
					"\n?otherXLabel "+NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM)+" ?literalForm2 ." +
					"\nFILTER(lang(?literalForm1) = lang(?literalForm2))"+
					"\n}";
			TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("xLabel", xLabel);
			try(TupleQueryResult  tupleQueryResult = tupleQuery.evaluate()){
				while(tupleQueryResult.hasNext()) {
					BindingSet bindingSet = tupleQueryResult.next();
					if(!force) {
						Literal literal = (Literal) bindingSet.getValue("literalForm1");
						String text = "xLabel " + NTriplesUtil.toNTriplesString(literal) + " cannot be"
								+ " moved as skoxl:prefLabel for " + NTriplesUtil.toNTriplesString(targetResource) 
								+ " since there is already a skosxl:prefLabel for the langauge "+literal.getLanguage().get() 
								+". By forcing  this operation, the old skosxl:prefLabel will be set as skosxl:altLabel";
						throw new AlreadyExistingLiteralFormForResourceException(text);
					} else {
						Value otherXLabel = bindingSet.getValue("otherXLabel");
						modelRemovals.add(targetResource, SKOSXL.PREF_LABEL, otherXLabel);
						modelAdditions.add(targetResource, SKOSXL.ALT_LABEL, otherXLabel);
					}
				}
			}
		}
		
		modelRemovals.add(sourceResource, oldPredicate, xLabel);
		modelAdditions.add(targetResource, predicate, xLabel);
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	//copied from the service SKOXL
	/**
	 * Generates a new URI for a SKOS concept, optionally given its accompanying preferred label and concept
	 * scheme. The actual generation of the URI is delegated to {@link #generateURI(String, Map)}, which in
	 * turn invokes the current binding for the extension point {@link URIGenerator}. In the end, the <i>URI
	 * generator</i> will be provided with the following:
	 * <ul>
	 * <li><code>concept</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>label</code> and <code>scheme</code> (each, if
	 * not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param label
	 *            the preferred label accompanying the concept (can be <code>null</code>)
	 * @param schemes
	 *            the schemes to which the concept is being attached at the moment of its creation (can be
	 *            <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptIRI(Literal label, List<IRI> schemes) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}

		if (schemes != null) {
			args.put(URIGenerator.Parameters.schemes,
					SimpleValueFactory.getInstance().createLiteral(TurtleHelp.serializeCollection(schemes)));
		}

		return generateIRI(URIGenerator.Roles.concept, args);
	}
	
	private class ConceptLabelValueGraph{
		private IRI concept;
		private IRI labelType;
		private Literal value;
		private IRI graph;
		
		public ConceptLabelValueGraph(IRI concept, IRI labelType, Literal value, IRI graph) {
			super();
			this.concept = concept;
			this.labelType = labelType;
			this.value = value;
			this.graph = graph;
		}

		public IRI getConcept() {
			return concept;
		}

		public IRI getLabelType() {
			return labelType;
		}

		public Literal getValue() {
			return value;
		}

		public IRI getGraph() {
			return graph;
		}
	}
	
	private class ConceptNoteValueGraph{
		private IRI concept;
		private IRI noteType;
		private Literal value;
		private IRI graph;
		
		public ConceptNoteValueGraph(IRI concept, IRI noteType, Literal value, IRI graph) {
			super();
			this.concept = concept;
			this.noteType = noteType;
			this.value = value;
			this.graph = graph;
		}

		public IRI getConcept() {
			return concept;
		}

		public IRI getNoteType() {
			return noteType;
		}

		public Literal getValue() {
			return value;
		}

		public IRI getGraph() {
			return graph;
		}
	}
}
