package it.uniroma2.art.semanticturkey.extension.settings.impl;

import javax.annotation.Nullable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
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
		BundleContext bundleContext = FrameworkUtil.getBundle(SettingsSupport.class).getBundleContext();
		@Nullable
		ServiceReference sr = bundleContext.getServiceReference(ExtensionPointManager.class.getName());
		@Nullable
		ExtensionPointManager exptManager;
		if (sr != null) {
			exptManager = (ExtensionPointManager) bundleContext.getService(sr);
		} else {
			exptManager = null;
		}
		Class<Settings> settingsClass = getSettingsClass(settingsManager, scope);
		try {
			return STPropertiesManager.loadSTPropertiesFromObjectNode(settingsClass, false, settings,
					STPropertiesManager.createObjectMapper(exptManager));
		} finally {
			bundleContext.ungetService(sr);
		}
	}

}
