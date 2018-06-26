package it.uniroma2.art.semanticturkey.zthes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ExporterContext;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

public class RdfToZthesMapper {
	
	private RepositoryConnection sourceConnection;
	private IRI lexicalizationModel;
	private List<String> langPriorityList = Arrays.asList("en", "fr", "de", "it"); //default
	
	/*
	 * conceptIri - termId: maps the concept IRI with the ID of the created/assigned terms.
	 * This map is useful in presence of multiple prefLabel for the same concept, e.g.:
	 * :c_123 has 3 labels: xl_en_123, xl_de_123, xl_it_123.
	 * I crate the term xl_en_123 (following the pivot en label) and I put in the map the entry
	 * <:c_123, xl_en_123>, so when converting relation toward concept :c_123 I know what termId to use
	 * 
	 */
	private Map<String, String> conceptTermIdMap;
	
	public RdfToZthesMapper(RepositoryConnection connection, ExporterContext exporterContext) {
		this.sourceConnection = connection;
		this.lexicalizationModel = exporterContext.getLexicalizationModel();
		conceptTermIdMap = new HashMap<String, String>();
	}
	
	public void setLanguagePriorityList(List<String> langs) {
		this.langPriorityList = langs;
	}
	
	public Zthes map() {
		Zthes zThes = new Zthes();
		String query = "SELECT ?concept WHERE { ?concept a " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT) + " }";
		TupleQuery tq = sourceConnection.prepareTupleQuery(query);
		TupleQueryResult result = tq.evaluate();
		
