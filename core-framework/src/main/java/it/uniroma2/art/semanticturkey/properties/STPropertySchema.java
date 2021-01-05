package it.uniroma2.art.semanticturkey.properties;

import java.util.HashMap;
import java.util.Map;

/**
 * A schema for a property in an {@link STProperties} object.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertySchema implements STProperties {

	@Override
	public String getShortName() {
		return null;
	}

	@STProperty(description = "")
	@Required
	public boolean required = false;

	@STProperty(description = "")
	@Required
	public Map<String, String> description = new HashMap<>();

	@STProperty(description = "")
	@Required
	public Map<String, String> displayName = new HashMap<>();

	@STProperty(description = "")
	@Enumeration(value = { "boolean", "short", "integer", "long", "float", "double", "IRI", "BNode",
			"Resource", "Literal", "RDFValue", "URL", "java.lang.String" })
	@Required
	public String type = "java.lang.String";

	@Enumeration(value = { "List", "Set", "None" })
	@Required
	public String container = "None";
}
