package it.uniroma2.art.semanticturkey.properties;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A simple pair.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class Pair<S, T> {

	private final S first;
	private final T second;

	public Pair(@JsonProperty("first") S first, @JsonProperty("second") T second) {
		this.first = first;
		this.second = second;
	}

	public S getFirst() {
		return first;
	}

	public T getSecond() {
		return second;
	}

}
