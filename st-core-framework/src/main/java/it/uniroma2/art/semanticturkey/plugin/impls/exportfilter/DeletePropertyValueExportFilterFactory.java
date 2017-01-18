package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.DeletePropertyValueExportFilterConfiguration;

/**
 * Factory for the instantiation of {@link DeletePropertyValueExportFilter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DeletePropertyValueExportFilterFactory
		implements PluginFactory<DeletePropertyValueExportFilterConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration> asList(new DeletePropertyValueExportFilterConfiguration());
	}

	@Override
	public DeletePropertyValueExportFilterConfiguration createDefaultPluginConfiguration() {
		return new DeletePropertyValueExportFilterConfiguration();
	}

	@Override
	public DeletePropertyValueExportFilterConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!DeletePropertyValueExportFilterConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (DeletePropertyValueExportFilterConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public DeletePropertyValueExportFilter createInstance(PluginConfiguration config) {
		return new DeletePropertyValueExportFilter((DeletePropertyValueExportFilterConfiguration) config);
	}

}
