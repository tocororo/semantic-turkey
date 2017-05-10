package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SESAME;

/**
 * Constant for the Changelog vocabulary used to represent the history of a repository. After the first
 * commit, the <i>history repository</i> is expected to have a shape like the following:
 * <p>
 * 
 * <pre>
 * {@code
 * 	<!-- the example below assumes the additional metadata specified by ST services -->
 * 
 *	<http://example.org/history#fec31f27-00b2-4a21-ac0f-e1d72ae90f4a> a cl:Commit ;
 *		prov:startedAtTime "2017-05-09T22:39:51.410+02:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
 *		prov:endedAtTime "2017-05-09T22:39:51.420+02:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
 *		prov:generated <http://example.org/history#72de0bd1-b132-4ec1-aa36-859df633d910> ;
 *		prov:qualifiedAssociation [
 *			prov:agent <http://semanticturkey.uniroma2.it/ns/users/TestUser> ;
 *			prov:hadRole <http://semanticturkey.uniroma2.it/ns/tracking/performer> .
 *		] ;
 *		cl:status "committed" .
 *
 *	<http://example.org/history#72de0bd1-b132-4ec1-aa36-859df633d910> cl:addedStatement <http://example.org/history#b3213db9-d52b-4985-9fbe-5201d1173785> ;
 *		a prov:Entity ;
 *		prov:generatedAtTime "2017-05-09T22:39:51.419+02:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
 *		prov:wasGeneratedBy <http://example.org/history#fec31f27-00b2-4a21-ac0f-e1d72ae90f4a> .
 *		
 *	<http://example.org/history#b3213db9-d52b-4985-9fbe-5201d1173785> a cl:Quadruple ;
 *		cl:context <http://example.org/graph-A> ;
 *		cl:object <http://xmlns.com/foaf/0.1/Person> ;
 *		cl:predicate <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ;
 *		cl:subject <http://example.org/socrates> .	
 *
 *	<http://example.org/history#7d1110b5-8542-430a-b940-7d0868d204d3> a cl:Commit ;
 *		prov:startedAtTime "2017-05-09T22:39:51.760+02:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
 *		prov:endedAtTime "2017-05-09T22:39:51.762+02:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
 *		prov:generated <http://example.org/history#4480a891-17f4-4dc9-965a-7ce1aa877b14> ;
 *		prov:qualifiedAssociation [
 *			prov:agent <http://semanticturkey.uniroma2.it/ns/users/TestUser> ;
 *			prov:hadRole <http://semanticturkey.uniroma2.it/ns/tracking/performer> .
 *		] ;
 * 		cl:status "committed" ;
 *		cl:parentCommit <http://example.org/history#fec31f27-00b2-4a21-ac0f-e1d72ae90f4a> .

 *	<http://example.org/history#4480a891-17f4-4dc9-965a-7ce1aa877b14> a prov:Entity ;
 *		prov:generatedAtTime "2017-05-09T22:39:51.761+02:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
 *		prov:wasGeneratedBy <http://example.org/history#7d1110b5-8542-430a-b940-7d0868d204d3> ;
 *		cl:removedStatement <http://example.org/history#b8931f7e-c1c0-4b28-8f1f-fc88ae0dccd7> .
 *	
 *	<http://example.org/history#b8931f7e-c1c0-4b28-8f1f-fc88ae0dccd7> a cl:Quadruple ;
 *		cl:context <http://example.org/graph-A> ;
 *		cl:object <http://xmlns.com/foaf/0.1/Person> ;
 *		cl:predicate <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ;
 *		cl:subject <http://example.org/socrates> .

 *
 * 	cl:MASTER cl:tip <http://example.org/history#7d1110b5-8542-430a-b940-7d0868d204d3> .
 * }
 * </pre>
 * <p>
 * The resource <code>cl:MASTER</code> conventionally holds a reference to the tip of the history: i.e. the
 * latest commit. Commits themselves are chained together via the property <code>cl:parentCommit</code>: it
 * connects a commit to the commit that was the tip when the update of the data repository was perfomed.
 * <p>
 * Each commit may be described via a number of metadata properties, and above all it is connected to the
 * triples effectively added or removed. In the history, the null context is represented via the resource
 * {@link SESAME#NIL}.
 * <p>
 * Usually, the metadata about changes to the data repository are recorded before of the actual change to the
 * data. This approach guarantees that even in face of failures of the history repository no change can be
 * lost in the history. The property <code>cl:status</code> is used to indicate that a change described by a
 * <code>cl:Commit</code> has been effectively committed to the data repository.
 * <p>
 * Usually, only the tip of the MASTER can be in an uncommitted state, because new commits are forbidden until
 * the tip is committed. An uncommitted tip may indicate a change on fly against the data repository, or that
 * there as been a failure:
 * <ol>
 * <li>the data repository has been updated, but the history repository could not update the status of the
 * commit</li>
 * <li>the data repository failed to commit, but the history repository failed to undo the changes to the
 * history</li>
 * </ol>
 * <p>
 * Therefore, if we exclude an operation on fly, an &quot;uncommitted&quot; indicates an uncertainty on
 * whether or not data have been effectively recorded.
 * 
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

	/**
	 * An immutable {@link Namespace} constant that represents the CHANGELOG namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
