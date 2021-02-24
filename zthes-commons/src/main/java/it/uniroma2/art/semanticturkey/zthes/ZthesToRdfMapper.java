package it.uniroma2.art.semanticturkey.zthes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LifterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;

public class ZthesToRdfMapper {
	
	private static Logger logger = LoggerFactory.getLogger(ZthesToRdfMapper.class);
	
	LifterContext lifterContext;
	SimpleValueFactory vf;
	
	private Zthes zThes;
	
	private Model model;
	
	/*
	 * termId - concept: maps the ID of the terms with the created/assigned concept.
	 * This map is useful in presence of LE relations, e.g.:
	 * Term 123 is in relation LE with 456 and 789. From term 123 I create the concept :c_123
	 * and I put in the map this three entries: <123,:c_123>, <456,:c_123>, <789,:c_123>, so when
	 * converting terms 456 and 789, I already know which concept to use
	 * 
	 */
	private Map<String, IRI> idConceptMap;
	
	/*
	 * termId - xLabel: maps the ID of the terms with the created/assigned xLabel (in case of SKOSXL lexModel).
	 * This map is useful when the same term is referenced multiple time and the created/assigned concept
	 * is get through createConcept() method. Since this method create also the xLabel of the concept
	 * is necessary to cache and retrieve it in case it has already been created (and so avoid to create multiple
	 * xLabel for the same concept)
	 */
	private Map<String, IRI> idXLabelMap;
	
	public ZthesToRdfMapper(Zthes zThes, LifterContext lifterContext) {
		this.zThes = zThes;
		this.lifterContext = lifterContext;
		model = new LinkedHashModel();
		vf = SimpleValueFactory.getInstance();
		idConceptMap = new HashMap<String, IRI>();
		idXLabelMap = new HashMap<String, IRI>();
	}
	
	public Model map() throws URIGenerationException {
		for (Term term : zThes.getTerms()) {
			if (term.getTermType() == TermType.PT) {
				//termId, termName, termLanguage
				IRI concept = createConcept(term);
				//map attributes only for preferred terms (concept)
				mapAttributes(concept, term);
			}
			//relations
			mapRelations(term);
		}
		return model;
	}
	
	/*
	 * termQualifier NO
	 * termCreatedDate
	 * termCreatedBy	? (rimosso da VB3?)
	 * termModifiedDate
	 * termModifiedBy	? (rimosso da VB3?)
	 * termStatus
	 * termNote
	 * 
	 * ???
	 * termVocabulary
	 * termCategory
	 * termApproval
	 * termSortkey
	 * postings
	 */
	private void mapAttributes(IRI termResource, Term term) {
		enrichTermCreatedDate(termResource, term);
		enrichTermModifiedDate(termResource, term);
		enrichTermCreatedBy(termResource, term);
		enrichTermStatus(termResource, term);
		enrichNotes(termResource, term);
	}
	
