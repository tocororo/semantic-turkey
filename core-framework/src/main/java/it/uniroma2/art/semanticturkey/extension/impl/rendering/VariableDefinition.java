package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Definition of a variable to be used inside a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class VariableDefinition implements STProperties {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.VariableDefinition";

		public static final String shortName = keyBase + ".shortName";
		public static final String propertyPath$description = keyBase + ".propertyPath.description";
		public static final String propertyPath$displayName = keyBase + ".propertyPath.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.propertyPath$description + "}", displayName =  "{" + MessageKeys.propertyPath$displayName + "}")
	public List<IRI> propertyPath;

}
