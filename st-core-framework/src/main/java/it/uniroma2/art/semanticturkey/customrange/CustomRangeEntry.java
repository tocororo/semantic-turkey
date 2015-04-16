package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.PRParserException;

import java.io.File;
import java.io.FileNotFoundException;
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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class CustomRangeEntry {
	
	private String id = "";
	private String name = "";
	private String description = "";
	private String ref = "";
	
	/**
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @throws FileNotFoundException 
	 */
	CustomRangeEntry(String id, String name, String description, String ref){
		this.id = id;
		this.name = name;
		this.description = description;
		this.ref = ref;
	}
	
	/**
	 * Returns the ID of the CustomRangeEntry
	 * @return
	 */
	public String getId(){
		return id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * Returns the name of the CustomRangeEntry
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
		
	/**
	 * Returns a verbose description about the CustomRangeEntry
	 * @return
	 */
	public String getDescription(){
		return description;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	/**
	 * Returns the ref of the CustomRangeEntry. It could be a CODA rule if the type of the CustomRangeEntry
	 * is <coda>graph</code>, or a CODA converter if the type is <code>node</code>.
	 * @return
	 */
	public String getRef(){
		return ref;
	}
	
	public void setRef(String ref){
		this.ref = ref;
	}
	
	public String getType(){
		if (this instanceof CustomRangeEntryNode)
			return "node";
		else
			return "graph";
	}
	
	/**
	 * Returns true if the type of the Custom Range Entry is "node", false otherwise
	 * @return
	 */
	public boolean isTypeNode(){
		return (this instanceof CustomRangeEntryNode); 
	}
	
	public CustomRangeEntryNode asCustomRangeEntryNode(){
		return (CustomRangeEntryNode) this;
	}
	
	/**
	 * Returns true if the type of the Custom Range Entry is "graph", false otherwise
	 * @return
	 */
	public boolean isTypeGraph(){
		return (this instanceof CustomRangeEntryGraph);
	}
	
	public CustomRangeEntryGraph asCustomRangeEntryGraph(){
		return (CustomRangeEntryGraph) this;
	}
	
	/**
	 * Parse the CODA rule contained in the <code>ref</code> tag and build a map of &ltuserPrompt, 
	 * type&gt pairs, where <code>userPrompt</code> is a field of the <code>userPrompt/</code>
	 * feature path and <code>type</code> is the converter used for that feature.
	 * @param codaCore an instance of CODACore already initialized 
	 * @return
	 * @throws PRParserException
	 */
	public abstract Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException;
	
	/**
	 * Serialize the CustomRangeEntry on a xml file.
	 */
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
			if (this.isTypeNode()){
				refElement.setTextContent(this.getRef());
			} else { //this.isTypeGraph
				CDATASection cdata = doc.createCDATASection(this.getRef());
				refElement.appendChild(cdata);
			}
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
