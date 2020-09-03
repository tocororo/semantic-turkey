package it.uniroma2.art.semanticturkey.sparql;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * Represents a graph pattern that can be used to construct a SPARQL query.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class GraphPattern {
	private final Map<String, IRI> prefixMapping;
	private final String pattern;
	private final List<ProjectionElement> projection;

	private static final Pattern URIorQNAME_PATTERN = Pattern.compile("(\\<.*?\\>)|([a-zA-Z]+):([a-zA-Z]*)");
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\?([a-zA-Z_]+)");

	public GraphPattern(Map<String, IRI> prefixMapping, List<ProjectionElement> projection, String pattern) {
		this.prefixMapping = prefixMapping;
		this.projection = projection;
		this.pattern = pattern;
	}

	public List<ProjectionElement> getProjection() {
		return projection;
	}

	public String getSPARQLPattern() throws IllegalStateException {
		Matcher matcher = URIorQNAME_PATTERN.matcher(pattern);
		StringBuffer sb = new StringBuffer();
		ValueFactory vf = SimpleValueFactory.getInstance();

		while (matcher.find()) {
			String uriTurtle = matcher.group(1);

			String substitution;

			if (uriTurtle != null) {
				substitution = uriTurtle;
			} else {
				String prefix = matcher.group(2);
				String localName = matcher.group(3);

				IRI namespace = prefixMapping.get(prefix);

				if (prefix.equals("http")) {
					substitution = matcher.group(0);
				} else {

					if (namespace == null) {
						throw new IllegalStateException(
								"Prefix '" + prefix + "' is not bound to a namespace");
					}

					substitution = NTriplesUtil
							.toNTriplesString(vf.createIRI(namespace.stringValue(), localName));
				}
			}
			matcher.appendReplacement(sb, substitution);
		}

		matcher.appendTail(sb);

		return sb.toString();
	}

	public GraphPattern renamed(Function<String, String> renamingFunction,
			BiMap<String, String> variableSubstitutionMapping) {
		Matcher matcher = VARIABLE_PATTERN.matcher(pattern);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String variableName = matcher.group(1);
			String newVariableName = variableSubstitutionMapping.get(variableName);

			if (newVariableName == null) {
				newVariableName = renamingFunction.apply(variableName);
				variableSubstitutionMapping.put(variableName, newVariableName);
			}
			matcher.appendReplacement(sb, "?" + newVariableName);
		}
		matcher.appendTail(sb);

		return new GraphPattern(prefixMapping, projection.stream()
				.map(el -> el.renamed(renamingFunction, variableSubstitutionMapping)).collect(toList()),
				sb.toString());
	}
}
