package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.List;
import java.util.function.Function;

@Documented
@Retention(RUNTIME)
public @interface SearchFacetProcessor {
	String joinUsingDelimiter() default "";

	Class<? extends Function<List<String>, String>> aggregateUsing() default NullProcessor.class;

	public static class NullProcessor implements Function<List<String>, String> {

		@Override
		public String apply(List<String> t) {
			throw new UnsupportedOperationException();
		}

	}
}
