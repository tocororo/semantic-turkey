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

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.importer.StoredImportConfiguration";

		public static final String loaderSpec$description = keyBase + ".loaderSpec.description";
		public static final String loaderSpec$displayName = keyBase + ".loaderSpec.displayName";
		public static final String rdfLifterSpec$description = keyBase + ".rdfLifterSpec.description";
		public static final String rdfLifterSpec$displayName = keyBase + ".rdfLifterSpec.displayName";
		public static final String format$description = keyBase + ".format.description";
		public static final String format$displayName = keyBase + ".format.displayName";
		public static final String transformationPipeline$description = keyBase + ".transformationPipeline.description";
		public static final String transformationPipeline$displayName = keyBase + ".transformationPipeline.displayName";
	}

	@Override
	public String getShortName() {
		return "Stored Import Configuration";
	}

	@STProperty(description = "An optional Loader that is able to fetch data from a certain source type", displayName = "Loader specification")
	public ExtensionSpecificationByRef loaderSpec;

	@STProperty(description = "An optional RDFLifter to triplify data fetched from some non-RDF source", displayName = "RDFLifter specification")
	public ExtensionSpecificationByRef rdfLifterSpec;

	@STProperty(description = "An optional data format", displayName = "Format")
	public String format;

	@STProperty(description = "A sequence of RDFTransformers applied to the imported data", displayName = "Transformation pipeline")
	@Required
	public List<ExtensionSpecificationByRef> transformationPipeline;
}
