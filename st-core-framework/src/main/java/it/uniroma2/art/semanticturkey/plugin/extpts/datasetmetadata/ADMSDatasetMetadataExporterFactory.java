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
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;

/**
 * Factory for the instantiation of {@link ADMSDatasetMetadataExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ADMSDatasetMetadataExporterFactory extends
		AbstractPluginFactory<DatasetMetadataExporterConfiguration, DatasetMetadataExporterSettings, ADMSDatasetMetadataExporterSettings, STProperties, STProperties>
		implements
		PluginFactory<DatasetMetadataExporterConfiguration, DatasetMetadataExporterSettings, ADMSDatasetMetadataExporterSettings, STProperties, STProperties> {

	public ADMSDatasetMetadataExporterFactory() {
		super(DatasetMetadataExporter.class.getName());
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
		return new ADMSDatasetMetadataExporter(this);
	}

	@Override
	protected ADMSDatasetMetadataExporterSettings buildProjectSettingsInternal() {
		return new ADMSDatasetMetadataExporterSettings();
	}

	@Override
	protected DatasetMetadataExporterSettings buildExtensionPointProjectSettingsInternal() {
		return new DatasetMetadataExporterSettings();
	}
	
	@Override
	protected STProperties buildProjectPreferencesInternal() {
		return new STPropertiesImpl() {
			
			@Override
			public String getShortName() {
				return "empty";
			}
		};
	}
	
	@Override
	protected STProperties buildExtensionPointProjectPreferencesInternal() {
		return new STPropertiesImpl() {
			
			@Override
			public String getShortName() {
				return "empty";
			}
		};
	}
}
