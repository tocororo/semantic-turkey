package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;

/**
 * This class provides services to convert SKOS to SKOSXL data and SKOSXL to SKOS.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */

@STService
public class Refactor2 extends STServiceAdapter  {
	
	private static Logger logger = LoggerFactory.getLogger(Refactor2.class);

	/**
	 * it refactor SKOS data into SKOSXL
	 * @return
	 * @throws URIGenerationException 
	 */
	@STServiceOperation
	@Write
	public void SKOStoSKOSXL() throws URIGenerationException{
		logger.info("request to refactor SKOS data to SKOSXL");
		
		List<ConceptLabelValueGraph> conceptLabelValueGraphList = new ArrayList<ConceptLabelValueGraph>();
		List<ConceptNoteValueGraph> conceptNoteValueGraphList = new ArrayList<ConceptNoteValueGraph>();
		
		IRI workingGraph = (IRI)getWorkingGraph();
		String workingGraphString = SPARQLHelp.toSPARQL(workingGraph);
		
		//first of all, take the SKOS data you need to refactor
		
		//get all the info regarding the labels(pref/alt/hidden)
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
		
		//get all the info regarding the notes (and its subproperties)
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
		
		//now iterate over the two lists (labels and note) to construct the necessary SKOSXL construct
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
			valueMapping.put(URIGenerator.Parameters.type, skosxlLabelType);
			IRI newIRIForLabel = generateIRI(URIGenerator.Roles.xLabel, valueMapping );
			//now add the new xlabel and remove the old data regarding SKOS
			String graphString = SPARQLHelp.toSPARQL(graph); 
			String conceptString = SPARQLHelp.toSPARQL(concept);
			String labelTypeString = SPARQLHelp.toSPARQL(labelType);
			String skoxlLabelTypeString = SPARQLHelp.toSPARQL(skosxlLabelType);
			String valueString = SPARQLHelp.toSPARQL(value);
					
			String newIRIForLabelString = SPARQLHelp.toSPARQL(newIRIForLabel);//"<"+newIRIForLabel.stringValue()+">";
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
			Update update = getManagedConnection().prepareUpdate(updateQuery);
			
			dataset = new SimpleDataset();
			
			contextList = QueryResults.asList(getManagedConnection().getContextIDs());
			//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT
			dataset.addNamedGraph(workingGraph);
			update.setDataset(dataset);
			//execute the UPDATE
			update.execute();
			
		}
		
		//now the notes list
		for(ConceptNoteValueGraph conceptNoteValueGraph : conceptNoteValueGraphList){
			IRI concept = conceptNoteValueGraph.getConcept();
			IRI noteType = conceptNoteValueGraph.getNoteType();
			Literal value = conceptNoteValueGraph.getValue();
			IRI graph = conceptNoteValueGraph.getGraph();
			
			Map<String, Value> valueMapping = new HashMap<String, Value>();
			valueMapping.put(URIGenerator.Parameters.lexicalForm, value);
			valueMapping.put(URIGenerator.Parameters.lexicalizedResource, concept);
			valueMapping.put(URIGenerator.Parameters.type, noteType);
			IRI newIRIForNote = generateIRI(URIGenerator.Roles.xNote, valueMapping );
			//now add the new xNote and remove the old data regarding SKOS
			String graphString = SPARQLHelp.toSPARQL(graph); 
			String conceptString = SPARQLHelp.toSPARQL(concept);
			String noteTypeString = SPARQLHelp.toSPARQL(noteType);
			String valueString = SPARQLHelp.toSPARQL(value); 
					
			String newIRIForNoteString = SPARQLHelp.toSPARQL(newIRIForNote); 
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
	
	/**
	 * it refactor SKOSXL data into SKOS
	 * @return
	 */
	@STServiceOperation
	@Write
	public void SKOSXLtoSKOS(){
		logger.info("request to refactor SKOSXL data to SKOS");
		
		IRI workingGraph = (IRI) getWorkingGraph();
		String workingGraphString = SPARQLHelp.toSPARQL(workingGraph);
		
		// @formatter:off
		String queryUpdate = 
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
				"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" + 
				//remove the SKOSXL data
				"DELETE { \n"+
				//the part relative to skos:note (and its sub properties)
				"GRAPH "+workingGraphString+" {?concept ?propNote ?reifiedNote . \n" +
				"?reifiedNote rdf:value ?value . } \n" +
				
				///the related to the labels (Pref, Alt and Hidden)
				"GRAPH "+workingGraphString+" {?genericSubj ?genericProp1 ?label . \n " +
				"?label ?genericProp2 ?genericObj . }\n" +
				
				"} \n " +
				
				//insert the SKOS data
				"INSERT { \n" +
				"GRAPH "+workingGraphString+"{?concept skos:prefLabel ?prefLabelLitForm . } \n " +
				"GRAPH "+workingGraphString+"{?concept skos:altLabel ?altLabelLitForm . } \n" +
				"GRAPH "+workingGraphString+"{?concept skos:hiddenLabel ?hiddenLabelLitForm . } \n" +
				"GRAPH "+workingGraphString+"{?concept ?propNote ?value .} \n" +
				"} \n" +
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
				"}\n" +

				//the notes (and its sub properties) part
				"UNION \n"+
				"{?propNote rdfs:subPropertyOf* skos:note . \n" + 
				"GRAPH "+workingGraphString+"{?concept ?propNote ?reifiedNote . \n" + 
				"?reifiedNote rdf:value ?value .} \n" +
				"} \n" +
				
				"}";
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
