package it.uniroma2.art.semanticturkey.zthes;

public abstract class TermEntity {
	
	private String termId;
	private String termName;
	private String termQualifier;

	private TermType termType;
	private String termLanguage;
	
	public TermEntity(String termId, String termName) {
		this.termId = termId;
		this.termName = termName;
	}
	
	/**
	 * Returns a 6-digits id omitting leading 1 or 9 when the id is 7-digits
	 * @return
	 */
	public String getTermId() {
		return termId;
	}
	
	public void setTermId(String termId) {
		this.termId = termId;
	}

	public String getTermName() {
		return termName;
	}

	public void setTermName(String termName) {
		this.termName = termName;
	}
	
	//TODO the following are foreseen by both term and relation, but I would prefer to avoid them
	
	public String getTermQualifier() {
		return termQualifier;
	}

	public void setTermQualifier(String termQualifier) {
		this.termQualifier = termQualifier;
	}

	public TermType getTermType() {
		return termType;
	}

	public void setTermType(TermType termType) {
		this.termType = termType;
	}

	public String getTermLanguage() {
		return termLanguage;
	}

	public void setTermLanguage(String termLanguage) {
		this.termLanguage = termLanguage;
	}

}