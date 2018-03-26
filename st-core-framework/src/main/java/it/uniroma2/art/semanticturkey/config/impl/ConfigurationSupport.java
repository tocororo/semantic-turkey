package it.uniroma2.art.semanticturkey.config.impl;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.ConfigurationReflectionException;
import it.uniroma2.art.semanticturkey.config.PUConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ProjectConfigurationManager;
import it.uniroma2.art.semanticturkey.config.SystemConfigurationManager;
import it.uniroma2.art.semanticturkey.config.UserConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
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
		return Arrays.stream(folder.list(new WildcardFileFilter("*.cfg")))
				.map(name -> name.substring(0, name.length() - 4 /* .cfg length */)).collect(toList());
	}

	public static <CONFTYPE extends Configuration> CONFTYPE loadConfiguration(
			ConfigurationManager<CONFTYPE> configurationManager, File folder, String identifier)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException,
			STPropertyAccessException {
		File configFile = new File(folder, configurationFilename(identifier));

		Class<CONFTYPE> configBaseClass = ReflectionUtilities.getInterfaceArgumentTypeAsClass(
				configurationManager.getClass(), ConfigurationManager.class, 0);

		return STPropertiesManager.loadSTPropertiesFromYAMLFiles(configBaseClass, true, configFile);
	}

	public static Class<?> getConfigurationClass(ConfigurationManager<?> systemConfigurationManager,
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
			CONFTYPE configuration) throws IOException, WrongPropertiesException, STPropertyUpdateException {
		STPropertiesChecker checker = STPropertiesChecker.getModelConfigurationChecker(configuration);

		if (!checker.isValid()) {
			throw new STPropertyUpdateException("Invalid configuration: " + checker.getErrorMessage());
		}
		File configFile = new File(folder, configurationFilename(identifier));
		STPropertiesManager.storeSTPropertiesInYAML(configuration, configFile, true);
	}

	public static void deleteConfiguration(File folder, String identifier)
			throws ConfigurationNotFoundException {
		File configFile = new File(folder, configurationFilename(identifier));
		if (!configFile.exists())
			throw new ConfigurationNotFoundException("Unable to find configuration: " + identifier);
		configFile.delete();
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
