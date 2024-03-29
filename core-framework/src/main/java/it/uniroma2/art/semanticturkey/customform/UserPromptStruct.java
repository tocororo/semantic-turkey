package it.uniroma2.art.semanticturkey.customform;

import it.uniroma2.art.coda.pearl.model.ConverterArgumentExpression;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ConverterRDFLiteralArgument;
import it.uniroma2.art.coda.pearl.model.annotation.Annotation;

import java.util.ArrayList;
import java.util.List;

public class UserPromptStruct {
	
	private String placeholderId; //id of the CODA placeholder (triple: PH - projectionOperator - FS)
	private String featureName; //stdForm or userPrompt
	private String userPromptName; //name of the feature userPrompt/example
	private String rdfType; //uri or literal
	private String literalDatatype; //optional datatype of the literal converter
	private String literalLang; //optional language of the literal converter
	private ConverterMention converter; //in the future this could be a list since a placeholder could be defined through multiple waterfall converters
	private String converterArgPhId; //used only when converter is langString and uses a placeholder as argument
	private String converterArgLangTag; //used only when converter is langString and uses a languageTag (string) as argument
	private boolean mandatory;

	private List<Annotation> annotations;
	
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
		this.converter = new ConverterMention("http://art.uniroma2.it/coda/contracts/default");
		this.annotations = new ArrayList<>();
	}
	
	public String getPlaceholderId() {
		return placeholderId;
	}

	public void setPlaceholderId(String placeholderId) {
		this.placeholderId = placeholderId;
	}

	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featurePath) {
		this.featureName = featurePath;
	}

	public String getUserPromptName() {
		return userPromptName;
	}

	public void setUserPromptName(String userPromptName) {
		this.userPromptName = userPromptName;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}

	/**
	 * Returns the type of the userPrompt (it can be uri or literal)
	 * @return
	 */
	public String getRdfType() {
		return rdfType;
	}

	/**
	 * Sets the type of the user prompt. It must be 'uri' or 'literal'
	 * @param rdfType
	 */
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
	
	public String getConverterArgPhId(){
		return converterArgPhId;
	}
	
	public void setConverterArgPhId(String phId) {
		this.converterArgPhId = phId;
	}

	public String getConverterArgLangTag(){
		return converterArgLangTag;
	}

	public void setConverterArgLangTag(String lang) {
		this.converterArgLangTag = lang;
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
	
	public String toString() {
		String s = "placeholderId: " + placeholderId;
		s += "\nuserPromptName: " + userPromptName;
		s += "\nrdfType: " + rdfType;
		s += "\nliteralDatatype: " + literalDatatype;
		s += "\nliteralLang: " + literalLang;
		s += "\nconverter: " + converter.getURI();
		s += "\nconverterArg: ";
		for (ConverterArgumentExpression converterArgumentExpression : converter.getAdditionalArguments()) {
			//TODO here I assume that all the arguments of a converter are literal. Handle better
			s += "\n\t" + ((ConverterRDFLiteralArgument) converterArgumentExpression).getLiteralValue().getLabel();
		}
		s += "\nmandatory: " + mandatory;
		return s;
	}

}