package it.uniroma2.art.semanticturkey.extension.settings.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.UserSettingsManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

public abstract class SettingsSupport {
	public static <T extends Settings> Class<T> getSettingsClass(SettingsManager settingsManager, Scope scope) {
		Class<?> managerTarget;

		switch (scope) {
		case SYSTEM:
			managerTarget = SystemSettingsManager.class;
			break;
		case PROJECT:
			managerTarget = ProjectSettingsManager.class;
			break;
		case USER:
			managerTarget = UserSettingsManager.class;
			break;
		case PROJECT_USER:
			managerTarget = PUSettingsManager.class;
			break;
		default:
			throw new IllegalArgumentException("Unrecognized scope: " + scope); // it should not happen
		}

		return ReflectionUtilities.getInterfaceArgumentTypeAsClass(settingsManager.getClass(), managerTarget,
				0);
	}

	public static Settings createSettings(SettingsManager settingsManager, Scope scope, ObjectNode settings)
			throws WrongPropertiesException, STPropertyAccessException {
		Class<Settings> settingsClass = getSettingsClass(settingsManager, scope);
		return STPropertiesManager.loadSTPropertiesFromObjectNode(settingsClass, false, settings);
	}

}
