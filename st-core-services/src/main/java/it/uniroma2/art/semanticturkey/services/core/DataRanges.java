package it.uniroma2.art.semanticturkey.services.core;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import it.uniroma2.art.semanticturkey.datarange.DataRangeAbstract;
import it.uniroma2.art.semanticturkey.datarange.DataRangeDataOneOf;
import it.uniroma2.art.semanticturkey.datarange.ParseDataRange;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;

/**
 * This class provides services for dealing with DataRanges .
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class DataRanges extends STServiceAdapter{

	
	private static Logger logger = LoggerFactory.getLogger(DataRanges.class);

	@STServiceOperation
	@Write
	public void createLiteralEnumeration(BNode bnode, Literal [] literalsArray) {
		
		logger.info("createLiteralEnumeration");
		
		List<Literal> literalList = Arrays.asList(literalsArray);
		
		DataRangeDataOneOf dataRangeDataOneOf = new DataRangeDataOneOf(bnode, literalList);

		List<Statement> triplesList = dataRangeDataOneOf.generateTriples();
		
		String baseURI = SPARQLHelp.toSPARQL(getWorkingGraph());
		
		String insertquery = "INSERT DATA {" + 
				"\nGRAPH "+baseURI+" {";
		for(Statement statement : triplesList) {
			String subj = SPARQLHelp.toSPARQL(statement.getSubject());
			String pred = SPARQLHelp.toSPARQL(statement.getPredicate());
			String obj = SPARQLHelp.toSPARQL(statement.getObject());
			insertquery += "\n"+subj+" "+pred+" "+obj+" .";
		}
		insertquery += "\n}" +
				"\n}";

		Update update = getManagedConnection().prepareUpdate(insertquery);
		update.execute();
		
	}
	
	@STServiceOperation
	@Read
	public List<Literal> getLiteralEnumeration(BNode bnode){
		logger.info("getLiteralEnumeration");
		List<Literal> literalList = new ArrayList<Literal>();
		
		DataRangeDataOneOf dataOneOf = null;
		DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration(bnode, getManagedConnection());
		if(dataRangeAbstract instanceof DataRangeDataOneOf){
			dataOneOf = (DataRangeDataOneOf) dataRangeAbstract;
		} else{
			//There was an error, since the bnode is not the expected datarange (ONEOF)
			//TODO decide what to do, at the moment, return an empty list
			return literalList;
		}
		
		literalList = dataOneOf.getLiteralList();
		
		return literalList;
	}
	
	@STServiceOperation
	@Write
	public void addLiteralToEnumeration(BNode bNode, Literal literal){
		//prepare a SPARQL update query, which takes the last element in the list (the ones having rdf:nil 
		// at the value for rdf:rest) and 
		
		String oneOfString = SPARQLHelp.toSPARQL(OWL.ONEOF);
		String dataypeString = SPARQLHelp.toSPARQL(RDFS.DATATYPE);
		String firstString = SPARQLHelp.toSPARQL(RDF.FIRST);
		String restString = SPARQLHelp.toSPARQL(RDF.REST);
		String nilString = SPARQLHelp.toSPARQL(RDF.NIL);
		String literalString = SPARQLHelp.toSPARQL(literal);
		
		String baseURI = SPARQLHelp.toSPARQL(getWorkingGraph());
		
		BNode newBnode = getManagedConnection().getValueFactory().createBNode();
		//String newBnodeString = SPARQLHelp.toSPARQL(newBnode);
		
		String updateQuery = 
				"DELETE {"+
				"\n?lastElem "+restString+" "+nilString+" ."+
				"\n}"+
				"INSERT {"+
				"\nGRAPH "+baseURI+"{"+
				"\n?lastElem "+restString+" ?newBNode ."+
				"\n?newBNode "+firstString+" "+literalString+" ." +
				"\n?newBNode "+restString+" "+nilString+" ." + 
				"\n}"+
				"\n}"+
				"WHERE {"+
				"\n?inputBNode 	a 	"+dataypeString+" . "+
				"\n?inputBNode "+oneOfString+" ?bnodeList ." +
				"\n?bnodeList "+restString+"* ?lastBNode ." +
				"\n?lastBNode "+restString+" "+nilString+" ."+
				"\n" +
				"\n}";
		Update update= getManagedConnection().prepareUpdate(updateQuery);
		update.setBinding("inputBNode", bNode);
		update.setBinding("newBnode", newBnode);
		update.execute();
	}
	
	
	@STServiceOperation
	@Read
	public JsonNode hasLieralInEnumeration(BNode bNode, Literal literal){
		//check if there is at least one element in the enumeration with the specified literal
		// using a single SPARQL query
		
		String oneOfString = SPARQLHelp.toSPARQL(OWL.ONEOF);
		String dataypeString = SPARQLHelp.toSPARQL(RDFS.DATATYPE);
		String firstString = SPARQLHelp.toSPARQL(RDF.FIRST);
		String restString = SPARQLHelp.toSPARQL(RDF.REST);
		//String nilString = SPARQLHelp.toSPARQL(RDF.NIL);
		String literalString = SPARQLHelp.toSPARQL(literal);
		
		String query = 
				"ASK"+
				"\nWHERE {"+
				"\n?inputBNode a "+dataypeString+" . "+
				"\n?inputBNode "+oneOfString+" ?bnodeList ." +
				"\n?bnodeList "+restString+"* ?bnodeInList ." +
				"\n?bnodeInList "+firstString+" "+literalString+" ."+
				"\n}";
		BooleanQuery booleanQuery = getManagedConnection().prepareBooleanQuery(query);
		booleanQuery.setBinding("inputBNode", bNode);
		boolean booleanReturn = booleanQuery.evaluate();
		
		return JsonNodeFactory.instance.booleanNode(booleanReturn);
	}
	
	
	@STServiceOperation
	@Write
	public void removeLiteralFromEnumeration(BNode bNode, Literal literal){
		//remove ALL the element of the list of owl:oneOf having the specified literal
		
		String oneOfString = SPARQLHelp.toSPARQL(OWL.ONEOF);
		String dataypeString = SPARQLHelp.toSPARQL(RDFS.DATATYPE);
		String firstString = SPARQLHelp.toSPARQL(RDF.FIRST);
		String restString = SPARQLHelp.toSPARQL(RDF.REST);
		String listString = SPARQLHelp.toSPARQL(RDF.LIST);
		//String nilString = SPARQLHelp.toSPARQL(RDF.NIL);
		String literalString = SPARQLHelp.toSPARQL(literal);
		
		String baseURI = SPARQLHelp.toSPARQL(getWorkingGraph());
		
		String updateQuery = 
				"DELETE {" +
				"\n?bnodeInList a "+listString+" ."+
				"\n?bnodeInList "+firstString+" "+literalString+" ."+
				// delete the connection to the next element
				"\n?bnodeInList "+restString+" ?nextBnodeInList ." + 
				//delete the connection to the previous element or to the inputBNode
				"\n?prevBNodeInList "+restString+" ?bnodeInList . "+ 
				"\n?datatypeBnode "+oneOfString+" ?bnodeInList . "+
				"\n}"+

				"\nINSERT {" +
				//add the connection to the previous element to the next element of the list
				"\nGRAPH "+baseURI+" {" +
				"\n?prevBNodeInList "+restString+" nextBnodeInList ." +
				"\n?datatypeBnode "+oneOfString+" nextBnodeInList ." +
				"\n}" + 
				"\n}" +

				"\nWHERE {"+
				"\n?inputBNode a "+dataypeString+" . "+
				"\n?inputBNode "+oneOfString+" ?bnodeList ." +
				
				"\n?bnodeList "+restString+"* ?bnodeInList ." +
				"\n?bnodeInList "+firstString+" "+literalString+" ."+
				
				//get the next element (a normal elemento or rdf:nil)
				"\n?bnodeInList "+restString+" ?nextBnodeInList ." +
				//if it is not the first element of the list
				"\nOPTIONAL{?prevBNodeInList "+restString+" ?bnodeInList . }"+ 
				//if it is the first element of the list
				"\nOPTIONAL{?datatypeBnode "+oneOfString+" ?bnodeInList . }"+
				
				"\n}";
		Update update = getManagedConnection().prepareUpdate(updateQuery);
		update.setBinding("inputBNode", bNode);
		update.execute();
				
		
	}
	
}
