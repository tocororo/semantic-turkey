package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.SPARQLExportFilterConfiguration;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * Factory for the instantiation of {@link SPARQLExportFilterTest}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLExportFilterFactory
		implements PluginFactory<SPARQLExportFilterConfiguration, STProperties, STProperties> {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public Collection<PluginConfiguration> getPluginConfigurations() {
		return Arrays.<PluginConfiguration>asList(new SPARQLExportFilterConfiguration());
	}

	@Override
	public SPARQLExportFilterConfiguration createDefaultPluginConfiguration() {
		return new SPARQLExportFilterConfiguration();
	}

	@Override
	public SPARQLExportFilterConfiguration createPluginConfiguration(String configType)
			throws UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException,
			ClassNotFoundException {
		Class<?> clazz = Class.forName(configType);

		if (!SPARQLExportFilterConfiguration.class.isAssignableFrom(clazz)) {
			throw new UnsupportedPluginConfigurationException();
		}

		try {
			return (SPARQLExportFilterConfiguration) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new UnloadablePluginConfigurationException(e);
		}
	}

	@Override
	public SPARQLExportFilter createInstance(PluginConfiguration config) {
		return new SPARQLExportFilter((SPARQLExportFilterConfiguration) config);
	}

}
