package it.uniroma2.art.semanticturkey.sparql;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Value;

public abstract class SPARQLUtilities {
	private static Pattern variablePattern = Pattern.compile("\\?([a-zA-Z]+)");
	
	public static Value java2node(Object object) {
		if (object instanceof Value) {
			return (Value)object;
		} else {
			throw new IllegalArgumentException("Could not convert java object to RDF value");
		}
	}

	public static Set<String> getVariables(String queryString) {
		Matcher matcher = variablePattern.matcher(queryString);
		
		Set<String> variables = new HashSet<>();
		
		while (matcher.find()) {
			String variableName = matcher.group(1);
			variables.add(variableName);
		}
		
		return variables;
	}

}
