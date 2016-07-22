package it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf;

import java.util.Collection;
import java.util.Properties;

import it.uniroma2.art.coda.converters.impl.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.ConfParameterNotFoundException;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;

/**
 * Configuration class for {@link NativeTemplateBasedURIGeneratorFactory}.
 */
public class NativeTemplateBasedURIGeneratorConfiguration extends AbstractPluginConfiguration {

	private Properties props;
	private Properties propDescriptions;
	
	public NativeTemplateBasedURIGeneratorConfiguration() {
		this.props = new Properties(TemplateBasedRandomIdGenerator.defaultPropertyValues);
		this.propDescriptions = new Properties(TemplateBasedRandomIdGenerator.defaultPropertyDescriptions);
	}

	@Override
	public String getShortName() {
		return "Native template-based";
	}

	@Override
	public Collection<String> getConfigurationParameters() {
		return props.stringPropertyNames();
	}
	
	@Override
	public String getParameterDescription(String id) throws ConfParameterNotFoundException {
		return propDescriptions.getProperty(id);
	}
	
	@Override
	public String getParameterContentType(String parID) throws ConfParameterNotFoundException {
		return null;
	}
	
	@Override
	public Object getParameterValue(String id) throws ConfParameterNotFoundException {
		return props.getProperty(id);
	}
	
	@Override
	public void setParameter(String id, Object value) throws BadConfigurationException {
		if (!(value instanceof String)) throw new BadConfigurationException("Object: " + value + " is not of type java.lang.String");
		
		props.setProperty(id, (String)value);
	}
	
	@Override
	public boolean hasRequiredParameters() {
		return false;
	}
	
	@Override
	public boolean isRequiredParameter(String parID) throws ConfParameterNotFoundException {
		return false;
	}

	public Properties getProperties() {
		return props;
	}
}
