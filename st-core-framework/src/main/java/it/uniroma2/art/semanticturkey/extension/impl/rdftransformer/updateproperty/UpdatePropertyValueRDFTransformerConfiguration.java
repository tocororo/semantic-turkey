package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueRDFTransformerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Update Property Value RDF Transformer";
	}

	@STProperty(description = "The subject of the filtered triple")
	@Required
	public String resource;

	@STProperty(description = "The predicate of the filtered triple")
	@Required
	public String property;

	@STProperty(description = "The new value to be set")
	@Required
	public String value = null;

	@STProperty(description = "if set, the triple <resource, property, oldValue> is "
			+ "replaced by <resource, property, value>. If not set, then all <resource, property, *> are "
			+ "deleted and <resource, property, value> is written")
	public String oldValue = null;

}
