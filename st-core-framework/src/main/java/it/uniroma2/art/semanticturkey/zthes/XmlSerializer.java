package it.uniroma2.art.semanticturkey.zthes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlSerializer {
	
	public void serialize(Zthes zThes, File file) throws ZthesSerializationException {
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element zThesElement = doc.createElement(Zthes.Tag.ZTHES);
			doc.appendChild(zThesElement);
			
			for (Term term : zThes.getTerms()) {
				appendTerm(doc, zThesElement, zThes, term);
			}
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new FileOutputStream(file));
			transformer.transform(source, result);
			result.getOutputStream().close();
			
		} catch (ParserConfigurationException | TransformerException | IOException e) {
			throw new ZthesSerializationException(e);
		}
		
		
	}
	
	private void appendTerm(Document doc, Element zThesElement, Zthes zThes, Term term) {
		Element termElement = doc.createElement(Term.Tag.TERM);
		zThesElement.appendChild(termElement);
		
		appendTermEntityElements(doc, termElement, term);
		
		List<TermNote> termNote = term.getTermNotes();
		if (termNote.size() > 0) {
			for (TermNote tn : termNote) {
				Element termNoteElement = doc.createElement(Term.Tag.TERM_NOTE);
				termElement.appendChild(termNoteElement);
				termNoteElement.setTextContent(tn.getNote());
				if (tn.getLabel() != null) {
					termNoteElement.setAttribute(TermNote.Attr.LABEL, tn.getLabel().toString());
				}
			}
		}
	
		TermStatus termStatus = term.getTermStatus();
		if (termStatus != null) {
			Element termStatusElement = doc.createElement(Term.Tag.TERM_STATUS);
			termElement.appendChild(termStatusElement);
			termStatusElement.setTextContent(termStatus.toString());
		}
	
		String termCreatedDate = term.getTermCreatedDate();
		if (termCreatedDate != null) {
			Element termCreatedDateElement = doc.createElement(Term.Tag.TERM_CREATED_DATE);
			termElement.appendChild(termCreatedDateElement);
			termCreatedDateElement.setTextContent(termCreatedDate);
		}
	
		String termCreatedBy = term.getTermCreatedBy();
		if (termCreatedBy != null) {
			Element termCreatedByElement = doc.createElement(Term.Tag.TERM_CREATED_BY);
			termElement.appendChild(termCreatedByElement);
			termCreatedByElement.setTextContent(termCreatedBy);
		}
	
		String termModifiedDate = term.getTermModifiedBy();
		if (termModifiedDate != null) {
			Element termModifiedDateElement = doc.createElement(Term.Tag.TERM_MODIFIED_DATE);
			termElement.appendChild(termModifiedDateElement);
			termModifiedDateElement.setTextContent(termModifiedDate);
		}
	
		String termModifiedBy = term.getTermModifiedBy();
		if (termModifiedBy != null) {
			Element termModifiedByElement = doc.createElement(Term.Tag.TERM_MODIFIED_BY);
			termElement.appendChild(termModifiedByElement);
			termModifiedByElement.setTextContent(termModifiedBy);
		}
		
		//Relations
		List<Relation> relation = term.getRelations();
		if (relation.size() > 0) {
			for (Relation r : relation) {
				Element relationElement = doc.createElement(Term.Tag.RELATION);
				termElement.appendChild(relationElement);
				
				RelationType relationType = r.getRelationType();
				Element relationTypeElement = doc.createElement(Relation.Tag.RELATION_TYPE);
				relationElement.appendChild(relationTypeElement);
				relationTypeElement.setTextContent(relationType.toString());
				
					float relationWeight = r.getWeight();
					if (relationWeight != 0.0f) {
						Element relationWeightElement = doc.createElement("relationWeight");
						relationElement.appendChild(relationWeightElement);
						relationWeightElement.setTextContent(relationWeight + "");
					}
				
				Term relatedTerm = zThes.getTermById(r.getTermId());
				appendTermEntityElements(doc, relationElement, relatedTerm);
				
				String sourceDb = r.getSourceDb();
				if (sourceDb != null) {
					Element sourceDbElement = doc.createElement(Relation.Tag.SOURCE_DB);
					relationElement.appendChild(sourceDbElement);
					relationElement.setTextContent(sourceDb);
				}
				
			}
		}
	}
	
	private void appendTermEntityElements(Document doc, Element parentElement, Term term) {
		
		Element termIdElement = doc.createElement(Term.Tag.TERM_ID);
		parentElement.appendChild(termIdElement);
		termIdElement.setTextContent(term.getTermId());
		
		String termName = term.getTermName();
		if (termName != null) {
			Element termNameElement = doc.createElement(Term.Tag.TERM_NAME);
			parentElement.appendChild(termNameElement);
			termNameElement.setTextContent(termName);
		}
		
		String termQualifier = term.getTermQualifier();
		if (termQualifier != null) {
			Element termQualifierElement = doc.createElement(Term.Tag.TERM_QUALIFIER);
			parentElement.appendChild(termQualifierElement);
			termQualifierElement.setTextContent(termQualifier);
		}
		
		
		TermType termType = term.getTermType();
		if (termType != null) {
			Element termTypeElement = doc.createElement(Term.Tag.TERM_TYPE);
			parentElement.appendChild(termTypeElement);
			termTypeElement.setTextContent(termType.toString());
		}
	
		String termLanguage = term.getTermLanguage();
		if (termLanguage != null) {
			Element termLanguageElement = doc.createElement(Term.Tag.TERM_LANGUAGE);
			parentElement.appendChild(termLanguageElement);
			termLanguageElement.setTextContent(termLanguage);
		}
		
	}
	
}