	private void mapRelations(Term term) throws URIGenerationException {
		for (Relation rel : term.getRelations()) {
			Term relatedTerm = zThes.getTermById(rel.getTermId());
			if (isRelationCompliant(term, rel.getRelationType(), relatedTerm)) { //be sure that the term types are correct
				IRI subject = null;
				IRI predicate = null;
				Value object = null;
				if (rel.getRelationType() == RelationType.BT) { //broader relation
					subject = createConcept(term);
					predicate = SKOS.BROADER;
					object = createConcept(relatedTerm);
				} else if (rel.getRelationType() == RelationType.NT) { //narrower relation
					subject = createConcept(term);
					predicate = SKOS.NARROWER;
					object = createConcept(relatedTerm);
				} else if (rel.getRelationType() == RelationType.RT) { //RT is related relation between PT
					subject = createConcept(term);
					predicate = SKOS.RELATED;
					object = createConcept(relatedTerm);
				} else if (rel.getRelationType() == RelationType.LE) { //linguistic equivalent
					//term represents a concept and relatedTerm is a preferred label in a different language
					subject = createConcept(term);
					if (lifterContext.getLexicalizationModel().equals(Project.SKOS_LEXICALIZATION_MODEL)) {
						predicate = SKOS.PREF_LABEL;
						object = createLiteral(relatedTerm);
					} else if (lifterContext.getLexicalizationModel().equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
						predicate = SKOSXL.PREF_LABEL;
						object = createXLabel(relatedTerm);
					}
				} else if (rel.getRelationType() == RelationType.UF) { //Use for: the current term should be used in preference to the related one
					//term represents a concept and the relatedTerm is an altLabel
					subject = createConcept(term);
					if (lifterContext.getLexicalizationModel().equals(Project.SKOS_LEXICALIZATION_MODEL)) {
						predicate = SKOS.ALT_LABEL;
						object = createLiteral(relatedTerm);
					} else if (lifterContext.getLexicalizationModel().equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
						predicate = SKOSXL.ALT_LABEL;
						object = createXLabel(relatedTerm);
					}
				} else if (rel.getRelationType() == RelationType.USE) { //Use instead: the related term should be used in preference to the current one
					//term represents an altLabel of the relatedTerm (that is the concept)
					subject = createConcept(relatedTerm);
					if (lifterContext.getLexicalizationModel().equals(Project.SKOS_LEXICALIZATION_MODEL)) {
						predicate = SKOS.ALT_LABEL;
						object = createLiteral(term);
					} else if (lifterContext.getLexicalizationModel().equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
						predicate = SKOSXL.ALT_LABEL;
						object = createXLabel(term);
					}
				} else {
					logger.debug("Unknown relation " + rel.getRelationType() + ". Ignored.");
				}
				model.add(subject, predicate, object);
			} else {
				logger.debug("Wrong term types (" + term.getTermType() + " -> " + relatedTerm.getTermType() + ") in relation" + rel.getRelationType());
			}
		}
	}
	
	private void enrichTermStatus(IRI resource, Term term) {
		TermStatus termStatus = term.getTermStatus();
		if (termStatus == TermStatus.deactivated) {
			model.add(resource, OWL.DEPRECATED, vf.createLiteral(true));
		}
	}
	
	private void enrichTermCreatedDate(IRI resource, Term term) {
		//dct:created
		String createdDate = term.getTermCreatedDate();
		if (createdDate != null) {
			SimpleDateFormat parser = new SimpleDateFormat("dd.MM.yyyy");
			try {
				Date date = parser.parse(createdDate);
				GregorianCalendar c = new GregorianCalendar();
				c.setTime(date);
				XMLGregorianCalendar gregorianDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
				Literal dateLiteral = vf.createLiteral(gregorianDate);
				model.add(resource, DCTERMS.CREATED, dateLiteral);
			} catch (ParseException | DatatypeConfigurationException e) {
				logger.debug("Unable to parse date " + createdDate);
			}
		}
	}
	
	private void enrichTermModifiedDate(IRI resource, Term term) {
		//dct:modified
		String modifiedDate = term.getTermModifiedDate();
		if (modifiedDate != null) {
			SimpleDateFormat parser = new SimpleDateFormat("dd.MM.yyyy");
			try {
				Date date = parser.parse(modifiedDate);
				GregorianCalendar c = new GregorianCalendar();
				c.setTime(date);
				XMLGregorianCalendar gregorianDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
				Literal dateLiteral = vf.createLiteral(gregorianDate);
				model.add(resource, DCTERMS.MODIFIED, dateLiteral);
			} catch (ParseException | DatatypeConfigurationException e) {
				logger.debug("Unable to parse date " + modifiedDate);
			}
		}
	}
	
	private void enrichTermCreatedBy(IRI resource, Term term) {
		//dct:creator
		String createdBy = term.getTermCreatedBy();
		if (createdBy != null) {
			model.add(resource, DCTERMS.CREATOR, vf.createLiteral(createdBy));
		}
	}
	
