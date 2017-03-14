package it.uniroma2.art.semanticturkey.datarange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;

public class ParseDataRange {

	
	
	public static DataRangeAbstract getDataRange(BNode startingBNode, RepositoryConnection conn){

		//see https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Data_Ranges

		//List<IRI> typeIriList = new ArrayList<IRI>();
		List<Literal> literalList = new ArrayList<>();
		IRI typeOfDataRange=null;
		BNode bnodeForDataOneOf=null;
		
		//use a sparql query to obtain all the literal associated to the input bnode with the property 
		// owl:oneOf and check that the input bnode has rdfs:Datatype as its own type
		
		String oneOfString = SPARQLHelp.toSPARQL(OWL.ONEOF);
		String dataypeString = SPARQLHelp.toSPARQL(RDFS.DATATYPE);
		String firstrString = SPARQLHelp.toSPARQL(RDF.FIRST);
		String restString = SPARQLHelp.toSPARQL(RDF.REST);
		String nilString = SPARQLHelp.toSPARQL(RDF.NIL);
		
		// @formatter:off
		String query = "SELECT ?firstBnodeInList ?bnodeInList ?literalValue ?nextBNode" + //change the * with the right variable (or use a CONSTRUCT)
						"\nWHERE{" +
						
						//do a subquery to obtain the main list (linked with owl:oneOf to the inputBNode)
						// and the first element of the list
						"\n{"+
						"\nSELECT ?bnodeList ?firstBnodeInList "+
						"\nWHERE {" +
						"\n?inputBNode 	a 	"+dataypeString+" . "+
						"\n?inputBNode "+oneOfString+" ?bnodeList" +
						//get the first element of the list
						"\n?bnodeList "+restString+" ?firstBnodeInList ."+
						"\n filter isBlank(?firstBnodeInList) " +
						"\n}" +
						"\n}" +

						//now to the outer query
						
						//now get all the elements of the list (use of the '*') and keep the order 
						"\n?bnodeList "+restString+"* ?bnodeInList ."+
						//for each element of the list, take the associated rdf:first and rdf:rest
						// even the last elemnt of the list has the rdf:rest, which is rdf:nil
						"\n?bnodeInList " +firstrString+" ?literalValue ." + 
						"\n?bnodeInList " +restString+" ?nextBNode ." + 
						//check that the only element in the list are bnode and that the values associated 
						// to them, with the property rds:first, are literal
						"\nfilter isBlank(?bnodeInList) " +
						"\nfilter isLiteral(?literalValue)" +
						"\n}";
		// @formatter:on
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("inputBNode", startingBNode);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		
		//create two temp maps
		Map <BNode, Literal> bnodeToLiteralMap = new HashMap<>();
		Map <BNode, BNode> bnodeToNextBNode = new HashMap<>();
		BNode firstBnodeInList=null;
		
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			if(firstBnodeInList==null){
				firstBnodeInList = (BNode)bindingSet.getBinding("firstBnodeInList").getValue();
			}
			BNode bnodeInList = (BNode) bindingSet.getBinding("bnodeInList").getValue();
			Literal  literalValue = (Literal) bindingSet.getBinding("literalValue").getValue();
			Value nextBNode = bindingSet.getBinding("nextBNode").getValue();
			bnodeToLiteralMap.put(bnodeInList, literalValue);
			if(nextBNode instanceof BNode){
				//exclude the last element, which has as next the rdf:nil, that is not a bnode
				bnodeToNextBNode.put(bnodeInList, (BNode) nextBNode);
			}
			
		}
		
		//now construct the ordered list of Literal in the enumeration
		boolean stop = false;
		BNode lastBNodeConsidered = firstBnodeInList;
		while(!stop){
			Literal literal = bnodeToLiteralMap.get(lastBNodeConsidered);
			lastBNodeConsidered = bnodeToNextBNode.get(lastBNodeConsidered);
			literalList.add(literal);
			if(lastBNodeConsidered == null){
				//the last element, in not in the map, since its next is rdf:nil
				stop = true;
			}
		}
		
		return new DataRangeDataOneOf(startingBNode, literalList);
		
	}
}
