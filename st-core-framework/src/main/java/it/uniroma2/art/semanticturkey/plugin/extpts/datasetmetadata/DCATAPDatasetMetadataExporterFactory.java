package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.AbstractPluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.DatasetMetadataExporterConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * Factory for the instantiation of {@link DCATAPDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class DCATAPDatasetMetadataExporterFactory extends
		AbstractPluginFactory<DatasetMetadataExporterConfiguration, DatasetMetadataExporterSettings, DCATAPDatasetMetadataExporterSettings>
		implements
		PluginFactory<DatasetMetadataExporterConfiguration, DatasetMetadataExporterSettings, DCATAPDatasetMetadataExporterSettings> {

	
	public DCATAPDatasetMetadataExporterFactory() {
		super(DatasetMetadataExporter.class.getName());
	}

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<STProperties> getPluginConfigurations() {
		return Arrays.asList(new DatasetMetadataExporterConfiguration());
	}

	@Override
	public DatasetMetadataExporterConfiguration createDefaultPluginConfiguration() {
		return new DatasetMetadataExporterConfiguration();
	}

	@Override
	public DatasetMetadataExporterConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!DatasetMetadataExporterConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (DatasetMetadataExporterConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public Object createInstance(STProperties conf) {
		return new DCATAPDatasetMetadataExporter(this);
	}

	@Override
	protected DCATAPDatasetMetadataExporterSettings buildProjectSettingsInternal() {
		return new DCATAPDatasetMetadataExporterSettings();
	}

	@Override
	protected DatasetMetadataExporterSettings buildExtensionPointProjectSettingsInternal() {
		return new DatasetMetadataExporterSettings();
	}
}
