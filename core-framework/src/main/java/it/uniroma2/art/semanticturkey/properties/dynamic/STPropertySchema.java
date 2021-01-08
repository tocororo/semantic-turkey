package it.uniroma2.art.semanticturkey.properties.dynamic;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A schema for a property in an {@link STProperties} object.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertySchema implements STProperties {

	@Override
	public String getShortName() {
		return "STProperty schema";
	}

	@STProperty(description = "")
	@Required
	public String name;

	@STProperty(description = "")
	@Required
	public boolean required = false;

	@STProperty(description = "")
	@Required
	public List<@LanguageTaggedString Literal> description;

	@STProperty(description = "")
	@Required
	public List<@LanguageTaggedString Literal> displayName;

	@STProperty(description = "")
	@Required
	public STPropertyType type = new STPropertyType();
	
	@STProperty(description = "")
	public STPropertyEnumeration enumeration;

}
