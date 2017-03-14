package it.uniroma2.art.semanticturkey.datarange;

import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Statement;

public abstract class DataRangeAbstract {
	
	protected BNode startingBNode;
	
	public BNode getBNode(){
		return startingBNode;
	}
	
	public abstract List<Statement> generateTriples();
}
