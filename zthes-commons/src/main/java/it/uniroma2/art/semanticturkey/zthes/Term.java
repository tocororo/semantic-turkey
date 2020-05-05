package it.uniroma2.art.semanticturkey.zthes;

import java.util.ArrayList;
import java.util.List;

public class Term extends TermEntity {
	
	public static class Tag {
		public static final String TERM = "term";
		public static final String TERM_ID = "termId";
		public static final String TERM_NAME = "termName";
		public static final String TERM_QUALIFIER = "termQualifier";
		public static final String TERM_TYPE = "termType";
		public static final String TERM_LANGUAGE = "termLanguage";
		public static final String TERM_STATUS = "termStatus";
		public static final String TERM_NOTE = "termNote";
		public static final String TERM_CREATED_DATE = "termCreatedDate";
		public static final String TERM_CREATED_BY = "termCreatedBy";
		public static final String TERM_MODIFIED_DATE = "termModifiedDate";
		public static final String TERM_MODIFIED_BY = "termModifiedBy";
		public static final String RELATION = "relation";
	}

	private TermStatus termStatus;
	private List<TermNote> termNotes;
	private String termCreatedDate;
	private String termCreatedBy;
	private String termModifiedDate;
	private String termModifiedBy;
	private List<Relation> relations;
	
	public Term(String termId, String termName) {
		super(termId, termName);
		this.termNotes = new ArrayList<TermNote>();
		this.relations = new ArrayList<Relation>();
	}

	public TermStatus getTermStatus() {
		return termStatus;
	}

	public void setTermStatus(TermStatus termStatus) {
		this.termStatus = termStatus;
	}

	public List<TermNote> getTermNotes() {
		return termNotes;
	}

	public void setTermNotes(List<TermNote> termNotes) {
		this.termNotes = termNotes;
	}
	
	public void addTermNote(TermNote termNote) {
		this.termNotes.add(termNote);
	}

	public String getTermCreatedDate() {
		return termCreatedDate;
	}

	public void setTermCreatedDate(String termCreatedDate) {
		this.termCreatedDate = termCreatedDate;
	}

	public String getTermCreatedBy() {
		return termCreatedBy;
	}

	public void setTermCreatedBy(String termCreatedBy) {
		this.termCreatedBy = termCreatedBy;
	}

	public String getTermModifiedDate() {
		return termModifiedDate;
	}

	public void setTermModifiedDate(String termModifiedDate) {
		this.termModifiedDate = termModifiedDate;
	}

	public String getTermModifiedBy() {
		return termModifiedBy;
	}

	public void setTermModifiedBy(String termModifiedBy) {
		this.termModifiedBy = termModifiedBy;
	}

	public List<Relation> getRelations() {
		return relations;
	}
	
	public List<Relation> getRelations(RelationType type) {
		List<Relation> rels = new ArrayList<>();
		for (Relation r : this.relations) {
			if (r.getRelationType() == type) {
				rels.add(r);
			}
		}
		return rels;
	}

	public void setRelation(List<Relation> relations) {
		this.relations = relations;
	}
	
	public void addRelation(Relation relation) {
		this.relations.add(relation);
	}
	
	@Override
	public String toString() {
		return Tag.TERM_ID + " " + this.getTermId() + "\n" +
				Tag.TERM_NAME + " " + this.getTermName() + "\n" +
				Tag.TERM_QUALIFIER + " " + this.getTermQualifier() + "\n" +
				Tag.TERM_TYPE + " " + this.getTermType() + "\n" +
				Tag.TERM_LANGUAGE + " " + this.getTermLanguage() + "\n" +
				Tag.TERM_STATUS + " " + this.termStatus + "\n" +
				Tag.TERM_NOTE + " " + this.termNotes + "\n" +
				Tag.TERM_CREATED_DATE + " " + this.termCreatedDate + "\n" +
				Tag.TERM_CREATED_BY + " " + this.termCreatedBy + "\n" +
				Tag.TERM_MODIFIED_DATE + " " + this.termModifiedDate + "\n" +
				Tag.TERM_MODIFIED_BY + " " + this.termModifiedBy + "\n" +
				Tag.RELATION + " " + this.relations;
	}
	
}
