package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.zthesserializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.extension.extpts.commons.io.FormatCapabilityProvider;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Factory for the instantiation of {@link ZthesSerializingExporter}.
 * 
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class ZthesSerializingExporterFactory 
	implements ConfigurableExtensionFactory<ZthesSerializingExporter, ZthesSerializingExporterConfiguration>,
		PUScopedConfigurableComponent<ZthesSerializingExporterConfiguration>, FormatCapabilityProvider {

	@Override
	public String getName() {
		return "Zthes Serializing Exporter";
	}

	@Override
	public String getDescription() {
		return "A Reformatting Exporter that serializes RDF data according to a concrete Zthes syntax";
	}

	@Override
	public ZthesSerializingExporter createInstance(ZthesSerializingExporterConfiguration conf) {
		return new ZthesSerializingExporter(conf);
	}
	
	@Override
	public Collection<ZthesSerializingExporterConfiguration> getConfigurations() {
		return Arrays.asList(new ZthesSerializingExporterConfiguration());
	}

	@Override
	public List<DataFormat> getFormats() {
		return Arrays.asList(new DataFormat("XML", "application/xml", "xml"));
	}

}
