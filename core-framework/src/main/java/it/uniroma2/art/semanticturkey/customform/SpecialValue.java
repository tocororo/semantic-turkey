package it.uniroma2.art.semanticturkey.customform;

import org.eclipse.rdf4j.model.Value;

public class SpecialValue {
	
	private Value rdf4jValue;
	private CustomFormValue cfValue;
	
	public boolean isRdf4jValue() {
		return this.rdf4jValue != null;
	}
	
	public boolean isCustomFormValue() {
		return this.cfValue != null;
	}
	
	public void setRdf4jValue(Value value) {
		this.rdf4jValue = value;
	}
	
	public Value getRdf4jValue() {
		return this.rdf4jValue;
	}
	
	public void setCustomFormValue(CustomFormValue cfValue) {
		this.cfValue = cfValue;
	}
	
	public CustomFormValue getCustomFormValue() {
		return this.cfValue;
	}
	
}
