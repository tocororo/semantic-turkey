package it.uniroma2.art.semanticturkey.customrange;

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

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;

public class CustomRangeEntryNode extends CustomRangeEntry {
	
	CustomRangeEntryNode(String id, String name, String description, String ref) {
		super(id, name, description, ref);
	}

	@Override
	public Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException {
		Collection<UserPromptStruct> form = new ArrayList<UserPromptStruct>();
		String ref = getRef();
		UserPromptStruct upStruct = CustomRangeEntryParseUtils.createUserPromptForNodeEntry(ref, codaCore);
		form.add(upStruct);
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
