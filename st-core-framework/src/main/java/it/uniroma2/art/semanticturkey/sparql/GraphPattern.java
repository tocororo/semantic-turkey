package it.uniroma2.art.semanticturkey.sparql;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;

public class GraphPattern {
	private final Map<String, IRI> prefixMapping;
	private final String pattern;
	private final ProjectionElement projectionElement;

	private static final Pattern URIorQNAME_PATTERN = Pattern.compile("(\\<.*?\\>)|([a-zA-Z]+):([a-zA-Z]*)");
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\?([a-zA-Z_]+)");

	public GraphPattern(Map<String, IRI> prefixMapping, ProjectionElement projectionElement, String pattern) {
		this.prefixMapping = prefixMapping;
		this.projectionElement = projectionElement;
		this.pattern = pattern;
	}

	public ProjectionElement getProjectionElement() {
		return projectionElement;
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
					matcher.appendReplacement(sb, prefix + ":" + localName);
				}

				if (namespace == null) {
					throw new IllegalStateException("Prefix '" + prefix + "' is not bound to a namespace");
				}

				substitution = NTriplesUtil
						.toNTriplesString(vf.createIRI(namespace.stringValue(), localName));
			}
			matcher.appendReplacement(sb, substitution);
		}

		matcher.appendTail(sb);

		return sb.toString();
	}

	public GraphPattern renamed(Function<String, String> renamingFunction,
			BiMap<String, String> projected2baseVariableMapping) {
		Matcher matcher = VARIABLE_PATTERN.matcher(pattern);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String variableName = matcher.group(1);
			String newVariableName = projected2baseVariableMapping.inverse().get(variableName);

			if (newVariableName == null) {
				newVariableName = renamingFunction.apply(variableName);
				projected2baseVariableMapping.put(newVariableName, variableName);
			}
			matcher.appendReplacement(sb, "?" + newVariableName);
		}
		matcher.appendTail(sb);

		return new GraphPattern(prefixMapping,
				projectionElement.renamed(renamingFunction, projected2baseVariableMapping), sb.toString());
	}
}
