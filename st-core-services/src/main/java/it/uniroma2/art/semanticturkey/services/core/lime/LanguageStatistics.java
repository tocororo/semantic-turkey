package it.uniroma2.art.semanticturkey.services.core.lime;

import java.math.BigInteger;
import java.util.Optional;

public class LanguageStatistics {
	private BigInteger lexicalizations;
	private BigInteger references;
	private Optional<BigInteger> lexicalEntries = Optional.empty();

	public BigInteger getLexicalizations() {
		return lexicalizations;
	}

	public void setLexicalizations(BigInteger bigInteger) {
		this.lexicalizations = bigInteger;
	}

	public BigInteger getReferences() {
		return references;
	}

	public void setReferences(BigInteger references) {
		this.references = references;
	}
	public Optional<BigInteger> getLexicalEntries() {
		return lexicalEntries;
	}
	
	public void setLexicalEntries(Optional<BigInteger> lexicalEntries) {
		this.lexicalEntries = lexicalEntries;
	}
}
