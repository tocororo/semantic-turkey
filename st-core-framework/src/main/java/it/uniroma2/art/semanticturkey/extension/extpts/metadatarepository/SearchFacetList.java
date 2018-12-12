package it.uniroma2.art.semanticturkey.extension.extpts.metadatarepository;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface SearchFacetList {
	SearchFacet[] value();
}
