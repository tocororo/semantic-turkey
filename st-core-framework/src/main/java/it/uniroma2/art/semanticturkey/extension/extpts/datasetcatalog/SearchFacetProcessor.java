package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

@Documented
@Retention(RUNTIME)
public @interface SearchFacetProcessor {
	String joinUsingDelimiter() default "";
}
