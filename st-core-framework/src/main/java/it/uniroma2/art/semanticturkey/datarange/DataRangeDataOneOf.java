package it.uniroma2.art.semanticturkey.datarange;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;


public class DataRangeDataOneOf extends DataRangeAbstract{
	private List <Literal> literalList;
	
	public DataRangeDataOneOf(BNode bnode, List <Literal> literalList) {
		this.startingBNode = bnode;
		this.literalList = literalList;
	}
	
	public List<Literal> getLiteralList(){
		return literalList;
	}
	
	
	@Override
	public List<Statement> generateTriples(){
		//see https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Enumeration_of_Literals
		
		List<Statement> statementsList = new ArrayList<Statement>();
		if(literalList.isEmpty()){
			//the input list is empty, so create an empty list of triples
			//TODO decide if this is the right thing to do
			return statementsList;
		}
		
		BNode oneOfBNode = SimpleValueFactory.getInstance().createBNode();
		//add the info: _:x rdf:type rdfs:Datatype .
		statementsList.add(SimpleValueFactory.getInstance().createStatement(startingBNode, RDF.TYPE, RDFS.DATATYPE));
		//add the info: _:x owl:oneOf _:y .
		statementsList.add(SimpleValueFactory.getInstance().createStatement(startingBNode, OWL.ONEOF, oneOfBNode));
		statementsList.add(SimpleValueFactory.getInstance().createStatement(oneOfBNode, RDF.TYPE, RDF.LIST));
		
		//add the list of literal in the form: _:x owl:oneOf LIST
		BNode lastBNode = oneOfBNode;
		boolean firstAdd = true;
		for(Literal literal : literalList){
			lastBNode = addToList(lastBNode, literal, statementsList, firstAdd);
			firstAdd = false;
		}
		closeList(lastBNode, statementsList);
		
		return statementsList;
	}
	
	
	protected BNode addToList(BNode bnode, Value value, List<Statement> statementsList, boolean firstAdd){
		BNode lastBNode = bnode;
		if(!firstAdd){
			BNode newBNode = SimpleValueFactory.getInstance().createBNode();
			statementsList.add(SimpleValueFactory.getInstance().createStatement(lastBNode, RDF.REST, newBNode));
			statementsList.add(SimpleValueFactory.getInstance().createStatement(newBNode, RDF.TYPE, RDF.LIST));
			lastBNode = newBNode;
		}
		statementsList.add(SimpleValueFactory.getInstance().createStatement(lastBNode, RDF.FIRST, value));
		return lastBNode;
	}
	
	protected void closeList(BNode bnode, List<Statement> statementsList){
		statementsList.add(SimpleValueFactory.getInstance().createStatement(bnode, RDF.REST, RDF.NIL));
		
	}
}