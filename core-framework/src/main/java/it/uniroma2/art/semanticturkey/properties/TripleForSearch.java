package it.uniroma2.art.semanticturkey.properties;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A triple used for search.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class TripleForSearch<P, S, M> {

	private final P predicate;
	private final S searchString;
	private final M mode;

	public TripleForSearch(@JsonProperty("predicate") P predicate, @JsonProperty("searchString") S searchString,
			@JsonProperty("mode") M mode) {
		this.predicate = predicate;
		this.searchString = searchString;
		this.mode = mode;
	}

	public P getPredicate() {
		return predicate;
	}

	public S getSearchString() {
		return searchString;
	}
	
	public M getMode() {
		return mode;
	}

}
