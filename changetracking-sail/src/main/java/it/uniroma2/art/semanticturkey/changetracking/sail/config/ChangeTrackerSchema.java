package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class ChangeTrackerSchema {
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/config/sail/changetracker#";

	public static final IRI SERVER_URL;
	public static final IRI SUPPORT_REPOSITORY_ID;
	public static final IRI METADATA_NS;
	public static final IRI HISTORY_GRAPH;

	public static final IRI INCLUDE_GRAPH;
	public static final IRI EXCLUDE_GRAPH;

	public static final IRI HISTORY_ENABLED;
	public static final IRI VALIDATION_ENABLED;
	public static final IRI BLACKLISTING_ENABLED;

	public static final IRI INTERACTIVE_NOTIFICATIONS;

	public static final IRI VALIDATION_GRAPH;
	public static final IRI BLACKLIST_GRAPH;

	public static final IRI UNDO_ENABLED;


	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		SERVER_URL = vf.createIRI(NAMESPACE, "serverURL");
		SUPPORT_REPOSITORY_ID = vf.createIRI(NAMESPACE, "supportRepositoryID");
		METADATA_NS = vf.createIRI(NAMESPACE, "metadataNS");
		HISTORY_GRAPH = vf.createIRI(NAMESPACE, "historyGraph");
		INCLUDE_GRAPH = vf.createIRI(NAMESPACE, "includeGraph");
		EXCLUDE_GRAPH = vf.createIRI(NAMESPACE, "excludeGraph");
		HISTORY_ENABLED = vf.createIRI(NAMESPACE, "historyEnabled");
		VALIDATION_ENABLED = vf.createIRI(NAMESPACE, "validationEnabled");
		INTERACTIVE_NOTIFICATIONS = vf.createIRI(NAMESPACE, "interactiveNotifications");
		VALIDATION_GRAPH = vf.createIRI(NAMESPACE, "validationGraph");
		BLACKLISTING_ENABLED = vf.createIRI(NAMESPACE, "blacklistEnabled");
		BLACKLIST_GRAPH = vf.createIRI(NAMESPACE, "blacklistGraph");
		UNDO_ENABLED = vf.createIRI(NAMESPACE, "undoEnabled");
	}
}
