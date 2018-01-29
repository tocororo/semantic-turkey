package it.uniroma2.art.semanticturkey.extension.settings;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.reflect.TypeUtils;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface PUSettingsManager<T extends Settings> extends SettingsManager {

	// Collection<String> getProjectSettingsIdentifiers(Project project, STUser user);

	default T getProjectSettings(Project project, STUser user /* , String identifier */)
			throws STPropertyAccessException {
		for (Type t : getClass().getGenericInterfaces()) {
			Map<TypeVariable<?>, Type> typeArgs = TypeUtils.getTypeArguments(t, PUSettingsManager.class);
			if (typeArgs != null) {
				for (Entry<TypeVariable<?>, Type> entry : typeArgs.entrySet()) {
					if (entry.getKey().getGenericDeclaration() == PUSettingsManager.class) {
						try {
							T settings = (T) TypeUtils.getRawType(entry.getValue(), null).newInstance();
							STPropertiesManager.getProjectPreferences(settings, project, user,
									getId());
							return settings;
						} catch (InstantiationException | IllegalAccessException e) {
							throw new STPropertyAccessException(e);
						}

					}
				}
			}
		}

		throw new IllegalStateException("Could not determine the settings type");
	}

	default void storeProjectSettings(Project project, STUser user, /* String identifier, */ T settings)
			throws STPropertyUpdateException {
		STPropertiesManager.setProjectPreferences(settings, project, user, getId(), true);
	}

}
