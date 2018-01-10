package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.UpdatePropertyValueExportFilterConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * Factory for the instantiation of {@link UpdatePropertyValueExportFilter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueExportFilterFactory implements
		PluginFactory<UpdatePropertyValueExportFilterConfiguration, STProperties, STProperties, STProperties, STProperties> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<STProperties> getPluginConfigurations() {
		return Arrays.<STProperties>asList(new UpdatePropertyValueExportFilterConfiguration());
	}

	@Override
	public UpdatePropertyValueExportFilterConfiguration createDefaultPluginConfiguration() {
		return new UpdatePropertyValueExportFilterConfiguration();
	}

	@Override
	public UpdatePropertyValueExportFilterConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!UpdatePropertyValueExportFilterConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (UpdatePropertyValueExportFilterConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public UpdatePropertyValueExportFilter createInstance(STProperties config) {
		return new UpdatePropertyValueExportFilter((UpdatePropertyValueExportFilterConfiguration) config);
	}

}
