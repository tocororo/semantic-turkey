package it.uniroma2.art.semanticturkey.sparql;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * A parser performing a shallow analysis of a SPARQL query to construct a modifiable representation (see
 * {@link TupleQueryShallowModel}).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLShallowParser {
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\?([a-zA-Z_]+)");

	private static final SPARQLShallowParser instance = new SPARQLShallowParser();

	public static SPARQLShallowParser getInstance() {
		return instance;
	}

	public TupleQueryShallowModel parseTupleQuery(String query) throws SPARQLShallowParserException {
		int indexOfQueryPatternEnd = query.lastIndexOf("}");

		if (indexOfQueryPatternEnd == -1) {
			throw new SPARQLShallowParserException("Could not determine the end of query pattern");
		}

		int indexOfSelectKeyword = query.indexOf("SELECT");

		if (indexOfSelectKeyword == -1) {
			throw new SPARQLShallowParserException("Could not determine the position of SELECT");
		}

		int indexOfWhereKeyword = query.indexOf("WHERE", indexOfSelectKeyword);

		if (indexOfWhereKeyword == -1) {
			throw new SPARQLShallowParserException("Could not determine the position of WHERE");
		}

		Matcher variableMatcher = VARIABLE_PATTERN
				.matcher(query.substring(indexOfSelectKeyword, indexOfWhereKeyword));
		List<String> initialQueryVariables = new ArrayList<>();
		while (variableMatcher.find()) {
			initialQueryVariables.add(variableMatcher.group(1));
		}
		int indexOfGroupByKeyword = query.lastIndexOf("GROUP BY");

		if (indexOfGroupByKeyword != -1) {
			indexOfGroupByKeyword = IntStream
					.of(query.indexOf("HAVING", indexOfGroupByKeyword),
							query.indexOf("ORDER", indexOfGroupByKeyword),
							query.indexOf("LIMIT", indexOfGroupByKeyword))
					.filter(v -> v != -1).min().orElse(query.length());
		}

		return new TupleQueryShallowModel(query, initialQueryVariables, indexOfWhereKeyword,
				indexOfQueryPatternEnd, indexOfGroupByKeyword);
	}
}
