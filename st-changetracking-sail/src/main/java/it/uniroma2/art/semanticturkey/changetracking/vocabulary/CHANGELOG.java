package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SESAME;

/**
 * Constant for the Changelog vocabulary used to represent the history of a repository. After the first
 * commit, the <i>history repository</i> is expected to have a shape like the following:
 * <p>
 * 
 * <pre>
 * history:0b3838bd-150d-4621-9477-f2aa6d2f64d7
 *   a cl:Commit ;
 *   cl:status "committed ;
 *   dcterms:created "...."^xsd:datetime ;
 *   cl:addedStatement history:4a24d3a7-2a50-41f5-8fe8-7fcf8304ae7a
 *   .
 * 
 * history:4a24d3a7-2a50-41f5-8fe8-7fcf8304ae7a
 *   a cl:Quadruple ;
 *   cl:subject &lt;http://example.org#john&gt; ;
 *   cl:predicate &lt;http://example.org#loves&gt; ;
 *   cl:object &lt;http://example.org#mary&gt; ;
 *   cl:context &lt;http://example.org&gt;
 *   .
 *   
 * history:bcff850b-7163-4576-9a37-cd2abbdbdf47
 *   a cl:Commit ;
 *   cl:status "committed ;
 *   cl:parentCommit history:0b3838bd-150d-4621-9477-f2aa6d2f64d7 ;
 *   dcterms:created "...."^xsd:datetime ;
 *   cl:addedStatement history:130626bc-e94a-4125-86b5-f1cf3791898f
 *   .
 *   
 * history:130626bc-e94a-4125-86b5-f1cf3791898f
 *   a cl:Quadruple ;
 *   cl:subject &lt;http://example.org#alice&gt; ;
 *   cl:predicate &lt;http://example.org#loves&gt; ;
 *   cl:object &lt;http://example.org#pete&gt; ;
 *   cl:context &lt;http://example.org&gt;
 *   .
 * cl:MASTER cl:tip history:bcff850b-7163-4576-9a37-cd2abbdbdf47 .
 * </pre>
 * <p>
 * The resource <code>cl:MASTER</code> conventionally holds a reference to the tip of the history: i.e. the
 * latest commit. Commits themselves are chained together via the property <code>cl:parentCommit</code>: it
 * connects a commit to the commit that was the tip when the update of the data repository was perfomed.
 * <p>
 * Each commit may be described via a number of metadata properties, and above all it is connected to the
 * triples effectibely added or removed. In the history, the null context is represented via the resource
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
