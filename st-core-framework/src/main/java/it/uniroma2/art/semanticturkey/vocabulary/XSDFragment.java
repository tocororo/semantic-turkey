package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * {@link org.eclipse.rdf4j.model.vocabulary.XMLSchema} defines only the datatypes.
 * Here there are defined the attributes needed in ST
 */
public abstract class XSDFragment {
	/** The XSD namespace: http://www.w3.org/2001/XMLSchema# */
	public static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

	/**
	 * Recommended prefix for the XSD namespace: "xsd"
	 */
	public static final String PREFIX = "xsd";

	/**
	 * An immutable {@link Namespace} constant that represents the XSD namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	public static final IRI MININCLUSIVE;
	public static final IRI MINEXCLUSIVE;
	public static final IRI MAXINCLUSIVE;
	public static final IRI MAXEXCLUSIVE;
	public static final IRI PATTERN;
	public static final IRI LENGTH;
	public static final IRI MINLENGTH;
	public static final IRI MAXLENGTH;
	public static final IRI LANGRANGE;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		MININCLUSIVE = vf.createIRI(NAMESPACE, "minInclusive");
		MINEXCLUSIVE = vf.createIRI(NAMESPACE, "minExclusive");
		MAXINCLUSIVE = vf.createIRI(NAMESPACE, "maxInclusive");
		MAXEXCLUSIVE = vf.createIRI(NAMESPACE, "maxExclusive");
		PATTERN = vf.createIRI(NAMESPACE, "pattern");
		LENGTH = vf.createIRI(NAMESPACE, "length");
		MINLENGTH = vf.createIRI(NAMESPACE, "minLength");
		MAXLENGTH = vf.createIRI(NAMESPACE, "maxLength");
		LANGRANGE = vf.createIRI(NAMESPACE, "langRange");
	}
}
