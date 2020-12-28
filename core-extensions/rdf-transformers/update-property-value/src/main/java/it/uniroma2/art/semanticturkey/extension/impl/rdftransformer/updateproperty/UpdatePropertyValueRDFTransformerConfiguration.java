package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueRDFTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty.UpdatePropertyValueRDFTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String resource$description = keyBase + ".resource.description";
		public static final String resource$displayName = keyBase + ".resource.displayName";
		public static final String property$description = keyBase + ".property.description";
		public static final String property$displayName = keyBase + ".property.displayName";
		public static final String value$description = keyBase + ".value.description";
		public static final String value$displayName = keyBase + ".value.displayName";
		public static final String oldValue$description = keyBase + ".oldValue.description";
		public static final String oldValue$displayName = keyBase + ".oldValue.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.resource$description + "}", displayName = "{" + MessageKeys.resource$displayName + "}")
	@Required
	public Resource resource;

	@STProperty(description = "{" + MessageKeys.property$description + "}", displayName = "{" + MessageKeys.property$displayName + "}")
	@Required
	@HasRole(RDFResourceRole.property)
	public IRI property;

	@STProperty(description = "{" + MessageKeys.value$description + "}", displayName = "{" + MessageKeys.value$displayName + "}")
	@Required
	public Value value = null;

	@STProperty(description = "{" + MessageKeys.oldValue$description + "}", displayName = "{" + MessageKeys.oldValue$displayName + "}")
	public Value oldValue = null;

}
