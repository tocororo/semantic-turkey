package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer;

import static java.util.stream.Collectors.toList;

import java.util.List;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.resources.DataFormat;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * Factory for the instantiation of {@link RDFDeserializingLifter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFDeserializingLifterFactory
		implements NonConfigurableExtensionFactory<RDFDeserializingLifter>, FormatCapabilityProvider {

	@Override
	public String getName() {
		return "RDF Deserializing Exporter";
	}

	@Override
	public String getDescription() {
		return "An RDF Lifter that deserializes RDF data according to a concrete RDF syntax";
	}

	@Override
	public RDFDeserializingLifter createInstance() {
		return new RDFDeserializingLifter();
	}

	@Override
	public List<DataFormat> getFormats() {
		return RDF4JUtilities.getInputFormats().stream().map(DataFormat::valueOf).collect(toList());
	}

}
