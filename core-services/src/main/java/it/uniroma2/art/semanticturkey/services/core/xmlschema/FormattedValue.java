package it.uniroma2.art.semanticturkey.services.core.xmlschema;

public class FormattedValue {
	private final String type;
	private final String formtted;
	private final boolean validated;

	public FormattedValue(String type, String formatted, boolean validated) {
		this.type = type;
		this.formtted = formatted;
		this.validated = validated;
	}

	public String getType() {
		return type;
	}

	public String getFormtted() {
		return formtted;
	}

	public boolean isValidated() {
		return validated;
	}
}
