package it.uniroma2.art.semanticturkey.resources;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.Optional2StringConverter;

/**
 * Metadata describing a lexicalization set.
 * 
 */
public class LexicalizationSetMetadata {

	private final IRI identity;
	private final IRI referenceDataset;
	private final Optional<IRI> lexiconDataset;
	private final IRI lexicalizationModel;
	private final String language;
	private final Optional<BigInteger> references;
	private final Optional<BigInteger> lexicalEntries;
	private final Optional<BigInteger> lexicalizations;
	private final Optional<BigDecimal> percentage;
	private final Optional<BigDecimal> avgNumOfLexicalizations;

	public LexicalizationSetMetadata(IRI identity, IRI referenceDataset, @Nullable IRI lexiconDataset,
			IRI lexicalizationModel, String language, @Nullable BigInteger references,
			@Nullable BigInteger lexicalEntries, @Nullable BigInteger lexicalizations,
			@Nullable BigDecimal percentage, @Nullable BigDecimal avgNumOfLexicalizations) {
		this.identity = identity;
		this.referenceDataset = referenceDataset;
		this.lexiconDataset = Optional.ofNullable(lexiconDataset);
		this.lexicalizationModel = lexicalizationModel;
		this.language = language;
		this.references = Optional.ofNullable(references);
		this.lexicalEntries = Optional.ofNullable(lexicalEntries);
		this.lexicalizations = Optional.ofNullable(lexicalizations);
		this.percentage = Optional.ofNullable(percentage);
		this.avgNumOfLexicalizations = Optional.ofNullable(avgNumOfLexicalizations);
	}

	@JsonSerialize(converter = IRI2StringConverter.class)
	public IRI getIdentity() {
		return identity;
	}

	@JsonSerialize(converter = IRI2StringConverter.class)
	public IRI getReferenceDataset() {
		return referenceDataset;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<IRI> getLexiconDataset() {
		return lexiconDataset;
	}

	@JsonSerialize(converter = IRI2StringConverter.class)
	public IRI getLexicalizationModel() {
		return lexicalizationModel;
	}

	public String getLanguage() {
		return language;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<BigInteger> getReferences() {
		return references;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<BigInteger> getLexicalEntries() {
		return lexicalEntries;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<BigInteger> getLexicalizations() {
		return lexicalizations;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<BigDecimal> getPercentage() {
		return percentage;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<BigDecimal> getAvgNumOfLexicalizations() {
		return avgNumOfLexicalizations;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("referenceDataset", referenceDataset)
				.add("lexiconDataset", lexiconDataset).add("lexicalizationModel", lexicalizationModel)
				.add("language", language).add("references", references).add("lexicalEntries", lexicalEntries)
				.add("lexicalizations", lexicalizations).add("percentage", percentage)
				.add("avgNumOfLexicalizations", avgNumOfLexicalizations).toString();
	}

}
