package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class ChangeTrackerSchema {
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/config/sail/changetracker#";

	public static final IRI HISTORY_REPOSITORY_ID;
	public static final IRI HISTORY_NS;
	public static final IRI HISTORY_GRAPH;

	public static final IRI INCLUDE_GRAPH;
	public static final IRI EXCLUDE_GRAPH;

	public static final IRI INTERACTIVE_NOTIFICATIONS;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		HISTORY_REPOSITORY_ID = vf.createIRI(NAMESPACE, "historyRepositoryID");
		HISTORY_NS = vf.createIRI(NAMESPACE, "historyNS");
		HISTORY_GRAPH = vf.createIRI(NAMESPACE, "historyGraph");
		INCLUDE_GRAPH = vf.createIRI(NAMESPACE, "includeGraph");
		EXCLUDE_GRAPH = vf.createIRI(NAMESPACE, "excludeGraph");
		INTERACTIVE_NOTIFICATIONS = vf.createIRI(NAMESPACE, "interactiveNotifications");
	}
}
