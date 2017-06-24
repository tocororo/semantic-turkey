package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.Collection;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;

public abstract class CustomForm {
	
	public static String PREFIX = "it.uniroma2.art.semanticturkey.customform.form.";
	
	public enum Types {
		node, graph;
	}
	
	private String id = "";
	private String name = "";
	private String description = "";
	private String ref = "";
	private CustomFormLevel level;
	
	/**
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 */
	CustomForm(String id, String name, String description, String ref){
		this.id = id;
		this.name = name;
		this.description = description;
		this.ref = ref;
		this.level = CustomFormLevel.project;
	}
	
	/**
	 * Returns the ID of the {@link CustomForm}
	 * @return
	 */
	public String getId(){
		return id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * Returns the name of the {@link CustomForm}
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
		
	/**
	 * Returns a verbose description about the {@link CustomForm}
	 * @return
	 */
	public String getDescription(){
		return description;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	/**
	 * Returns the ref of the {@link CustomForm}. It could be a CODA rule if the type of the {@link CustomForm}
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
		if (this instanceof CustomFormNode)
			return CustomForm.Types.node.toString();
		else
			return CustomForm.Types.graph.toString();
	}
	
	/**
	 * Returns <code>true</code> if the type of the {@link CustomForm} is "node", <code>false</code> otherwise
	 * @return
	 */
	public boolean isTypeNode(){
		return (this instanceof CustomFormNode); 
	}
	
	public CustomFormNode asCustomFormNode(){
		return (CustomFormNode) this;
	}
	
	/**
	 * Returns <code>true</code> if the type of the {@link CustomForm} is "graph", <code>false</code> otherwise
	 * @return
	 */
	public boolean isTypeGraph(){
		return (this instanceof CustomFormGraph);
	}
	
	public CustomFormGraph asCustomFormGraph(){
		return (CustomFormGraph) this;
	}
	
	public CustomFormLevel getLevel() {
		return level;
	}
	
	public void setLevel(CustomFormLevel level) {
		this.level = level;
	}
	
	/**
	 * Parse the CODA rule contained in the <code>ref</code> tag and build a map of &ltuserPrompt, 
	 * type&gt pairs, where <code>userPrompt</code> is a field of the <code>userPrompt/</code>
	 * feature path and <code>type</code> is the converter used for that feature.
	 * @param codaCore an instance of {@link CODACore} already initialized 
	 * @return
	 * @throws PRParserException
	 * @throws RDFModelNotSetException 
	 * @throws ModelAccessException 
	 */
	public abstract Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException, RDFModelNotSetException;
	
	/**
	 * Serialize the {@link CustomForm} as xml on the given file.
	 */
	public void save(File file) {
		CustomFormXMLHelper.serializeCustomForm(this, file);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj instanceof CustomForm) {
			CustomForm objCF = (CustomForm) obj;
			return (
				this.id.equals(objCF.getId()) && this.level.equals(objCF.getLevel())
			);
		} else {
			return false;
		}
	}

}
