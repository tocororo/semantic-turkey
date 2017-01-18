package it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Lexicon Model for Ontologies - Core Vocabulary.
 *
 * @see <a href="https://www.w3.org/2016/05/ontolex/#core">Lexicon Model for Ontologies - Core</a>
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ONTOLEX {

	/**
	 * The ONTOLEX namespace: http://www.w3.org/ns/lemon/ontolex#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/lemon/ontolex#";

	/**
	 * Recommended prefix for the Lexicon Model for Ontologies - Core namespace: "ontolex"
	 */
	public static final String PREFIX = "ontolex";

	/**
	 * An immutable {@link Namespace} constant that represents the Lexicon Model for Ontologies - Core
	 * namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes

	/** ontolex:Affix */
	public static final IRI AFFIX;

	/** ontolex:ConceptSet */
	public static final IRI CONCEPT_SET;

	/** ontolex:Form */
	public static final IRI FORM;

	/** ontolex:LexicalConcept */
	public static final IRI LEXICAL_CONCEPT;

	/** ontolex:LexicalEntry */
	public static final IRI LEXICAL_ENTRY;

	/** ontolex:LexicalSense */
	public static final IRI LEXICAL_SENSE;

	/** ontolex:MultiWordExpression */
	public static final IRI MULTI_WORD_EXPRESSION;

	/** ontolex:Word */
	public static final IRI WORD;

	// Properties

	/** ontolex:canonicalForm */
	public static final IRI CANONICAL_FORM;

	/** ontolex:concept */
	public static final IRI CONCEPT;

	/** ontolex:denotes */
	public static final IRI DENOTES;

	/** ontolex:evokes */
	public static final IRI EVOKES;

	/** ontolex:isConceptOf */
	public static final IRI IS_CONCEPT_OF;

	/** ontolex:isDenotedBy */
	public static final IRI IS_DENOTED_BY;

	/** ontolex:isEvokedBy */
	public static final IRI IS_EVOKED_BY;

	/** ontolex:isLexicalizedSenseOf */
	public static final IRI IS_LEXICALIZED_SENSE_OF;

	/** ontolex:isReferenceOf */
	public static final IRI IS_REFERENCE_OF;

	/** ontolex:isSenseOf */
	public static final IRI IS_SENSE_OF;

	/** ontolex:lexicalForm */
	public static final IRI LEXICAL_FORM;

	/** ontolex:lexicalizedSense */
	public static final IRI LEXICALIZED_SENSE;

	/** ontolex:morphologicalPattern */
	public static final IRI MORPHOLOGICAL_PATTERN;

	/** ontolex:otherForm */
	public static final IRI OTHER_FORM;

	/** ontolex:phoneticRep */
	public static final IRI PHONETIC_REP;

	/** ontolex:reference */
	public static final IRI REFERENCE;

	/** ontolex:representation */
	public static final IRI REPRESENTATION;

	/** ontolex:sense */
	public static final IRI SENSE;

	/** ontolex:usage */
	public static final IRI USAGE;

	/** ontolex:writtenRep */
	public static final IRI WRITTEN_REP;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		AFFIX = vf.createIRI(NAMESPACE, "Affix");
		CONCEPT_SET = vf.createIRI(NAMESPACE, "ConceptSet");
		FORM = vf.createIRI(NAMESPACE, "Form");
		LEXICAL_CONCEPT = vf.createIRI(NAMESPACE, "LexicalConcept");
		LEXICAL_ENTRY = vf.createIRI(NAMESPACE, "LexicalEntry");
		LEXICAL_SENSE = vf.createIRI(NAMESPACE, "LexicalSense");
		MULTI_WORD_EXPRESSION = vf.createIRI(NAMESPACE, "MultiWordExpression");
		WORD = vf.createIRI(NAMESPACE, "Word");

		CANONICAL_FORM = vf.createIRI(NAMESPACE, "canonicalForm");
		CONCEPT = vf.createIRI(NAMESPACE, "concept");
		DENOTES = vf.createIRI(NAMESPACE, "denotes");
		EVOKES = vf.createIRI(NAMESPACE, "evokes");
		IS_CONCEPT_OF = vf.createIRI(NAMESPACE, "isConceptOf");
		IS_DENOTED_BY = vf.createIRI(NAMESPACE, "isDenotedBy");
		IS_EVOKED_BY = vf.createIRI(NAMESPACE, "isEvokedBy");
		IS_LEXICALIZED_SENSE_OF = vf.createIRI(NAMESPACE, "isLexicalizedSenseOf");
		IS_REFERENCE_OF = vf.createIRI(NAMESPACE, "isReferenceOf");
		IS_SENSE_OF = vf.createIRI(NAMESPACE, "isSenseOf");
		LEXICAL_FORM = vf.createIRI(NAMESPACE, "lexicalForm");
		LEXICALIZED_SENSE = vf.createIRI(NAMESPACE, "lexicalizedSense");
		MORPHOLOGICAL_PATTERN = vf.createIRI(NAMESPACE, "morphologicalPattern");
		OTHER_FORM = vf.createIRI(NAMESPACE, "otherForm");
		PHONETIC_REP = vf.createIRI(NAMESPACE, "phoneticRep");
		REFERENCE = vf.createIRI(NAMESPACE, "reference");
		REPRESENTATION = vf.createIRI(NAMESPACE, "representation");
		SENSE = vf.createIRI(NAMESPACE, "sense");
		USAGE = vf.createIRI(NAMESPACE, "usage");
		WRITTEN_REP = vf.createIRI(NAMESPACE, "writtenRep");
	}
}