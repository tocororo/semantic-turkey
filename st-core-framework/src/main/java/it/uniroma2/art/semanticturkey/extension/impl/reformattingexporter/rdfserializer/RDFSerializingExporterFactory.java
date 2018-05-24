package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer;

import static java.util.stream.Collectors.toList;

import java.util.List;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.resources.DataFormat;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * Factory for the instantiation of {@link RDFSerializingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFSerializingExporterFactory
		implements NonConfigurableExtensionFactory<RDFSerializingExporter>, FormatCapabilityProvider {

	@Override
	public String getName() {
		return "RDF Serializing Exporter";
	}

	@Override
	public String getDescription() {
		return "A Reformatting Exporter that serializes RDF data according to a concrete RDF syntax";
	}

	@Override
	public RDFSerializingExporter createInstance() {
		return new RDFSerializingExporter();
	}

	@Override
	public List<DataFormat> getFormats() {
		return RDF4JUtilities.getOutputFormats().stream().map(DataFormat::valueOf).collect(toList());
	}

}
