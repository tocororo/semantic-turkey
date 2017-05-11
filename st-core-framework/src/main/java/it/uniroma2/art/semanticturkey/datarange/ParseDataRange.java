package it.uniroma2.art.semanticturkey.datarange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;

public class ParseDataRange {

	
	
	public static DataRangeAbstract getLiteralEnumeration(BNode startingBNode, RepositoryConnection conn){
		//see https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Data_Ranges

		List<Literal> literalList = new ArrayList<>();
		//IRI typeOfDataRange=null;
		//BNode bnodeForDataOneOf=null;
		
		//use a sparql query to obtain all the literal associated to the input bnode with the property 
		// owl:oneOf and check that the input bnode has rdfs:Datatype as its own type
		
		String oneOfString = SPARQLHelp.toSPARQL(OWL.ONEOF);
		String dataypeString = SPARQLHelp.toSPARQL(RDFS.DATATYPE);
		String firstrString = SPARQLHelp.toSPARQL(RDF.FIRST);
		String restString = SPARQLHelp.toSPARQL(RDF.REST);
		//String nilString = SPARQLHelp.toSPARQL(RDF.NIL);
		
		// @formatter:off
		String query = "SELECT ?bnodeList ?bnodeInList ?literalValue ?nextBNode" + 
						"\nWHERE{" +
						
						//do a subquery to obtain the main list (linked with owl:oneOf to the inputBNode)
						// and the first element of the list
						"\n{"+
						"\nSELECT ?bnodeList "+
						"\nWHERE {" +
						"\n?inputBNode 	a 	"+dataypeString+" . "+
						"\n?inputBNode "+oneOfString+" ?bnodeList ." +
						//?gnodeList is the first element of the list (and the list itself, since this is how 
						//list are in RDF)
						"\n filter isBlank(?bnodeList) " +
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
				firstBnodeInList = (BNode)bindingSet.getBinding("bnodeList").getValue();
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
		
		if(bnodeToNextBNode.isEmpty()){
			//the query found no solution, so return and empty list
			return new DataRangeDataOneOf(startingBNode, literalList, conn);
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
		
		return new DataRangeDataOneOf(startingBNode, literalList, conn);
	}
	
	
	public static DataRangeAbstract getLiteralEnumeration(BNode startingBNode, Model statements, 
			RepositoryConnection conn){
		//see https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Data_Ranges
		
		List<Literal> literalList = new ArrayList<>();
		
		//consult the input repository to obtain all the RDF Literals in the List linked to the input BNode
		// using the property OWL.ONEOF (and the input bnode should have the type RDFS.DATATYPE)
		Optional<Resource> optionalRes = Models.objectResource(statements.filter(startingBNode, OWL.ONEOF, null));
		if(!optionalRes.isPresent()){
			return new DataRangeDataOneOf(startingBNode, literalList, conn);
		}
		BNode listBNode = (BNode)optionalRes.get();
		
		
		for(Value value : RDFCollections.asValues(statements, listBNode, new ArrayList<Value>()) ){
			if(value instanceof Literal){
				literalList.add((Literal) value);
			}
			else{
				//TODO
				//the element in the list is not a literal, this should never happen, decide what to do
			}
		}
		
		return new DataRangeDataOneOf(startingBNode, literalList, conn);
		
	}
}
