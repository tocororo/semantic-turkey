package it.uniroma2.art.semanticturkey.services.core.lime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.vocabulary.ontolexlemon.LIME;

public class LIMEProfiler {
	public Model profileLexicalizationSet(RepositoryConnection repConn, IRI referenceDataset,
			DatasetStatistics datasetStatistics, LexicalizationModel lexicalizationModel) {
		ValueFactory vf = repConn.getValueFactory();

		Map<String, LanguageStatistics> stats = lexicalizationModel.computeStatistics(repConn, new IRI[0]);

		Model model = new LinkedHashModel();

		for (Map.Entry<String, LanguageStatistics> statsForLang : stats.entrySet()) {
			String langCode = statsForLang.getKey();
			LanguageStatistics langStats = statsForLang.getValue();

			IRI lexSetID = vf.createIRI(referenceDataset.stringValue() + langCode + "_"
					+ lexicalizationModel.getName() + "_" + langCode + "_lexicalizationSet");

			model.add(lexSetID, RDF.TYPE, LIME.LEXICALIZATION_SET);
			model.add(lexSetID, LIME.LANGUAGE, vf.createLiteral(langCode));

			model.add(lexSetID, LIME.LEXICALIZATION_MODEL, lexicalizationModel.getIRI());
			model.add(lexSetID, LIME.REFERENCE_DATASET, referenceDataset);

			BigInteger references = langStats.getReferences();
			model.add(lexSetID, LIME.REFERENCES, vf.createLiteral(references));

			Optional<BigInteger> lexicalEntriesHolder = langStats.getLexicalEntries();
			lexicalEntriesHolder.ifPresent(lexicalEntries -> {
				model.add(lexSetID, LIME.LEXICAL_ENTRIES, vf.createLiteral(lexicalEntries));
			});
			BigInteger lexicalizations = langStats.getLexicalizations();
			model.add(lexSetID, LIME.LEXICALIZATIONS, vf.createLiteral(lexicalizations));

			BigDecimal referencesBigDecimal = new BigDecimal(references);
			BigDecimal avgNumOfLexicalizations = new BigDecimal(lexicalizations).divide(referencesBigDecimal);
			model.add(lexSetID, LIME.AVG_NUM_OF_LEXICALIZATIONS, vf.createLiteral(avgNumOfLexicalizations));

			BigDecimal percentage = referencesBigDecimal
					.divide(new BigDecimal(datasetStatistics.getReferences()));
			model.add(lexSetID, LIME.PERCENTAGE, vf.createLiteral(percentage));
		}

		return model;
	}

	public DatasetStatistics profileDataset(RepositoryConnection repConn, IRI referenceDataset,
			SemanticModel semanticModel, Model model) {
		ValueFactory vf = repConn.getValueFactory();

		DatasetStatistics datasetStatistics = semanticModel.computeStatistics(repConn, referenceDataset,
				new IRI[0]);

		model.add(referenceDataset, RDF.TYPE, VOID.DATASET);
		model.add(referenceDataset, DCTERMS.CONFORMS_TO, semanticModel.getIRI());
		model.add(referenceDataset, VOID.TRIPLES, vf.createLiteral(datasetStatistics.getTriples()));

		return datasetStatistics;
	}

}
