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

	/** ct:SYSINFO **/
	public static final IRI SYSINFO;

	/** ct:graph-management */
	public static final IRI GRAPH_MANAGEMENT;

	/** ct:history-graph */
	public static final IRI HISTORY_GRAPH;

	/** ct:validation-graph */
	public static final IRI VALIDATION_GRAPH;
	
	/** ct:blacklist-graph */
	public static final IRI BLACKLIST_GRAPH;

	/** ct:includeGraph */
	public static final IRI INCLUDE_GRAPH;

	/** ct:excludeGraph */
	public static final IRI EXCLUDE_GRAPH;

	/** ct:commit-metadata */
	public static final IRI COMMIT_METADATA;

	/** ct:validation */
	public static final IRI VALIDATION;

	/** ct:accept */
	public static final IRI ACCEPT;

	/** ct:reject */
	public static final IRI REJECT;

	/** ct:enabled */
	public static final IRI ENABLED;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		STAGED_ADDITIONS = vf.createIRI(NAMESPACE, "staged-additions");
		STAGED_REMOVALS = vf.createIRI(NAMESPACE, "staged-removals");

		SYSINFO = vf.createIRI(NAMESPACE, "SYSINFO");

		GRAPH_MANAGEMENT = vf.createIRI(NAMESPACE, "graph-management");
		INCLUDE_GRAPH = vf.createIRI(NAMESPACE, "includeGraph");
		EXCLUDE_GRAPH = vf.createIRI(NAMESPACE, "excludeGraph");
		HISTORY_GRAPH = vf.createIRI(NAMESPACE, "history-graph");
		VALIDATION_GRAPH = vf.createIRI(NAMESPACE, "validation-graph");
		BLACKLIST_GRAPH = vf.createIRI(NAMESPACE, "blacklist-graph");

		COMMIT_METADATA = vf.createIRI(NAMESPACE, "commit-metadata");

		VALIDATION = vf.createIRI(NAMESPACE, "validation");
		ACCEPT = vf.createIRI(NAMESPACE, "accept");
		REJECT = vf.createIRI(NAMESPACE, "reject");
		ENABLED = vf.createIRI(NAMESPACE, "enabled");
	}
}
