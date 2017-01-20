package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * Constant for the Change Tracker vocabulary used to interact with the {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class CHANGETRACKER {
	/** http://semanticturkey.uniroma2.it/ns/change-tracker# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/change-tracker#";

	/**
	 * Recommended prefix for the CHANGETRACKER namespace: "ct"
	 */
	public static final String PREFIX = "ct";

	/** ct:stagedAdditions */
	public static final IRI STAGED_ADDITIONS;
	
	/** ct:stagedRemovals */
	public static final IRI STAGED_REMOVALS;

	/** ct:graphManagement */
	public static final IRI GRAPH_MANAGEMENT;
	
	/** ct:includeGraph */
	public static final IRI INCLUDE_GRAPH;
	
	/** ct:excludeGraph */
	public static final IRI EXCLUDE_GRAPH;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		
		STAGED_ADDITIONS = vf.createIRI(NAMESPACE, "staged-additions");
		STAGED_REMOVALS = vf.createIRI(NAMESPACE, "staged-removals");
		
		GRAPH_MANAGEMENT = vf.createIRI(NAMESPACE, "graph-management");
		INCLUDE_GRAPH = vf.createIRI(NAMESPACE, "includeGraph");
		EXCLUDE_GRAPH = vf.createIRI(NAMESPACE, "excludeGraph");
	}
}
