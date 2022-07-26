package it.uniroma2.art.semanticturkey.services.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Target(value = {ElementType.METHOD})
public @interface Produces {

    String[] value();

}
