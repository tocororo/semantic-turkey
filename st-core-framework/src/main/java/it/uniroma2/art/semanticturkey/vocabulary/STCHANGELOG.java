package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for Semantic Turkey-specific changelog metadata.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class STCHANGELOG {
	/** http://semanticturkey.uniroma2.it/ns/st-changelog# */
	public static final String NAMESPACE = "http://semanticturkey.uniroma2.it/ns/st-changelog#";

	/**
	 * Recommended prefix for the STCHANGELOG namespace: "stcl"
	 */
	public static final String PREFIX = "stcl";

	/**
	 * An immutable {@link Namespace} constant that represents the STCHANGELOG namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	public static final IRI PERFORMER;
	public static final IRI VALIDATOR;

	public static final Value PARAMETERS;

	static {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		PERFORMER = vf.createIRI(NAMESPACE, "performer");
		VALIDATOR = vf.createIRI(NAMESPACE, "validator");
		PARAMETERS = vf.createIRI(NAMESPACE, "parameters");
	}
}
