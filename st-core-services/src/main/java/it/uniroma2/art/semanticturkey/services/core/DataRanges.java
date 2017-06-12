package it.uniroma2.art.semanticturkey.services.core;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import it.uniroma2.art.semanticturkey.datarange.DataRangeAbstract;
import it.uniroma2.art.semanticturkey.datarange.DataRangeDataOneOf;
import it.uniroma2.art.semanticturkey.datarange.ParseDataRange;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
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

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public AnnotatedValue<BNode> createLiteralEnumeration(@Optional BNode bnode, Literal [] literalsArray) {
		
		logger.info("createLiteralEnumeration");
		
		if(bnode==null){
			bnode = getManagedConnection().getValueFactory().createBNode();
		}
		
		List<Literal> literalList = Arrays.asList(literalsArray);
		
		RepositoryConnection conn = getManagedConnection();
		
		DataRangeDataOneOf dataRangeDataOneOf = new DataRangeDataOneOf(bnode, literalList, conn.getValueFactory());

		List<Statement> triplesList = dataRangeDataOneOf.generateTriples();
		
		conn.add(triplesList, getWorkingGraph());
		
		AnnotatedValue<BNode> annotatedValue = new AnnotatedValue<BNode>(bnode);
		return annotatedValue;
	}
	
	@STServiceOperation
	@Read
	// TODO @PreAuthorize
	public Collection<AnnotatedValue<Literal>> getLiteralEnumeration(BNode bnode){
		logger.info("getLiteralEnumeration");
		Collection<AnnotatedValue<Literal>> literalList = new ArrayList<AnnotatedValue<Literal>>();
		
		DataRangeDataOneOf dataOneOf = null;
		DataRangeAbstract dataRangeAbstract = ParseDataRange.getLiteralEnumeration(bnode, getManagedConnection());
		if(dataRangeAbstract instanceof DataRangeDataOneOf){
			dataOneOf = (DataRangeDataOneOf) dataRangeAbstract;
		} else{
			//There was an error, since the bnode is not the expected datarange (ONEOF)
			//TODO decide what to do, at the moment, return an empty list
			return literalList;
		}
		
		List<Literal> literalTempList = dataOneOf.getLiteralList();
		
		for(Literal literal : literalTempList){
			literalList.add(new AnnotatedValue<Literal>(literal));
		}
		
		return literalList;
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void addLiteralToEnumeration(BNode bnode, Literal literal){
		//prepare a SPARQL update query, which takes the last element in the list (the ones having rdf:nil 
		// at the value for rdf:rest) and 
		
		String oneOfString = SPARQLHelp.toSPARQL(OWL.ONEOF);
		String dataypeString = SPARQLHelp.toSPARQL(RDFS.DATATYPE);
		String firstString = SPARQLHelp.toSPARQL(RDF.FIRST);
		String restString = SPARQLHelp.toSPARQL(RDF.REST);
		String nilString = SPARQLHelp.toSPARQL(RDF.NIL);
		String typeString = SPARQLHelp.toSPARQL(RDF.TYPE);
		String listString = SPARQLHelp.toSPARQL(RDF.LIST);
		String literalString = SPARQLHelp.toSPARQL(literal);
		
		//String baseURI = SPARQLHelp.toSPARQL(getWorkingGraph());
		
		BNode newBnode = getManagedConnection().getValueFactory().createBNode();
		//String newBnodeString = SPARQLHelp.toSPARQL(newBnode);
		
		String updateQuery = 
				"DELETE {"+
				"\n GRAPH ?g {" +
				"\n?lastBNode "+restString+" "+nilString+" ."+
				"\n}" +
				"\n}"+
				"\nINSERT {"+
				"\nGRAPH ?g {"+
				"\n?lastBNode "+restString+" ?newBNode ."+
				"\n?newBNode "+typeString+" "+listString +" ."+
				"\n?newBNode "+firstString+" "+literalString+" ." +
				"\n?newBNode "+restString+" "+nilString+" ." + 
				"\n}"+
				"\n}"+
				"WHERE {"+
				"\nGRAPH ?g {" +
				"\n?inputBNode 	a 	"+dataypeString+" . "+
				"\n?inputBNode "+oneOfString+" ?bnodeList ." +
				"\n?bnodeList "+restString+"* ?lastBNode ." +
				"\n?lastBNode "+restString+" "+nilString+" ."+
				"\n}" +
				"\n}";
		
		Update update= getManagedConnection().prepareUpdate(updateQuery);
		update.setBinding("inputBNode", bnode);
		update.setBinding("newBNode", newBnode);
		update.execute();
	}
	
	
	@STServiceOperation
	@Read
	// TODO @PreAuthorize
	public JsonNode hasLiteralInEnumeration(BNode bnode, Literal literal){
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
		booleanQuery.setBinding("inputBNode", bnode);
		boolean booleanReturn = booleanQuery.evaluate();
		
		return JsonNodeFactory.instance.booleanNode(booleanReturn);
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void removeLiteralFromEnumeration(BNode bnode, Literal literal){
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
				//add the connection to the previous element to the next element of the list or 
				// add the OWL.ONEOF between the next element and the inputBNode
				"\nGRAPH "+baseURI+" {" +
				"\n?prevBNodeInList "+restString+" ?nextBnodeInList ." +
				"\n?datatypeBnode "+oneOfString+" ?nextBnodeInList ." +
				"\n}" + 
				"\n}" +

				"\nWHERE {"+
				"\n?inputBNode a "+dataypeString+" . "+
				"\n?inputBNode "+oneOfString+" ?bnodeList ." +
				
				"\n?bnodeList "+restString+"* ?bnodeInList ." +
				"\n?bnodeInList "+firstString+" "+literalString+" ."+
				
				//get the next element (a normal element or rdf:nil)
				"\n?bnodeInList "+restString+" ?nextBnodeInList ." +
				//if it is not the first element of the list
				"\nOPTIONAL{?prevBNodeInList "+restString+" ?bnodeInList . }"+ 
				//if it is the first element of the list
				"\nOPTIONAL{?datatypeBnode "+oneOfString+" ?bnodeInList . }"+
				
				"\n}";
		Update update = getManagedConnection().prepareUpdate(updateQuery);
		update.setBinding("inputBNode", bnode);
		update.execute();
				
		
	}
	
}
