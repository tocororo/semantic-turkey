package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.deleteproperty;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DeletePropertyValueRDFTransformerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Delete Property Value RDF Transformer";
	}

	@STProperty(description = "The subject of the filtered out triple")
	@Required
	public String resource;

	@STProperty(description = "The predicate of the filtered out triple")
	@Required
	public String property;

	@STProperty(description = "The value of the triple being filtered out. If not set, "
			+ "then all triples of the form <resource, predicate, *> are deleted")
	public String value = null;
}
