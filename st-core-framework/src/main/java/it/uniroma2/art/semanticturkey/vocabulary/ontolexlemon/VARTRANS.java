package it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Lexicon Model for Ontologies - Variation and translation module (vartrans) Vocabulary.
 *
 * @see <a href="https://www.w3.org/2016/05/ontolex/#variation-translation-vartrans">Lexicon Model for
 *      Ontologies - Variation and translation module (vartrans)</a>
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class VARTRANS {

	/**
	 * The VARTRANS namespace: http://www.w3.org/ns/lemon/vartrans#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/lemon/vartrans#";

	/**
	 * Recommended prefix for the Lexicon Model for Ontologies - Variation and translation module (vartrans)
	 * namespace: "vartrans"
	 */
	public static final String PREFIX = "vartrans";

	/**
	 * An immutable {@link Namespace} constant that represents the Lexicon Model for Ontologies - Variation
	 * and translation module (vartrans) namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes

	/** vartrans:ConceptualRelation */
	public static final IRI CONCEPTUAL_RELATION;

	/** vartrans:LexicalRelation */
	public static final IRI LEXICAL_RELATION;

	/** vartrans:LexicoSemanticRelation */
	public static final IRI LEXICO_SEMANTIC_RELATION;

	/** vartrans:SenseRelation */
	public static final IRI SENSE_RELATION;

	/** vartrans:TerminologicalRelation */
	public static final IRI TERMINOLOGICAL_RELATION;

	/** vartrans:Translation */
	public static final IRI TRANSLATION;

	/** vartrans:TranslationSet */
	public static final IRI TRANSLATION_SET;

	// Properties

	/** vartrans:category */
	public static final IRI CATEGORY;

	/** vartrans:conceptRel */
	public static final IRI CONCEPT_REL;

	/** vartrans:lexicalRel */
	public static final IRI LEXICAL_REL;

	/** vartrans:relates */
	public static final IRI RELATES;

	/** vartrans:senseRel */
	public static final IRI SENSE_REL;

	/** vartrans:source */
	public static final IRI SOURCE;

	/** vartrans:target */
	public static final IRI TARGET;

	/** vartrans:trans */
	public static final IRI TRANS;

	/** vartrans:translatableAs */
	public static final IRI TRANSLATABLE_AS;

	/*
	 * FIXME: the constant HAS_TRANSLATION does not match the local name "translation" because of a collision
	 * with identifier for the class vartrans:Translation
	 */
	/** vartrans:translation */
	public static final IRI HAS_TRANSLATION;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		CONCEPTUAL_RELATION = vf.createIRI(NAMESPACE, "ConceptualRelation");
		LEXICAL_RELATION = vf.createIRI(NAMESPACE, "LexicalRelation");
		LEXICO_SEMANTIC_RELATION = vf.createIRI(NAMESPACE, "LexicoSemanticRelation");
		SENSE_RELATION = vf.createIRI(NAMESPACE, "SenseRelation");
		TERMINOLOGICAL_RELATION = vf.createIRI(NAMESPACE, "TerminologicalRelation");
		TRANSLATION = vf.createIRI(NAMESPACE, "Translation");
		TRANSLATION_SET = vf.createIRI(NAMESPACE, "TranslationSet");

		CATEGORY = vf.createIRI(NAMESPACE, "category");
		CONCEPT_REL = vf.createIRI(NAMESPACE, "conceptRel");
		LEXICAL_REL = vf.createIRI(NAMESPACE, "lexicalRel");
		RELATES = vf.createIRI(NAMESPACE, "relates");
		SENSE_REL = vf.createIRI(NAMESPACE, "senseRel");
		SOURCE = vf.createIRI(NAMESPACE, "source");
		TARGET = vf.createIRI(NAMESPACE, "target");
		TRANS = vf.createIRI(NAMESPACE, "trans");
		TRANSLATABLE_AS = vf.createIRI(NAMESPACE, "translatableAs");
		HAS_TRANSLATION = vf.createIRI(NAMESPACE, "translation");
	}
}