		while (result.hasNext()) {
			IRI concept = (IRI)result.next().getValue("concept");
			
			List<Term> preferredTerms = getPreferredTerms(concept);
			
			//identify pivot term, the only one which add hierarchical relations to
			Term ptPivot = getPivotTerm(preferredTerms);
			
			//add the relation LE among all the PTs
			for (Term pt : preferredTerms) {
				for (Term ptTarget : preferredTerms) {
					if (pt != ptTarget) { //except between the same term
						pt.addRelation(new Relation(RelationType.LE, ptTarget.getTermId(), ptTarget.getTermName()));
					}
				}
			}
			
			enrichNotes(concept, ptPivot);
			
			//add other relations and attributes
			GraphQueryResult describeResult = describeResource(concept);
			while (describeResult.hasNext()) {
				Statement stmt = describeResult.next();
				IRI predicate = stmt.getPredicate();
				/*
				 * Each relation between concepts (BT, NT, RT) is created with the localName of the related concept as termId
				 * (instead of the localName of its pivot label) and with an empty termName.
				 * This is necessary since when iterating over the concepts, I still have no all the concepts available
				 * with their info.
				 * All the relations will be fixed later, once all the terms (and so conceptTermIdMap) are collected.
				 */
				if (predicate.equals(SKOS.BROADER)) {
					if (stmt.getSubject().equals(concept)) { //concept as subject -> object is broader
						IRI relatedConcept = (IRI) stmt.getObject();
						ptPivot.addRelation(new Relation(RelationType.BT, relatedConcept.stringValue(), null));
					} else { //concept as object -> subject is narrower
						IRI relatedConcept = (IRI) stmt.getSubject(); 
						ptPivot.addRelation(new Relation(RelationType.NT, relatedConcept.stringValue(), null));
					}
				} else if (predicate.equals(SKOS.NARROWER)) {
					if (stmt.getSubject().equals(concept)) { //concept as subject -> object is narrower
						IRI relatedConcept = (IRI) stmt.getObject();
						ptPivot.addRelation(new Relation(RelationType.NT, relatedConcept.stringValue(), null));
					} else { //concept as object -> subject is broader
						IRI relatedConcept = (IRI) stmt.getSubject();
						ptPivot.addRelation(new Relation(RelationType.BT, relatedConcept.stringValue(), null));
					}
				} else if (predicate.equals(SKOS.RELATED)) {
					IRI relatedConcept = (IRI) stmt.getObject();
					ptPivot.addRelation(new Relation(RelationType.RT, relatedConcept.stringValue(), null));
				} else if (predicate.equals(SKOS.ALT_LABEL)) {
					Literal altLabel = (Literal) stmt.getObject();
					Term nonDescriptor = new Term(concept.getLocalName()+altLabel.getLanguage().get(), altLabel.getLabel());
					nonDescriptor.addRelation(new Relation(RelationType.USE, ptPivot.getTermId(), ptPivot.getTermName()));
					zThes.addTerm(nonDescriptor);
					ptPivot.addRelation(new Relation(RelationType.UF, nonDescriptor.getTermId(), nonDescriptor.getTermName()));
				} else if (predicate.equals(SKOSXL.ALT_LABEL)) {
					IRI altLabel = (IRI) stmt.getObject();
					GraphQueryResult xLabelDescribeResult = describeResource(altLabel);
					while (xLabelDescribeResult.hasNext()) {
						Statement xLabelStmt = xLabelDescribeResult.next();
						if (xLabelStmt.getPredicate().equals(SKOSXL.LITERAL_FORM)) {
							Literal litForm = (Literal) xLabelStmt.getObject();
							Term nonDescriptor = new Term(concept.getLocalName()+litForm.getLanguage().get(), litForm.getLabel());
							nonDescriptor.addRelation(new Relation(RelationType.USE, ptPivot.getTermId(), ptPivot.getTermName()));
							zThes.addTerm(nonDescriptor);
							ptPivot.addRelation(new Relation(RelationType.UF, nonDescriptor.getTermId(), nonDescriptor.getTermName()));
							break;
						}
					}
				}
				//Attributes //TODO attributes assigned to all PT or only to the pivot?
				else if (predicate.equals(DCTERMS.CREATED)) {
					XMLGregorianCalendar xmlDatetime = ((Literal)stmt.getObject()).calendarValue();
					Date date = xmlDatetime.toGregorianCalendar().getTime();
					String formattedDate = new SimpleDateFormat("dd.MM.yyyy").format(date);
//					ptPivot.setTermCreatedDate(formattedDate);
					preferredTerms.forEach(pt -> pt.setTermCreatedDate(formattedDate));
				} else if (predicate.equals(DCTERMS.MODIFIED)) {
					XMLGregorianCalendar xmlDatetime = ((Literal)stmt.getObject()).calendarValue();
					Date date = xmlDatetime.toGregorianCalendar().getTime();
					String formattedDate = new SimpleDateFormat("dd.MM.yyyy").format(date);
//					ptPivot.setTermModifiedDate(formatter.format(date));
					preferredTerms.forEach(pt -> pt.setTermCreatedDate(formattedDate));
				} else if (predicate.equals(OWL2Fragment.DEPRECATED) && stmt.getObject().stringValue().equals("true")) {
//					ptPivot.setTermStatus(TermStatus.deactivated);
					preferredTerms.forEach(pt -> pt.setTermStatus(TermStatus.deactivated));
				}
			}
			
			for (Term pt : preferredTerms) {
				zThes.addTerm(pt);
			}
		}
		
