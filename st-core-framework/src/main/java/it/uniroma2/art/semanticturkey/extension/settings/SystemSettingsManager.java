package it.uniroma2.art.semanticturkey.extension.settings;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.reflect.TypeUtils;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;

/**
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface SystemSettingsManager<T extends Settings> extends SettingsManager {

	// Collection<String> getSystemSettingsIdentifiers();

	default T getSystemSettings(/* String identifier */) throws STPropertyAccessException {
		for (Type t : getClass().getGenericInterfaces()) {
			Map<TypeVariable<?>, Type> typeArgs = TypeUtils.getTypeArguments(t, SystemSettingsManager.class);
			if (typeArgs != null) {
				for (Entry<TypeVariable<?>, Type> entry : typeArgs.entrySet()) {
					if (entry.getKey().getGenericDeclaration() == SystemSettingsManager.class) {
						try {
							T settings = (T) TypeUtils.getRawType(entry.getValue(), null).newInstance();
							STPropertiesManager.getSystemSettings(settings, getId());
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

	default void storeSystemSettings(/* String identifier, */ T settings) throws STPropertyUpdateException {
		STPropertiesManager.setSystemSettings(settings, getId(), true);
	}

}
