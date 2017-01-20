package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constant for the Changelog vocabulary used to represent the history of a repository.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class CHANGELOG {
	/** http://semanticturkey.uniroma2.it/ns/changelog# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/changelog#";

	/**
	 * Recommended prefix for the CHANGETRACKER namespace: "cl"
	 */
	public static final String PREFIX = "cl";

	public static final IRI QUADRUPLE;
	public static final IRI SUBJECT;
	public static final IRI PREDICATE;
	public static final IRI OBJECT;
	public static final IRI CONTEXT;
	
	public static final IRI REMOVED_STATEMENT;
	public static final IRI ADDED_STATEMENT;
	
	public static final IRI COMMIT;
	public static final IRI PARENT_COMMIT;
	public static final IRI STATUS;

	public static final IRI MASTER;
	public static final IRI TIP;


	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		QUADRUPLE = vf.createIRI(NAMESPACE, "Quadruple");
		SUBJECT = vf.createIRI(NAMESPACE, "subject");
		PREDICATE = vf.createIRI(NAMESPACE, "predicate");
		OBJECT = vf.createIRI(NAMESPACE, "object");
		CONTEXT = vf.createIRI(NAMESPACE, "context");
		
		REMOVED_STATEMENT = vf.createIRI(NAMESPACE, "removedStatement");
		ADDED_STATEMENT = vf.createIRI(NAMESPACE, "addedStatement");
		
		COMMIT = vf.createIRI(NAMESPACE, "Commit");
		PARENT_COMMIT = vf.createIRI(NAMESPACE, "parentCommit");
		STATUS = vf.createIRI(NAMESPACE, "status");
		
		MASTER = vf.createIRI(NAMESPACE, "MASTER");
		TIP = vf.createIRI(NAMESPACE, "tip");
	}
}
