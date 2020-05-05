package it.uniroma2.art.semanticturkey.config.exporter;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.ExtensionSpecificationByRef;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A stored export configuration
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class StoredExportConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Stored Export Configuration";
	}

	@STProperty(description = "The graphs to be exported. An empty array means all graphs the name of which is an IRI", displayName = "Graphs")
	@Required
	public List<IRI> graphs;

	@STProperty(description = "A sequence of RDFTransformers, each applied to a subset of the exported graphs", displayName = "Transformation pipeline")
	@Required
	public List<Pair<ExtensionSpecificationByRef, List<IRI>>> transformationPipeline;

	@STProperty(description = "Tells if inferred triples should be included", displayName = "Include inferred triples")
	@Required
	public Boolean includeInferred;

	@STProperty(description = "An optional ReformattingExporter that reformats the data to a (usually non-RDF) format", displayName = "Reformatting exporter specification")
	public ExtensionSpecificationByRef reformattingExporterSpec;

	@STProperty(description = "An optional Deployer to export the data somewhere instead of simply downloading it", displayName = "Deployer specification")
	public ExtensionSpecificationByRef deployerSpec;

}
