package it.uniroma2.art.semanticturkey.utilities;

import java.util.Optional;

public class OptionalUtils {
	@SafeVarargs
	public static <T> Optional<T> firstPresent(Optional<T>... optionals) {
		if (optionals.length == 0) {
			return Optional.empty();
		}

		for (Optional<T> opt : optionals) {
			if (opt.isPresent()) {
				return opt;
			}
		}

		return Optional.empty();
	}
}
