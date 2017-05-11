package it.uniroma2.art.semanticturkey.datarange;

import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public abstract class DataRangeAbstract {
	
	protected BNode startingBNode;
	
	RepositoryConnection conn;
	
	public BNode getBNode(){
		return startingBNode;
	}
	
	public abstract List<Statement> generateTriples();
}
