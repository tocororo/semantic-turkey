package it.uniroma2.art.semanticturkey.properties;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Holds a reference to an an extension together with an optional inline configuration.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public final class ExtensionSpecification {

	private final String extensionID;
	private final @Nullable ObjectNode config;

	public ExtensionSpecification(@JsonProperty(value = "extensionID", required = true) String extensionID,
			@JsonProperty("config") ObjectNode config) {
		this.extensionID = extensionID;
		this.config = config;
	}

	/**
	 * Returns the extension identifier
	 * 
	 * @return the extension identifier
	 */
	public String getExtensionID() {
		return extensionID;
	}

	/**
	 * Returns the configuration
	 * 
	 * @return the configuration, or <code>null</code>
	 */
	public ObjectNode getConfig() {
		return config;
	}

}
