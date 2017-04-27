package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class OWLIM {

	/** The OWLIM namespace: http://www.ontotext.com/trree/owlim# */
	public static final String NAMESPACE = "http://www.ontotext.com/trree/owlim#";

	/**
	 * Recommended prefix for the OWLIM namespace: "owlim"
	 */
	public static final String PREFIX = "owlim";

	/**
	 * An immutable {@link Namespace} constant that represents the OWLIM namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** owlim:base-URL */
	public static final IRI BASE_URL;

	/** owlim:defaultNS */
	public static final IRI DEFAULT_NS;

	/** owlim:entity-index-size */
	public static final IRI ENTITY_INDEX_SIZE;

	/** owlim:entity-id-size */
	public static final IRI ENTITY_ID_SIZE;

	/** owlim:imports */
	public static final IRI IMPORTS;

	/** owlim:repository-type */
	public static final IRI REPOSITORY_TYPE;

	/** owlim:rule-set */
	public static final IRI RULE_SET;

	/** owlim:storage-folder */
	public static final IRI STORAGE_FOLDER;

	/** owlim:enable-context-index */
	public static final IRI ENABLE_CONTEXT_INDEX;

	/** owlim:enablePredicateList */
	public static final IRI ENABLE_PREDICATE_LIST;

	/** owlim:in-memory-literal-properties */
	public static final IRI IN_MEMORY_LITERAL_PROPERTIES;

	/** owlim:enable-literal-index */
	public static final IRI ENABLE_LITERAL_INDEX;

	/** owlim:check-for-inconsistencies */
	public static final IRI CHECK_FOR_INCONSISTENCIES;

	/** owlim:disable-sameAs */
	public static final IRI DISABLE_SAME_AS;

	/** owlim:query-timeout */
	public static final IRI QUERY_TIMEOUT;

	/** owlim:query-limit-results */
	public static final IRI QUERY_LIMIT_RESULTS;

	/** owlim:throw-QueryEvaluationException-on-timeout */
	public static final IRI THROW_QUERY_EVALUATION_EXCEPTION_ON_TIMEOUT;

	/** owlim:read-only */
	public static final IRI READ_ONLY;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		BASE_URL = vf.createIRI(NAMESPACE, "base-URL");
		DEFAULT_NS = vf.createIRI(NAMESPACE, "defaultNS");
		ENTITY_INDEX_SIZE = vf.createIRI(NAMESPACE, "entity-index-size");
		ENTITY_ID_SIZE = vf.createIRI(NAMESPACE, "entity-id-size");
		IMPORTS = vf.createIRI(NAMESPACE, "imports");
		REPOSITORY_TYPE = vf.createIRI(NAMESPACE, "repository-type");
		RULE_SET = vf.createIRI(NAMESPACE, "rule-set");
		STORAGE_FOLDER = vf.createIRI(NAMESPACE, "storage-folder");
		ENABLE_CONTEXT_INDEX = vf.createIRI(NAMESPACE, "enable-context-index");
		ENABLE_PREDICATE_LIST = vf.createIRI(NAMESPACE, "enablePredicateList");
		IN_MEMORY_LITERAL_PROPERTIES = vf.createIRI(NAMESPACE, "in-memory-literal-properties");
		ENABLE_LITERAL_INDEX = vf.createIRI(NAMESPACE, "enable-literal-index");
		CHECK_FOR_INCONSISTENCIES = vf.createIRI(NAMESPACE, "check-for-inconsistencies");
		DISABLE_SAME_AS = vf.createIRI(NAMESPACE, "disable-sameAs");
		QUERY_TIMEOUT = vf.createIRI(NAMESPACE, "query-timeout");
		QUERY_LIMIT_RESULTS = vf.createIRI(NAMESPACE, "query-limit-results");
		THROW_QUERY_EVALUATION_EXCEPTION_ON_TIMEOUT = vf.createIRI(NAMESPACE,
				"throw-QueryEvaluationException-on-timeout");
		READ_ONLY = vf.createIRI(NAMESPACE, "read-only");

	}
}
