package it.uniroma2.art.semanticturkey.zthes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlReader {
	
	private Document doc;

	public Zthes parseZThes(InputStream is) throws ZthesException, SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(is);
		doc.getDocumentElement().normalize();
		return parseZThes();
		
	}
	
	public Zthes parseZThes(File file) throws ZthesException, ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(file);
		doc.getDocumentElement().normalize();
		return parseZThes();
	}
	
	private Zthes parseZThes() throws ZthesException {
		Zthes zthes = new Zthes();
		Element zthesElement = doc.getDocumentElement();
		if (!zthesElement.getNodeName().equals(Zthes.Tag.ZTHES)) {
			throw new ZthesException("Unknown root element " + zthesElement.getNodeName() + ". " + Zthes.Tag.ZTHES + " expected");
		}
		NodeList zthesChildNodes = zthesElement.getChildNodes();
		for (int i = 0; i < zthesChildNodes.getLength(); i++) {
			Node childNode = zthesChildNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) childNode;
				if (childElement.getNodeName().equals(Term.Tag.TERM)) {
					Term term = parseTerm(childElement);
					zthes.addTerm(term);
				}
			}
		}
		//terms referenced in relations, might be not defined as term element => check and repair
		for (Term t : zthes.getTerms()) {
			for (Relation r: t.getRelations()) {
				Term relatedTerm = zthes.getTermById(r.getTermId());
				if (relatedTerm == null) { //not defined => create term
					relatedTerm = new Term(r.getTermId(), r.getTermName());
					relatedTerm.setTermLanguage(r.getTermLanguage());
					relatedTerm.setTermQualifier(r.getTermQualifier());
					relatedTerm.setTermType(r.getTermType());
				}
				zthes.addTerm(relatedTerm);
			}
		}

		return zthes;
	}
	
	private Term parseTerm(Element termElement) {
		Term term = null;
		//mandatory
		String termId = null;
		//optional
		String termName = null;
		String termLanguage = null;
		List<Relation> relations = new ArrayList<Relation>();
		String termCreatedBy = null;
		String termCreatedDate = null;
		String termModifiedBy = null;
		String termModifiedDate = null;
		List<TermNote> termNotes = new ArrayList<TermNote>();
		String termQualifier = null;
		TermStatus termStatus = null;
		TermType termType = null;
		NodeList termChildNodes = termElement.getChildNodes();
		for (int i = 0; i < termChildNodes.getLength(); i++) {
			Node childNode = termChildNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) childNode;
				if (childElement.getNodeName().equals(Term.Tag.RELATION)) {
					relations.add(parseRelation(childElement));
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_CREATED_BY)) {
					termCreatedBy = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_CREATED_DATE)) {
					termCreatedDate = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_ID)) {
					termId = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_LANGUAGE)) {
					termLanguage = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_MODIFIED_BY)) {
					termModifiedBy = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_MODIFIED_DATE)) {
					termModifiedDate = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_NAME)) {
					termName = sanitizeTextContent(childElement.getTextContent());
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_NOTE)) {
					termNotes.add(parseTermNote(childElement));
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_QUALIFIER)) {
					termQualifier = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_STATUS)) {
					String termStatusValue = childElement.getTextContent();
					if (!termStatusValue.equals("")) {
						termStatus = TermStatus.valueOf(termStatusValue);
					}
				} else if (childElement.getNodeName().equals(Term.Tag.TERM_TYPE)) {
					termType = TermType.valueOf(childElement.getTextContent());
				} else {
//					logger.warn("Unknown child element of " + Term.Tag.TERM + " element: '" +
//							childElement.getNodeName() + "'. Element ignored.");
				}
			}
		}
		if (termId != null) {
			term = new Term(termId, termName);
			if (relations != null) {
				term.setRelation(relations);
			}
			if (termCreatedBy != null && !termCreatedBy.equals("")) {
				term.setTermCreatedBy(termCreatedBy);
			}
			if (termCreatedDate != null && !termCreatedDate.equals("")) {
				term.setTermCreatedDate(termCreatedDate);
			}
			if (termLanguage != null && !termLanguage.equals("")) {
				term.setTermLanguage(termLanguage);
			}
			if (termModifiedBy != null && !termModifiedBy.equals("")) {
				term.setTermModifiedBy(termModifiedBy);
			}
			if (termModifiedDate != null && !termModifiedDate.equals("")) {
				term.setTermModifiedDate(termModifiedDate);
			}
			if (termNotes != null) {
				term.setTermNotes(termNotes);
			}
			if (termQualifier != null && !termQualifier.equals("")) {
				term.setTermQualifier(termQualifier);
			}
			if (termStatus != null) {
				term.setTermStatus(termStatus);
			}
			if (termType != null) {
				term.setTermType(termType);
			}
		} else {
//			logger.warn("Mandatory child element not found of " + Term.Tag.TERM + " element");
		}
		return term;
	}
	
	private Relation parseRelation(Element relationElement) {
		Relation relation = null;
		//mandatory
		RelationType relationType = null;
		String termId = null;
		String termName = null;
		//optional
		String sourceDb = null;
		String termQualifier = null;
		TermType termType = null;
		String termLanguage = null;
		float weight = 0.0f;
		
		//Attributes
		NamedNodeMap nodeAttrs = relationElement.getAttributes();
		for (int i = 0; i < nodeAttrs.getLength(); i++) {
			Node nodeAttr = nodeAttrs.item(i);
			if (nodeAttr.getNodeType() == Node.ATTRIBUTE_NODE) {
				Attr attr = (Attr) nodeAttr;
				if (attr.getName().equals(Relation.Attr.WEIGHT)) {
					DecimalFormat format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.GERMAN));
					try {
						weight = format.parse(attr.getValue()).floatValue();
					} catch (ParseException e1) {}
				} else {
//					logger.warn("Unknown attribute of " + Term.Tag.RELATION + " element: '" +
//							attr.getName() + "'. Attribute ignored.");
				}
			}
		}
		//Elements
		NodeList relationChildNodes = relationElement.getChildNodes();
		for (int i = 0; i < relationChildNodes.getLength(); i++) {
			Node childNode = relationChildNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) childNode;
				if (childElement.getNodeName().equals(Relation.Tag.RELATION_TYPE)) {
					relationType = RelationType.valueOf(childElement.getTextContent());
				} else if (childElement.getNodeName().equals(Relation.Tag.SOURCE_DB)) {
					sourceDb = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Relation.Tag.TERM_ID)) {
					termId = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Relation.Tag.TERM_LANGUAGE)) {
					termLanguage = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Relation.Tag.TERM_NAME)) {
					termName = sanitizeTextContent(childElement.getTextContent());
				} else if (childElement.getNodeName().equals(Relation.Tag.TERM_QUALIFIER)) {
					termQualifier = childElement.getTextContent();
				} else if (childElement.getNodeName().equals(Relation.Tag.TERM_TYPE)) {
					termType = TermType.valueOf(childElement.getTextContent());
				} else {
//					logger.warn("Unknown child element of " + Term.Tag.RELATION + " element: '" +
//							childElement.getNodeName() + "'. Element ignored.");
				}
			}
		}
		if (relationType != null && termId != null) {
			relation = new Relation(relationType, termId, termName);
			if (sourceDb != null && !sourceDb.equals("")) {
				relation.setSourceDb(sourceDb);
			}
			if (termQualifier != null && !termQualifier.equals("")) {
				relation.setTermQualifier(termQualifier);
			}
			if (termType != null) {
				relation.setTermType(termType);
			}
			if (termLanguage != null && !termLanguage.equals("")) {
				relation.setTermLanguage(termLanguage);
			}
			if (weight != 0.0f) {
				relation.setWeight(weight);
			}
		}
		return relation;
	}
	
	private TermNote parseTermNote(Element termNoteElement) {
		TermNote termNote = new TermNote(sanitizeTextContent(termNoteElement.getTextContent()));
		//Optional attributes
		String label = null;
		NamedNodeMap nodeAttrs = termNoteElement.getAttributes();
		for (int i = 0; i < nodeAttrs.getLength(); i++) {
			Node nodeAttr = nodeAttrs.item(i);
			if (nodeAttr.getNodeType() == Node.ATTRIBUTE_NODE) {
				Attr attr = (Attr) nodeAttr;
				if (attr.getName().equals(TermNote.Attr.LABEL)) {
					label = attr.getValue();
				}
			}
		}
		if (label != null) {
			termNote.setLabel(label);
		}
		return termNote;
	}

	/**
	 * Removes tabs, newlines and multiple whitespaces from text content string
	 */
	private String sanitizeTextContent(String text) {
		return text.replaceAll("\\n", " ").replaceAll("\\t", " ").replaceAll(" +", " ").trim();
	}

}
