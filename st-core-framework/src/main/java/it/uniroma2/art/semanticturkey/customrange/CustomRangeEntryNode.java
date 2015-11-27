package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ConverterMention;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

public class CustomRangeEntryNode extends CustomRangeEntry {
	
	CustomRangeEntryNode(String id, String name, String description, String ref) {
		super(id, name, description, ref);
	}

	@Override
	public Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException {
		Collection<UserPromptStruct> form = new ArrayList<UserPromptStruct>();
		String ref = getRef();
		/* the ref in case of node CRE contains a rdfType (uri or literal), followed by an optional
		 * datatype (in case of literal) and an optional converter. */
		if (ref.startsWith("uri")){
			UserPromptStruct upStruct = new UserPromptStruct("value", "value", "uri");
			String converter = "http://art.uniroma2.it/coda/contracts/default";
			if (ref.contains("(") && ref.contains(")")){
				converter = ref.substring(ref.indexOf("(")+1, ref.indexOf(")"));
			}
			upStruct.setConverter(converter);
			form.add(upStruct);
		} else if (ref.startsWith("literal")){
			UserPromptStruct upStruct = new UserPromptStruct("value", "value", "literal");
			String converter = "http://art.uniroma2.it/coda/contracts/default";
			if (ref.contains("(") && ref.endsWith(")")){
				converter = ref.substring(ref.lastIndexOf("(")+1, ref.indexOf(")"));
				ref = ref.substring(0, ref.lastIndexOf("("));//remove the converter from the end of the ref
			}
			upStruct.setConverter(converter);
			if (ref.contains("^^")){
				String datatype = ref.substring(ref.indexOf("^^")+2);
				upStruct.setLiteralDatatype(datatype);
			} else if (ref.contains("@")){
				String lang = ref.substring(ref.indexOf("@")+1);
				upStruct.setLiteralLang(lang);
			}
			form.add(upStruct);
		} else {
			throw new PRParserException("Invalid ref in CustomRangeEntry " + getId());
		}
		return form;
	}
	
	@Override
	public void saveXML(){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element creElement = doc.createElement("customRangeEntry");
			doc.appendChild(creElement);
			creElement.setAttribute("id", this.getId());
			creElement.setAttribute("name", this.getName());
			creElement.setAttribute("type", this.getType());
			
			Element descrElement = doc.createElement("description");
			descrElement.setTextContent(this.getDescription());
			creElement.appendChild(descrElement);
			
			Element refElement = doc.createElement("ref"); 
			refElement.setTextContent(this.getRef());
			creElement.appendChild(refElement);
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(CustomRangeProvider.getCustomRangeEntryFolder(), this.getId() + ".xml"));
			transformer.transform(source, result);
		} catch (ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}
	

}
