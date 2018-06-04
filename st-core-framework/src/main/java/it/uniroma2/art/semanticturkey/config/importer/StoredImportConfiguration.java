package it.uniroma2.art.semanticturkey.config.importer;

import java.util.List;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.ExtensionSpecificationByRef;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A stored export configuration
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class StoredImportConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Stored Import Configuration";
	}

	@STProperty(description = "An optional Loader that is able to fetch data from a certain source type", displayName = "Loader specification")
	public ExtensionSpecificationByRef loaderSpec;

	@STProperty(description = "An optional RDFLifter to triplify data fetched from some non-RDF source", displayName = "RDFLifter specification")
	public ExtensionSpecificationByRef rdfLifterSpec;

	@STProperty(description = "A sequence of RDFTransformers applied to the imported data", displayName = "Transformation pipeline")
	@Required
	public List<ExtensionSpecificationByRef> transformationPipeline;
}
