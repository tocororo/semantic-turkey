package it.uniroma2.art.semanticturkey.extension.impl.rendering;

public class TemplateParsingException extends Exception {

	private static final long serialVersionUID = -4697746396337341038L;

	public TemplateParsingException(String template, int position, String message) {
		super(template + "@" + position + ":" + message);
	}
}
