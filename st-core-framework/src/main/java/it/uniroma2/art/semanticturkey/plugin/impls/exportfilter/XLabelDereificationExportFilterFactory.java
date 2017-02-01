package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.XLabelDereificationExportFilterConfiguration;

/**
 * Factory for the instantiation of {@link XLabelDereificationExportFilter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XLabelDereificationExportFilterFactory
		implements PluginFactory<XLabelDereificationExportFilterConfiguration> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration> asList(new XLabelDereificationExportFilterConfiguration());
	}

	@Override
	public XLabelDereificationExportFilterConfiguration createDefaultPluginConfiguration() {
		return new XLabelDereificationExportFilterConfiguration();
	}

	@Override
	public XLabelDereificationExportFilterConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!XLabelDereificationExportFilterConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (XLabelDereificationExportFilterConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public XLabelDereificationExportFilter createInstance(PluginConfiguration config) {
		return new XLabelDereificationExportFilter((XLabelDereificationExportFilterConfiguration) config);
	}

}