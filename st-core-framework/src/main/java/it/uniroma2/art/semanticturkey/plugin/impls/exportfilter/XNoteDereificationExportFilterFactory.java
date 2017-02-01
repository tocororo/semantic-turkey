package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.XNoteDereificationExportFilterConfiguration;

/**
 * Factory for the instantiation of {@link XNoteDereificationExportFilter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XNoteDereificationExportFilterFactory
		implements PluginFactory<XNoteDereificationExportFilterConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration> asList(new XNoteDereificationExportFilterConfiguration());
	}

	@Override
	public XNoteDereificationExportFilterConfiguration createDefaultPluginConfiguration() {
		return new XNoteDereificationExportFilterConfiguration();
	}

	@Override
	public XNoteDereificationExportFilterConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!XNoteDereificationExportFilterConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (XNoteDereificationExportFilterConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public XNoteDereificationExportFilter createInstance(PluginConfiguration config) {
		return new XNoteDereificationExportFilter((XNoteDereificationExportFilterConfiguration) config);
	}

}