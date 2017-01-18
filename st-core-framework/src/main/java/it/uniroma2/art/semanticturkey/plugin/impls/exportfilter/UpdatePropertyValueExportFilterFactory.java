package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.DeletePropertyValueExportFilterConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.UpdatePropertyValueExportFilterConfiguration;

/**
 * Factory for the instantiation of {@link UpdatePropertyValueExportFilter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueExportFilterFactory
		implements PluginFactory<UpdatePropertyValueExportFilterConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration> asList(new UpdatePropertyValueExportFilterConfiguration());
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
	public UpdatePropertyValueExportFilter createInstance(PluginConfiguration config) {
		return new UpdatePropertyValueExportFilter((UpdatePropertyValueExportFilterConfiguration) config);
	}

}
