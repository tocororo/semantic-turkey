package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * Constants for the Blacklist vocabulary used by the {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class BLACKLIST {
	/** http://semanticturkey.uniroma2.it/ns/blacklist# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/blacklist#";

	/**
	 * Recommended prefix for the BLACKLIST namespace: "blacklist"
	 */
	public static final String PREFIX = "blacklist";

	/**
	 * An immutable {@link Namespace} constant that represents the BLACKLIST namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** blacklist:BlacklistedTerm */
	public static final IRI BLACKLISTED_TERM;

	/** blacklist:label */
	public static final IRI LABEL;

	/** blacklist:lowercasedLabel */
	public static final IRI LOWERCASED_LABEL;

	/** blacklist:concept */
	public static final IRI CONCEPT;

	/** blacklist:facet */
	public static final IRI FACET;

	/** blacklist:template */
	public static final IRI TEMPLATE;

	/** blacklist:templateType */
	public static final IRI TEMPLATE_TYPE;

	/** blacklist:parameterBinding */
	public static final IRI PARAMETER_BINDING;

	/** blacklist:constantBinding */
	public static final IRI CONSTANT_BINDING;

	
	/** blacklist:ntTerm */
	public static final IRI NT_TERM;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		BLACKLISTED_TERM = vf.createIRI(NAMESPACE, "BlacklistedTerm");
		LABEL = vf.createIRI(NAMESPACE, "label");
		LOWERCASED_LABEL = vf.createIRI(NAMESPACE, "lowercasedLabel");
		CONCEPT = vf.createIRI(NAMESPACE, "concept");
		FACET = vf.createIRI(NAMESPACE, "facet");
		TEMPLATE = vf.createIRI(NAMESPACE, "template");
		TEMPLATE_TYPE = vf.createIRI(NAMESPACE, "templateType");
		PARAMETER_BINDING = vf.createIRI(NAMESPACE, "parameterBinding");
		CONSTANT_BINDING = vf.createIRI(NAMESPACE, "constantBinding");
		NT_TERM = vf.createIRI(NAMESPACE, "ntTerm");
	}

}
