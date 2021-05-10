package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

public class SimpleValue implements ValueForProp{

	private String value;

	public SimpleValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object object){
		if(object instanceof  SimpleValue){
			return value.equals(((SimpleValue) object).getValue());
		}
		return  false;

	}
}
