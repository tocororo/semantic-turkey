package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.AbstractPluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.DatasetMetadataExporterConfiguration;

/**
 * Factory for the instantiation of {@link DCATDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DCATDatasetMetadataExporterFactory extends
		AbstractPluginFactory<DatasetMetadataExporterConfiguration, DatasetMetadataExporterSettings, DCATDatasetMetadataExporterSettings>
		implements
		PluginFactory<DatasetMetadataExporterConfiguration, DatasetMetadataExporterSettings, DCATDatasetMetadataExporterSettings> {

	
	public DCATDatasetMetadataExporterFactory() {
		super(DatasetMetadataExporter.class.getName());
	}

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
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
	public Object createInstance(PluginConfiguration conf) {
		return new DCATDatasetMetadataExporter(this);
	}

	@Override
	protected DCATDatasetMetadataExporterSettings buildProjectSettingsInternal() {
		return new DCATDatasetMetadataExporterSettings();
	}

	@Override
	protected DatasetMetadataExporterSettings buildExtensionPointProjectSettingsInternal() {
		return new DatasetMetadataExporterSettings();
	}
}
