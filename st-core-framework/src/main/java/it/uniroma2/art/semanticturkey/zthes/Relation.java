package it.uniroma2.art.semanticturkey.zthes;

public class Relation extends TermEntity {
	
	public static class Tag {
		public static final String RELATION_TYPE = "relationType";
		public static final String SOURCE_DB = "sourceDb";
		public static final String TERM_ID = "termId";
		public static final String TERM_NAME = "termName";
		public static final String TERM_QUALIFIER = "termQualifier";
		public static final String TERM_TYPE = "termType";
		public static final String TERM_LANGUAGE = "termLanguage";
	}
	public static class Attr {
		public static final String WEIGHT = "weight";
	}

	private RelationType relationType;
	private String sourceDb;
	private float weight; //optional
	
	public Relation(RelationType relationType, String termId, String termName) {
		super(termId, termName);
		this.relationType = relationType;
	}

	public RelationType getRelationType() {
		return relationType;
	}

	public void setRelationType(RelationType relationType) {
		this.relationType = relationType;
	}

	public String getSourceDb() {
		return sourceDb;
	}

	public void setSourceDb(String sourceDb) {
		this.sourceDb = sourceDb;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	@Override
	public String toString() {
		return Tag.RELATION_TYPE + " " + this.relationType + "\n" +
				Tag.SOURCE_DB + " " + this.sourceDb + "\n" +
				Tag.TERM_ID + " " + this.getTermId() + "\n" +
				Tag.TERM_NAME + " " + this.getTermName() + "\n" +
				Tag.TERM_QUALIFIER + " " + this.getTermQualifier() + "\n" +
				Tag.TERM_TYPE + " " + this.getTermType() + "\n" +
				Tag.TERM_LANGUAGE + " " + this.getTermLanguage() + "\n" +
				Attr.WEIGHT + " " + this.weight;
	}
	
	
	
}
