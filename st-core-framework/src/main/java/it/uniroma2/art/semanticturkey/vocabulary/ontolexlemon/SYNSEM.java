package it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Lexicon Model for Ontologies - Syntax and semantics module (synsem) Vocabulary.
 *
 * @see <a href="https://www.w3.org/2016/05/ontolex/#syntax-and-semantics-synsem">Lexicon Model for Ontologies
 *      - Syntax and semantics module (synsem)</a>
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SYNSEM {

	/**
	 * The SYNSEM namespace: http://www.w3.org/ns/lemon/synsem#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/lemon/synsem#";

	/**
	 * Recommended prefix for the Lexicon Model for Ontologies - Syntax and semantics module (synsem)
	 * namespace: "synsem"
	 */
	public static final String PREFIX = "synsem";

	/**
	 * An immutable {@link Namespace} constant that represents the Lexicon Model for Ontologies - Syntax and
	 * semantics module (synsem) namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes

	/** synsem:OntoMap */
	public static final IRI ONTO_MAP;

	/** synsem:SyntacticArgument */
	public static final IRI SYNTACTIC_ARGUMENT;

	/** synsem:SyntacticFrame */
	public static final IRI SYNTACTIC_FRAME;

	// Properties

	/** synsem:condition */
	public static final IRI CONDITION;

	/** synsem:isA */
	public static final IRI IS_A;

	/** synsem:marker */
	public static final IRI MARKER;

	/** synsem:objOfProp */
	public static final IRI OBJ_OF_PROP;

	/** synsem:ontoCorrespondence */
	public static final IRI ONTO_CORRESPONDENCE;

	/** synsem:ontoMapping */
	public static final IRI ONTO_MAPPING;

	/** synsem:optional */
	public static final IRI OPTIONAL;

	/** synsem:propertyDomain */
	public static final IRI PROPERTY_DOMAIN;

	/** synsem:propertyRange */
	public static final IRI PROPERTY_RANGE;

	/** synsem:subjOfProp */
	public static final IRI SUBJ_OF_PROP;

	/** synsem:submap */
	public static final IRI SUBMAP;

	/** synsem:synArg */
	public static final IRI SYN_ARG;

	/** synsem:synBehavior */
	public static final IRI SYN_BEHAVIOR;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		ONTO_MAP = vf.createIRI(NAMESPACE, "OntoMap");
		SYNTACTIC_ARGUMENT = vf.createIRI(NAMESPACE, "SyntacticArgument");
		SYNTACTIC_FRAME = vf.createIRI(NAMESPACE, "SyntacticFrame");

		CONDITION = vf.createIRI(NAMESPACE, "condition");
		IS_A = vf.createIRI(NAMESPACE, "isA");
		MARKER = vf.createIRI(NAMESPACE, "marker");
		OBJ_OF_PROP = vf.createIRI(NAMESPACE, "objOfProp");
		ONTO_CORRESPONDENCE = vf.createIRI(NAMESPACE, "ontoCorrespondence");
		ONTO_MAPPING = vf.createIRI(NAMESPACE, "ontoMapping");
		OPTIONAL = vf.createIRI(NAMESPACE, "optional");
		PROPERTY_DOMAIN = vf.createIRI(NAMESPACE, "propertyDomain");
		PROPERTY_RANGE = vf.createIRI(NAMESPACE, "propertyRange");
		SUBJ_OF_PROP = vf.createIRI(NAMESPACE, "subjOfProp");
		SUBMAP = vf.createIRI(NAMESPACE, "submap");
		SYN_ARG = vf.createIRI(NAMESPACE, "synArg");
		SYN_BEHAVIOR = vf.createIRI(NAMESPACE, "synBehavior");
	}
}