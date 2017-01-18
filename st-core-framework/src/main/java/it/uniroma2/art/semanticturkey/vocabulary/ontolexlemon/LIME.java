package it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Lexicon Model for Ontologies - Linguistic metadata module (lime) Vocabulary.
 *
 * @see <a href="https://www.w3.org/2016/05/ontolex/#metadata-lime">Lexicon Model for Ontologies - Linguistic
 *      metadata module (lime)</a>
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LIME {

	/**
	 * The LIME namespace: http://www.w3.org/ns/lemon/lime#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/lemon/lime#";

	/**
	 * Recommended prefix for the Lexicon Model for Ontologies - Linguistic metadata module (lime) namespace:
	 * "lime"
	 */
	public static final String PREFIX = "lime";

	/**
	 * An immutable {@link Namespace} constant that represents the Lexicon Model for Ontologies - Linguistic
	 * metadata module (lime) namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes

	/** lime:ConceptualizationSet */
	public static final IRI CONCEPTUALIZATION_SET;

	/** lime:LexicalLinkset */
	public static final IRI LEXICAL_LINKSET;

	/** lime:LexicalizationSet */
	public static final IRI LEXICALIZATION_SET;

	/** lime:Lexicon */
	public static final IRI LEXICON;

	// Properties

	/** lime:avgAmbiguity */
	public static final IRI AVG_AMBIGUITY;

	/** lime:avgNumOfLexicalizations */
	public static final IRI AVG_NUM_OF_LEXICALIZATIONS;

	/** lime:avgNumOfLinks */
	public static final IRI AVG_NUM_OF_LINKS;

	/** lime:avgSynonymy */
	public static final IRI AVG_SYNONYMY;

	/** lime:concepts */
	public static final IRI CONCEPTS;

	/** lime:conceptualDataset */
	public static final IRI CONCEPTUAL_DATASET;

	/** lime:conceptualizations */
	public static final IRI CONCEPTUALIZATIONS;

	/** lime:entry */
	public static final IRI ENTRY;

	/** lime:language */
	public static final IRI LANGUAGE;

	/** lime:lexicalEntries */
	public static final IRI LEXICAL_ENTRIES;

	/** lime:lexicalizationModel */
	public static final IRI LEXICALIZATION_MODEL;

	/** lime:lexicalizations */
	public static final IRI LEXICALIZATIONS;

	/** lime:lexiconDataset */
	public static final IRI LEXICON_DATASET;

	/** lime:linguisticCatalog */
	public static final IRI LINGUISTIC_CATALOG;

	/** lime:links */
	public static final IRI LINKS;

	/** lime:partition */
	public static final IRI PARTITION;

	/** lime:percentage */
	public static final IRI PERCENTAGE;

	/** lime:referenceDataset */
	public static final IRI REFERENCE_DATASET;

	/** lime:references */
	public static final IRI REFERENCES;

	/** lime:resourceType */
	public static final IRI RESOURCE_TYPE;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		CONCEPTUALIZATION_SET = vf.createIRI(NAMESPACE, "ConceptualizationSet");
		LEXICAL_LINKSET = vf.createIRI(NAMESPACE, "LexicalLinkset");
		LEXICALIZATION_SET = vf.createIRI(NAMESPACE, "LexicalizationSet");
		LEXICON = vf.createIRI(NAMESPACE, "Lexicon");

		AVG_AMBIGUITY = vf.createIRI(NAMESPACE, "avgAmbiguity");
		AVG_NUM_OF_LEXICALIZATIONS = vf.createIRI(NAMESPACE, "avgNumOfLexicalizations");
		AVG_NUM_OF_LINKS = vf.createIRI(NAMESPACE, "avgNumOfLinks");
		AVG_SYNONYMY = vf.createIRI(NAMESPACE, "avgSynonymy");
		CONCEPTS = vf.createIRI(NAMESPACE, "concepts");
		CONCEPTUAL_DATASET = vf.createIRI(NAMESPACE, "conceptualDataset");
		CONCEPTUALIZATIONS = vf.createIRI(NAMESPACE, "conceptualizations");
		ENTRY = vf.createIRI(NAMESPACE, "entry");
		LANGUAGE = vf.createIRI(NAMESPACE, "language");
		LEXICAL_ENTRIES = vf.createIRI(NAMESPACE, "lexicalEntries");
		LEXICALIZATION_MODEL = vf.createIRI(NAMESPACE, "lexicalizationModel");
		LEXICALIZATIONS = vf.createIRI(NAMESPACE, "lexicalizations");
		LEXICON_DATASET = vf.createIRI(NAMESPACE, "lexiconDataset");
		LINGUISTIC_CATALOG = vf.createIRI(NAMESPACE, "linguisticCatalog");
		LINKS = vf.createIRI(NAMESPACE, "links");
		PARTITION = vf.createIRI(NAMESPACE, "partition");
		PERCENTAGE = vf.createIRI(NAMESPACE, "percentage");
		REFERENCE_DATASET = vf.createIRI(NAMESPACE, "referenceDataset");
		REFERENCES = vf.createIRI(NAMESPACE, "references");
		RESOURCE_TYPE = vf.createIRI(NAMESPACE, "resourceType");
	}
}