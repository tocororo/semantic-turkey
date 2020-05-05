package it.uniroma2.art.semanticturkey.services.core.history;

/**
 * Resuming metadata about an operation parameter.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */

public class ParameterInfo {
	private final String name;
	private final String value;

	public ParameterInfo(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
}
