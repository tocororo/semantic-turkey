package it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Lexicon Model for Ontologies - Decomposition module (decomp) Vocabulary.
 *
 * @see <a href="https://www.w3.org/2016/05/ontolex/#decomposition-decomp">Lexicon Model for Ontologies -
 *      Decomposition module (decomp)</a>
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DECOMP {

	/**
	 * The DECOMP namespace: http://www.w3.org/ns/lemon/decomp#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/lemon/decomp#";

	/**
	 * Recommended prefix for the Lexicon Model for Ontologies - Decomposition module (decomp) namespace:
	 * "decomp"
	 */
	public static final String PREFIX = "decomp";

	/**
	 * An immutable {@link Namespace} constant that represents the Lexicon Model for Ontologies -
	 * Decomposition module (decomp) namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes

	/** decomp:Component */
	public static final IRI COMPONENT;

	// Properties

	/** decomp:constituent */
	public static final IRI CONSTITUENT;

	/** decomp:correspondsTo */
	public static final IRI CORRESPONDS_TO;

	/** decomp:subterm */
	public static final IRI SUBTERM;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		COMPONENT = vf.createIRI(NAMESPACE, "Component");

		CONSTITUENT = vf.createIRI(NAMESPACE, "constituent");
		CORRESPONDS_TO = vf.createIRI(NAMESPACE, "correspondsTo");
		SUBTERM = vf.createIRI(NAMESPACE, "subterm");
	}
}