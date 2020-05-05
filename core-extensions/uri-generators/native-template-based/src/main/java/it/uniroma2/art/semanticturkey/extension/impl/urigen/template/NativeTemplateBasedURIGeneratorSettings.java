package it.uniroma2.art.semanticturkey.extension.impl.urigen.template;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;

/**
 * Configuration class for {@link NativeTemplateBasedURIGeneratorFactory}.
 */
public class NativeTemplateBasedURIGeneratorSettings implements Settings {

//	private Properties props;
//	private Properties propDescriptions;
	
	public NativeTemplateBasedURIGeneratorSettings() {
//		this.props = new Properties(TemplateBasedRandomIdGenerator.defaultPropertyValues);
//		this.propDescriptions = new Properties(TemplateBasedRandomIdGenerator.defaultPropertyDescriptions);
	}

	@Override
	public String getShortName() {
		return "Native template-based";
	}
/*
	@Override
	public Collection<String> getProperties() {
		return props.stringPropertyNames();
	}
	
	@Override
	public String getPropertyDescription(String id) throws PropertyNotFoundException {
		return propDescriptions.getProperty(id);
	}
	
	@Override
	public String getPropertyContentType(String id) throws PropertyNotFoundException {
		return null;
	}
	
	@Override
	public Object getPropertyValue(String id) throws PropertyNotFoundException {
		return props.getProperty(id);
	}
	
	@Override
	public void setPropertyValue(String id, Object value) throws WrongPropertiesException {
		if (!(value instanceof String)) throw new WrongPropertiesException("Object: " + value + " is not of type java.lang.String");
		
		props.setProperty(id, (String)value);
	}
	
	@Override
	public boolean hasRequiredProperties() {
		return false;
	}
	
	@Override
	public boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		return false;
	}

	public Properties getBackingProperties() {
		return props;
	}
*/	
	
}
