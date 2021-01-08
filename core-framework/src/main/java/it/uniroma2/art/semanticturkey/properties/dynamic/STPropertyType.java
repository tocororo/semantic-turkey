package it.uniroma2.art.semanticturkey.properties.dynamic;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A type for an {@link STProperties} property.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertyType implements STProperties {

	@Override
	public String getShortName() {
		return "STProperty type";
	}

	@STProperty(description = "")
	@Required
	@Enumeration(value = { "boolean", "short", "integer", "long", "float", "double", "IRI", "BNode",
			"Resource", "Literal", "RDFValue", "URL", "java.lang.String" })
	public String name = "java.lang.String";

}
