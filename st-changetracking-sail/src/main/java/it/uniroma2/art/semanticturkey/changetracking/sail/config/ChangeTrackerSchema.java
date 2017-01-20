package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class ChangeTrackerSchema {
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/config/sail/changetracker#";
	
	public static final IRI HISTORY_REPOSITORY_ID;
	public static final IRI HISTORY_NS;
	public static final IRI HISTORY_GRAPH;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();
		
		HISTORY_REPOSITORY_ID = vf.createIRI(NAMESPACE, "historyRepositoryID");
		HISTORY_NS = vf.createIRI(NAMESPACE, "historyNS");
		HISTORY_GRAPH = vf.createIRI(NAMESPACE, "historyGraph");
	}
}
