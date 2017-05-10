package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
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

	/**
	 * An immutable {@link Namespace} constant that represents the CHANGETRACKER namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** ct:staged-additions */
	public static final IRI STAGED_ADDITIONS;
	
	/** ct:staged-removals */
	public static final IRI STAGED_REMOVALS;

	/** ct:graph-management */
	public static final IRI GRAPH_MANAGEMENT;
	
	/** ct:includeGraph */
	public static final IRI INCLUDE_GRAPH;
	
	/** ct:excludeGraph */
	public static final IRI EXCLUDE_GRAPH;

	/** ct:commit-metadata */
	public static final IRI COMMIT_METADATA;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		
		STAGED_ADDITIONS = vf.createIRI(NAMESPACE, "staged-additions");
		STAGED_REMOVALS = vf.createIRI(NAMESPACE, "staged-removals");
		
		GRAPH_MANAGEMENT = vf.createIRI(NAMESPACE, "graph-management");
		INCLUDE_GRAPH = vf.createIRI(NAMESPACE, "includeGraph");
		EXCLUDE_GRAPH = vf.createIRI(NAMESPACE, "excludeGraph");
		
		COMMIT_METADATA = vf.createIRI(NAMESPACE, "commit-metadata");
	}
}
