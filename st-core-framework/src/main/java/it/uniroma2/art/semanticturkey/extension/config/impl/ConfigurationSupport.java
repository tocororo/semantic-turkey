package it.uniroma2.art.semanticturkey.extension.config.impl;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationReflectionException;
import it.uniroma2.art.semanticturkey.extension.config.PUConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.ProjectConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.SystemConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.UserConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

public abstract class ConfigurationSupport {

	public static final String CONFIG_TYPE_PARAM = "@type";

	public static File getConfigurationFolder(SystemConfigurationManager<?> manager) {
		return STPropertiesManager.getSystemPropertyFolder(manager.getId());
	}

	public static File getConfigurationFolder(ProjectConfigurationManager<?> manager, Project project) {
		return STPropertiesManager.getProjectPropertyFolder(project, manager.getId());
	}

	public static File getConfigurationFolder(UserConfigurationManager<?> manager, STUser user) {
		return STPropertiesManager.getUserPropertyFolder(user, manager.getId());
	}

	public static File getConfigurationFolder(PUConfigurationManager<?> manager, Project project,
			STUser user) {
		return STPropertiesManager.getPUBindingPropertyFolder(project, user, manager.getId());
	}

	public static Collection<String> listConfigurationIdentifiers(File folder) {
		return Arrays.stream(folder.list(new WildcardFileFilter("*.cfg"))).collect(toList());
	}

	public static <CONFTYPE extends Configuration> CONFTYPE loadConfiguration(
			ConfigurationManager<?> systemConfigurationManager, File folder, String identifier)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException {
		File configFile = new File(folder, configurationFilename(identifier));
		Properties props = new Properties();
		try (InputStream is = new FileInputStream(configFile)) {
			props.load(is);
		} catch (FileNotFoundException e) {
			throw new ConfigurationNotFoundException(
					String.format("Unable to find system configuration %s for component %s", identifier,
							systemConfigurationManager.getId()));
		}

		@Nullable
		String configType = props.getProperty(CONFIG_TYPE_PARAM);
		if (configType != null) {
			props.remove(CONFIG_TYPE_PARAM);
		}

		Class<?> configClass = getConfigurationClass(systemConfigurationManager, configType);

		try {
			Configuration configObj = (Configuration) configClass.newInstance();
			configObj.setProperties(props);
			@SuppressWarnings("unchecked")
			CONFTYPE castedConfigObj = (CONFTYPE) configObj;
			return castedConfigObj;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ConfigurationReflectionException(e);
		}

	}

	protected static Class<?> getConfigurationClass(ConfigurationManager<?> systemConfigurationManager,
			@Nullable String configType) throws ConfigurationReflectionException {
		Class<?> configClass;
		Class<?> baseConfigurationClass = ReflectionUtilities.getInterfaceArgumentTypeAsClass(
				systemConfigurationManager.getClass(), SystemConfigurationManager.class, 0);

		if (configType == null) {
			configClass = baseConfigurationClass;
		} else {
			try {
				configClass = systemConfigurationManager.getClass().getClassLoader().loadClass(configType);
				if (!baseConfigurationClass.isAssignableFrom(configClass)) {
					throw new ConfigurationReflectionException(
							"Invalid configuration class: " + configClass.getName());
				}

			} catch (ClassNotFoundException e) {
				throw new ConfigurationReflectionException(e);
			}
		}

		if (!Configuration.class.isAssignableFrom(configClass)) {
			throw new ConfigurationReflectionException("Not a Configuration class: " + configClass.getName());
		}
		return configClass;
	}

	private static String configurationFilename(String identifier) {
		return identifier + ".cfg";
	}

	public static <CONFTYPE extends Configuration> void storeConfiguration(File folder, String identifier,
			CONFTYPE configuration) throws IOException, WrongPropertiesException {
		Properties props = new Properties();
		configuration.storeProperties(props);
		props.setProperty(CONFIG_TYPE_PARAM, configuration.getClass().getName());
		File configFile = new File(folder, configurationFilename(identifier));
		try (OutputStream out = new FileOutputStream(configFile)) {
			props.store(out, null);
		}
	}

	public static Configuration createConfiguration(ConfigurationManager<?> configurationManager,
			Map<String, Object> properties) throws WrongPropertiesException {
		@Nullable
		String configType = Optional.ofNullable(properties.get(CONFIG_TYPE_PARAM)).map(Object::toString)
				.orElse(null);
		if (configType != null) {
			properties.remove(CONFIG_TYPE_PARAM);
		}
		Class<?> configClass = getConfigurationClass(configurationManager, configType);
		Configuration config;
		try {
			config = (Configuration) configClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ConfigurationReflectionException(e);
		}
		Properties props = new Properties();
		properties.forEach((k, v) -> {
			if (v != null)
				props.setProperty(k, v.toString());
		});
		config.setProperties(props);
		return config;
	}

}
