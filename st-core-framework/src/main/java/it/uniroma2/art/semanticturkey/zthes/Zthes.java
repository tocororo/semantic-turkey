package it.uniroma2.art.semanticturkey.zthes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Zthes {
	
	public static class Tag {
		public static final String ZTHES = "Zthes";
	}
	
	private Map<String, Term> termMap; //termId - term
	private Multimap<RelationType, Term> relationMap; //relationType - [termWithThatRelation]
	
	public Zthes() {
		termMap = new HashMap<String, Term>();
		relationMap = HashMultimap.create();
	}
	
	public Collection<Term> getTerms() {
		return termMap.values();
	}
	
	public Term getTermById(String termId) {
		return termMap.get(termId);
	}
	
	public void addTerm(Term term) {
		termMap.put(term.getTermId(), term);
		//Add the termId-relatedTermId to the map
		for (Relation relation : term.getRelations()) {//for each relation of the term
			RelationType relationType = relation.getRelationType();
			relationMap.put(relationType, term);
		}
	}
	
	public Collection<Term> getTermsWithRelation(RelationType relationType) {
		return relationMap.get(relationType);
	}
	
}