	private void enrichNotes(IRI concept, Term term) {
		List<TermNote> notes = term.getTermNotes();
		for (TermNote note : notes) {
			Literal noteLiteral = vf.createLiteral(note.getNote());
			String noteLabel = note.getLabel();
			if (noteLabel != null) {
				if (note.getLabel().equalsIgnoreCase("changeNote")) {
					model.add(concept, SKOS.CHANGE_NOTE, noteLiteral);
				} else if (note.getLabel().equalsIgnoreCase("definition")) {
					model.add(concept, SKOS.DEFINITION, noteLiteral);
				} else if (note.getLabel().equalsIgnoreCase("editorialNote")) {
					model.add(concept, SKOS.EDITORIAL_NOTE, noteLiteral);
				} else if (note.getLabel().equalsIgnoreCase("example")) {
					model.add(concept, SKOS.EXAMPLE, noteLiteral);
				} else if (note.getLabel().equalsIgnoreCase("historyNote")) {
					model.add(concept, SKOS.HISTORY_NOTE, noteLiteral);
				} else if (note.getLabel().equalsIgnoreCase("scopeNote")) {
					model.add(concept, SKOS.SCOPE_NOTE, noteLiteral);
				} else { //default case: label unknown
					model.add(concept, SKOS.NOTE, noteLiteral);
				}
			} else { //no label attr => still in the default case
				model.add(concept, SKOS.NOTE, noteLiteral);
			}
		}
	}
	
	private boolean isRelationCompliant(Term term, RelationType relationType, Term relatedTerm) {
		TermType expectedFrom = relationType.getTermTypeFrom();
		TermType expectedTo = relationType.getTermTypeTo();
		return term.getTermType() == expectedFrom && relatedTerm.getTermType() == expectedTo;
	}
	
	/**
	 * Creates and adds a Concept from the given term. Returns the Concept
	 * @param term
	 * @return
	 * @throws URIGenerationException 
	 */
	private IRI createConcept(Term term) throws URIGenerationException {
		IRI concept;
		concept = idConceptMap.get(term.getTermId()); //get concept from map (in case of LE relations)
		if (concept == null) { //in case concept was not yet created for the given term (or one of its LE terms)
			//create a new concept...
			concept = lifterContext.generateIRI("concept", new HashMap<>());
			//...put it and its LE terms into the map
			idConceptMap.put(term.getTermId(), concept);
			for (TermEntity linguisticEquivalentTerm : term.getRelations(RelationType.LE)) {
				idConceptMap.put(linguisticEquivalentTerm.getTermId(), concept);
			}
		}
		//add concept and its label to the connection
		model.add(concept, RDF.TYPE, SKOS.CONCEPT);
		if (lifterContext.getLexicalizationModel().equals(Project.SKOS_LEXICALIZATION_MODEL)) {
			model.add(concept, SKOS.PREF_LABEL, createLiteral(term));
		} else if (lifterContext.getLexicalizationModel().equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			IRI prefLabel = createXLabel(term);
			model.add(concept, SKOSXL.PREF_LABEL, prefLabel);
		}
		return concept;
	}
	
	/**
	 * Creates and adds an xLabel from termName of the given term. Returns the xLabel
	 * @param term
	 * @return
	 * @throws URIGenerationException 
	 */
	private IRI createXLabel(TermEntity term) throws URIGenerationException {
		IRI xLabel;
		xLabel = idXLabelMap.get(term.getTermId()); //get xLabel from map (in case xlabel was already created)
		if (xLabel == null) { //in case xLabel was not yet created for the given term
			Literal literalForm = createLiteral(term);
			Map<String, Value> argsMap = new HashMap<>();
			argsMap.put("lexicalForm", literalForm);
			xLabel = lifterContext.generateIRI("xLabel", argsMap);
			model.add(xLabel, RDF.TYPE, SKOSXL.LABEL);
			model.add(xLabel, SKOSXL.LITERAL_FORM, literalForm);
			
			idXLabelMap.put(term.getTermId(), xLabel);
		}
		return xLabel;
	}
	
	
	private Literal createLiteral(TermEntity term) {
		Literal literal;
		if (term.getTermLanguage() != null) {
			String normalizedTermLang = new Locale(term.getTermLanguage()).getLanguage();
			literal = vf.createLiteral(term.getTermName(), normalizedTermLang);
		} else { //no termLanguage
			literal = vf.createLiteral(term.getTermName());
		}
		return literal;
	}
	
}
