package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Repeatable(SearchFacetList.class)
public @interface SearchFacet {
	String name();
	String description();
	boolean allowsMultipleValues();
	SearchFacetProcessor processedUsing();
}
