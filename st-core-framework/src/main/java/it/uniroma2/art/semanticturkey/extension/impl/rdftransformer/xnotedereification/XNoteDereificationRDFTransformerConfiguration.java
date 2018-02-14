package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xnotedereification;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XNoteDereificationRDFTransformerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "XNote Dereification RDF Transformer";
	}

	@STProperty(description = "Preserves reified notes in the output")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean preserveReifiedNotes = true;
}