		/* 
		 * fix the relations termId and termName attributes.
		 */
		for (Term term : zThes.getTerms()) {
			for (Relation r : term.getRelations()) {
				String fixedRelationTermId = conceptTermIdMap.get(r.getTermId());
				if (fixedRelationTermId != null) {
					/*
					 * if not null it means that relation term id was a "temp" id taken from the concept
					 * and it needed to be fixed (otherwise it was already taken from label localName)
					 */
					r.setTermId(fixedRelationTermId);
				}
				r.setTermName(zThes.getTermById(r.getTermId()).getTermName());
			}
		}
		return zThes;
	}
	
	private void enrichNotes(IRI concept, Term term) {
		String query = "SELECT ?notePred ?noteValue WHERE {\n"
				+ "?notePred " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.NOTE) + " .\n"
				+ NTriplesUtil.toNTriplesString(concept) + " ?notePred ?note .\n"
				+ "OPTIONAL {\n"
				+ "FILTER(isLiteral(?note))\n"
				+ NTriplesUtil.toNTriplesString(concept) + " ?notePred ?note .\n"
				+ "BIND(?note as ?noteValue)\n"
				+ "}\n"
				+ "OPTIONAL { ?note " + NTriplesUtil.toNTriplesString(RDF.VALUE) + " ?noteValue }\n"
				+ "}";
		TupleQueryResult results = sourceConnection.prepareTupleQuery(query).evaluate();
		while (results.hasNext()) {
			BindingSet bs = results.next();
			IRI pred = (IRI) bs.getBinding("notePred").getValue();
			String note = bs.getBinding("noteValue").getValue().stringValue();
			TermNote termNote = new TermNote(note);
			termNote.setLabel(pred.getLocalName());
			term.addTermNote(termNote);
		}
	}
	
	private GraphQueryResult describeResource(IRI resource) {
		GraphQuery gq = sourceConnection.prepareGraphQuery("DESCRIBE " + NTriplesUtil.toNTriplesString(resource));
		return gq.evaluate();
	}
	
	private List<Term> getPreferredTerms(IRI concept) {
		List<Term> preferredTerms = new ArrayList<>();
		//get all prefLabels
		if (lexicalizationModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			String query = "SELECT DISTINCT ?label WHERE { "
				+ NTriplesUtil.toNTriplesString(concept) + " " + NTriplesUtil.toNTriplesString(SKOS.PREF_LABEL) + " ?label . }";
			TupleQueryResult prefLabelResult = sourceConnection.prepareTupleQuery(query).evaluate();
			while (prefLabelResult.hasNext()) {
				BindingSet bs = prefLabelResult.next();
				Literal prefLabel = (Literal) bs.getValue("label");
				String label = prefLabel.getLabel();
				String lang = prefLabel.getLanguage().get();
				Term term = new Term(concept.getLocalName(), label);
				term.setTermType(TermType.PT);
				term.setTermLanguage(lang);
				preferredTerms.add(term);
			}
		} else if (lexicalizationModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			String query = "SELECT DISTINCT ?label ?lit WHERE { "
				+ NTriplesUtil.toNTriplesString(concept) + " " + NTriplesUtil.toNTriplesString(SKOSXL.PREF_LABEL) + " ?label . "
				+ "?label "+ NTriplesUtil.toNTriplesString(SKOSXL.LITERAL_FORM) + " ?lit . }";
			TupleQueryResult prefLabelResult = sourceConnection.prepareTupleQuery(query).evaluate();
			while (prefLabelResult.hasNext()) {
				BindingSet bs = prefLabelResult.next();
				IRI prefLabel = (IRI) bs.getValue("label");
				Literal literalForm = (Literal) bs.getValue("lit");
				String label = literalForm.getLabel();
				String lang = literalForm.getLanguage().get();
				Term term = new Term(prefLabel.getLocalName(), label);
				term.setTermType(TermType.PT);
				term.setTermLanguage(lang);
				preferredTerms.add(term);
			}
		}
		if (preferredTerms.isEmpty()) { //if the given concept has no labels, add a single term with only termId
			preferredTerms.add(new Term(concept.getLocalName(), null));
		}
		conceptTermIdMap.put(concept.stringValue(), getPivotTerm(preferredTerms).getTermId());
		
		return preferredTerms;
	}
	
	private Term getPivotTerm(List<Term> preferredTerms) {
		Term ptPivot = null;
		for (Term pt : preferredTerms) {
			if (ptPivot == null) { //no pivot term set yet.
				ptPivot = pt;
			} else {
				int pivotLangPriority = langPriorityList.indexOf(ptPivot.getTermLanguage());
				int termLangPriority = langPriorityList.indexOf(pt.getTermLanguage());
				/*
				 * 2 scenarios:
				 * Both term, current pivot (ptPivot) and checked term (pt) have lang in the langPriorityList
				 * 	=> perform the comparison
				 * Only one term has the lang in langPriorityList
				 *  => the term with lang in the langPriorityList in the new pivot term
				 *  (just check the case where only the checked term has lang in langPriorityList, since in the opposite
				 *  case the pivot term doesn't change)
				 */
				if (pivotLangPriority != -1 && termLangPriority != -1) {
					if (termLangPriority < pivotLangPriority) {
						ptPivot = pt;
					}
				} else if (pivotLangPriority == -1 && termLangPriority != -1) {
					ptPivot = pt;
				}
			}
		}
		return ptPivot;
	}
	
}
