package it.uniroma2.art.semanticturkey.properties;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds a reference to an an extension together with an optional relative reference to a stored
 * configuration.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public final class ExtensionSpecificationByRef {

	private final String extensionID;
	private final @Nullable String configRef;

	public ExtensionSpecificationByRef(@JsonProperty(value = "extensionID", required = true) String extensionID,
			@JsonProperty("configRef") String configRef) {
		this.extensionID = extensionID;
		this.configRef = configRef;
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
	 * Returns the relative reference to a stored configuration
	 * 
	 * @return the relative reference to a stored configuration, or <code>null</code>
	 */
	public String getConfigRef() {
		return configRef;
	}

}
