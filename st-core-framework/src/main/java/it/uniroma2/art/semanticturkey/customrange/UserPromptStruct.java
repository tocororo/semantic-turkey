package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.coda.pearl.model.ConverterMention;

public class UserPromptStruct {
	
	private String placeholderId;
	private String userPromptName; //name of the feature userPrompt/example
	private String rdfType; //uri or literal
	private String literalDatatype;
	private String literalLang;
	private ConverterMention converter; //in the future this could be a list of string since a placeholder could be defined through multiple waterfall converters
	private boolean mandatory;
	
	/**
	 * Creates a new UserPromptStruct with the given name and the given type
	 * @param userPromptName name of the feature <code>userPrompt/example</code>
	 * @param rdfType Should be <code>uri</code> or <code>literal</code>
	 */
	public UserPromptStruct(String placeholderId, String userPromptName, String rdfType){
		this.placeholderId = placeholderId;
		this.userPromptName = userPromptName;
		this.rdfType = rdfType;
		this.mandatory = true;
	}
	
	public String getPlaceholderId() {
		return placeholderId;
	}

	public void setPlaceholderId(String placeholderId) {
		this.placeholderId = placeholderId;
	}
	
	public String getUserPromptName() {
		return userPromptName;
	}

	public void setUserPromptName(String userPromptName) {
		this.userPromptName = userPromptName;
	}

	/**
	 * Returns the type of the userPrompt (it can be uri or literal)
	 * @return
	 */
	public String getRdfType() {
		return rdfType;
	}

	public void setRdfType(String rdfType) {
		this.rdfType = rdfType;
	}

	public String getLiteralDatatype() {
		return literalDatatype;
	}

	public void setLiteralDatatype(String literalDatatype) {
		this.literalDatatype = literalDatatype;
	}

	public String getLiteralLang() {
		return literalLang;
	}

	public void setLiteralLang(String literalLang) {
		this.literalLang = literalLang;
	}
	
	public ConverterMention getConverter(){
		return converter;
	}
	
	public void setConverter(ConverterMention converter) {
		this.converter = converter;
	}
	
	public boolean isMandatory(){
		return mandatory;
	}
	
	public void setMandatory(boolean mandatory){
		this.mandatory = mandatory;
	}
	
	public boolean isUri(){
		return rdfType.equals("uri");
	}

	public boolean isLiteral(){
		return rdfType.equals("literal");
	}
	
	public boolean hasDatatype(){
		return literalDatatype != null;
	}
	
	public boolean hasLanguage(){
		return literalLang != null;
	}
	
	public boolean hasConverter(){
		return converter != null;
	}
}
