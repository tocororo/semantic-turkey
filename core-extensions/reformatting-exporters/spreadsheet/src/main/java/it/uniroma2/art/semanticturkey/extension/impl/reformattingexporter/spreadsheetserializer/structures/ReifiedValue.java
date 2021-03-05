package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

public class ReifiedValue implements ValueForProp {
	private String iriValue;
	private String literalValue;

	public ReifiedValue(String iriValue, String literalValue) {
		this.iriValue = iriValue;
		this.literalValue = literalValue;
	}

	public String getIriValue() {
		return iriValue;
	}

	public String getLiteralValue() {
		return literalValue;
	}

	@Override
	public boolean equals(Object object){
		boolean same = false;
		if(object instanceof ReifiedValue){
			return (iriValue.equals(((ReifiedValue) object).getIriValue()) &&
					literalValue.equals(((ReifiedValue) object).getLiteralValue()));
		}
		return same;
	}
}
