package it.uniroma2.art.semanticturkey.extension.impl.urigen.template;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Properties;

import it.uniroma2.art.coda.converters.impl.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties.AnnotatedTypeBuilder;

/**
 * Configuration class for {@link NativeTemplateBasedURIGeneratorFactory}.
 */
public class NativeTemplateBasedURIGeneratorConfiguration implements Configuration {

	public static final AnnotatedType STRING_ANNOTATED_TYPE = new AnnotatedTypeBuilder()
			.withType(String.class).build();

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGeneratorConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}

	private Properties props;
	private Properties propDescriptions;

	public NativeTemplateBasedURIGeneratorConfiguration() {
		this.props = new Properties(TemplateBasedRandomIdGenerator.defaultPropertyValues);
		this.propDescriptions = new Properties(TemplateBasedRandomIdGenerator.defaultPropertyDescriptions);
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@Override
	public Collection<String> getProperties() {
		return props.stringPropertyNames();
	}

	@Override
	public AnnotatedType getPropertyAnnotatedType(String id) throws PropertyNotFoundException {
		return STRING_ANNOTATED_TYPE;
	}

	@Override
	public String getPropertyDescription(String id) throws PropertyNotFoundException {
		return propDescriptions.getProperty(id);
	}

	@Override
	public String getPropertyDisplayName(String id) throws PropertyNotFoundException {
		return id;
	}

	@Override
	public String getPropertyContentType(String id) throws PropertyNotFoundException {
		return null;
	}

	@Override
	public boolean isEnumerated(String id) throws PropertyNotFoundException {
		return false;
	}

	@Override
	public Annotation[] getAnnotations(String id) throws PropertyNotFoundException {
		return new Annotation[0];
	}

	@Override
	public Object getPropertyValue(String id) throws PropertyNotFoundException {
		return props.getProperty(id);
	}

	@Override
	public void setPropertyValue(String id, Object value) throws WrongPropertiesException {
		if (!(value instanceof String))
			throw new WrongPropertiesException("Object: " + value + " is not of type java.lang.String");

		props.setProperty(id, (String) value);
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

}
