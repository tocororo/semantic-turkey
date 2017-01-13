package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;

import it.uniroma2.art.semanticturkey.services.core.PluginSpecification;

/**
 * Converts a string to a {@link PluginSpecification}, containing a plugin factoryId and its configuration
 * parameter
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StringToPluginSpecificationConverter implements Converter<String, PluginSpecification> {

	private static final Pattern specPattern = Pattern
			.compile("^([a-zA-Z0-9_\\.]+)(?:\\|\\|([a-zA-Z0-9_\\.]+))?(?:\\|\\|(.*))$");

	@Override
	public PluginSpecification convert(String source) {
		Matcher matcher = specPattern.matcher(source);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Illegal plugin specification");
		}

		String factoryId = matcher.group(1);
		String configType = matcher.group(2);
		String serializedProperties = matcher.group(3);
		Properties properties = new Properties();
		if (serializedProperties != null) {
			try {
				properties.load(new StringReader(serializedProperties));
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return new PluginSpecification(factoryId, configType, properties);
	}

}
