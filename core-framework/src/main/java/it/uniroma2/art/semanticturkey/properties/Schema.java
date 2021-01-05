package it.uniroma2.art.semanticturkey.properties;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;

/**
 * Annotate an {@link STProperties} property to a schema provider.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it"></a>
 *
 */
@Documented
@Target(ElementType.TYPE_USE)
@Retention(RUNTIME)
public @interface Schema {
	Class<? extends SystemSettingsManager<STPropertiesSchema>> settingsManager();
}
