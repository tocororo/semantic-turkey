package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.zthesdeserializer;

import java.util.Arrays;
import java.util.List;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Factory for the instantiation of {@link ZthesDeserializingLifter}.
 * 
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class ZthesDeserializingLifterFactory
		implements NonConfigurableExtensionFactory<ZthesDeserializingLifter>, FormatCapabilityProvider {

	@Override
	public String getName() {
		return "Zthes Deserializing Lifter";
	}

	@Override
	public String getDescription() {
		return "An RDF Lifter that deserializes RDF data according to a concrete Zthes syntax";
	}

	@Override
	public ZthesDeserializingLifter createInstance() {
		return new ZthesDeserializingLifter();
	}

	@Override
	public List<DataFormat> getFormats() {
		return Arrays.asList(new DataFormat("XML", "application/xml", "xml"));
	}

}
