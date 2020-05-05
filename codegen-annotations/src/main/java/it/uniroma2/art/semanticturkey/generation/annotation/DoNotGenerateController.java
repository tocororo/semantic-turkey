package it.uniroma2.art.semanticturkey.generation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * this annotation is used to inform the {@link STServiceProcessor} not to generate a controller for the
 * service annotated with it
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
 * 
 */
@Retention(value = RetentionPolicy.SOURCE)
@Target(value = { ElementType.TYPE })
public @interface DoNotGenerateController {
}