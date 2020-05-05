package it.uniroma2.art.semanticturkey.zthes;

public enum RelationType {
	NT(TermType.PT, TermType.PT),
	BT(TermType.PT, TermType.PT),
	RT(TermType.PT, TermType.PT),
	LE(TermType.PT, TermType.PT),
	USE(TermType.ND, TermType.PT),
	UF(TermType.PT, TermType.ND);
	
	private TermType termTypeFrom;
	private TermType termTypeTo;
	
	private RelationType(TermType termTypeFrom, TermType termTypeTo) {
		this.termTypeFrom = termTypeFrom;
		this.termTypeTo = termTypeTo;
	}
	
	public TermType getTermTypeFrom() {
		return this.termTypeFrom;
	}
	
	public TermType getTermTypeTo() {
		return this.termTypeTo;
	}
}
