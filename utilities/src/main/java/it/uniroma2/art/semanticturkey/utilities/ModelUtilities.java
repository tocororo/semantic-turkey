package it.uniroma2.art.semanticturkey.utilities;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.axis.types.NCName;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Namespaces;

import com.github.jsonldjava.shaded.com.google.common.collect.Maps;

public class ModelUtilities {

	/**
	 * given namespace <code>namespace</code>, this tries to automatically suggest a prefix for it
	 * 
	 * @param namespace
	 * @return the guessed prefix, or null if the guess was not possible
	 */
	public static String guessPrefix(String namespace) {
		int lowerCutIndex;
		String tempString;

		if (namespace.endsWith("/") || namespace.endsWith("#"))
			tempString = namespace.substring(0, namespace.length() - 1);
		else
			tempString = namespace;

		if (tempString.matches(".*\\.(\\w{2}|\\w{3})"))
			tempString = tempString.substring(0, tempString.lastIndexOf("."));

		int pointLowerCutIndex = tempString.lastIndexOf(".");
		int slashLowerCutIndex = tempString.lastIndexOf("/");
		if (pointLowerCutIndex > slashLowerCutIndex)
			lowerCutIndex = pointLowerCutIndex;
		else
			lowerCutIndex = slashLowerCutIndex;

		String prefix = tempString.substring(lowerCutIndex + 1);
		return isPrefixSyntValid(prefix) ? prefix : null;
	}

	/**
	 * given the candidate prefix <code>prefix</code>, return true it is syntactically valid, false otherwise
	 * 
	 * @param prefix
	 *            the prefix to be checked if it is syntactically valid
	 * @return true if the prefix is syntactically valid, false otherwise
	 */
	public static boolean isPrefixSyntValid(String prefix) {
		return NCName.isValid(prefix);
	}

	/**
	 * given the baseuri of an ontology, returns the baseuri
	 * 
	 * @param baseuri
	 * @return
	 */
	public static String createDefaultNamespaceFromBaseURI(String baseuri) {
		if (baseuri.endsWith("/") || baseuri.endsWith("#"))
			return baseuri;
		else
			return baseuri + "#";
	}

	/**
	 * Extends the given namespace {@code ns} with the supplied string {@code ext}.
	 * 
	 * @param ns
	 * @param ext
	 * @return
	 */
	public static String extendNamespace(String ns, String ext) {
		if (ns.endsWith("#")) {
			return ns.substring(0, ns.length() - 1) + "/" + ext;
		} else if (ns.endsWith("/")) {
			return ns + ext;
		} else {
			return ns + "/" + ext;
		}
	}

	/**
	 * Converts an RDF4J {@link Value} to a {@link Literal}. This is a convenience overload of
	 * {@link #toLiteral(Value, Map, ValueFactory)} that uses {@link SimpleValueFactory}.
	 * 
	 * @param value
	 * @param prefixDeclarations
	 * @return
	 */
	public static Literal toLiteral(Value value, Map<String, String> prefixDeclarations) {
		return toLiteral(value, prefixDeclarations, SimpleValueFactory.getInstance());
	}

	/**
	 * Converts an RDF4J {@link Value} to a {@link Literal}.This is a convenience overload of
	 * {@link #toLiteral(Value, Map, ValueFactory)} that uses no prefix declaration and a
	 * {@link SimpleValueFactory}.
	 * 
	 * @param value
	 * @param prefixDeclarations
	 * @param vf
	 * @return
	 */
	public static Literal toLiteral(Value value) {
		return toLiteral(value, Collections.emptyMap(), SimpleValueFactory.getInstance());
	}

	/**
	 * Converts an RDF4J {@link Value} to a {@link Literal}. If the argument is a kind of {@link Resource},
	 * the {@link Resource#toString()} is used as label of the produced literal, possibly shortening URIs with
	 * the supplied prefix declarations.
	 * 
	 * @param value
	 * @param prefixDeclarations
	 * @param vf
	 * @return
	 */
	public static Literal toLiteral(Value value, Map<String, String> prefixDeclarations, ValueFactory vf) {
		if (value instanceof Literal) {
			return (Literal) value;
		} else {
			String label;
			if (value instanceof IRI) {
				label = getQName((IRI) value, prefixDeclarations);
			} else {
				label = value.stringValue();
			}
			return vf.createLiteral(label);
		}
	}

	/**
	 * Returns a qname representing the supplied IRI. If no prefix declaration applies, then the original IRI
	 * is returned.
	 * 
	 * @param input
	 * @param prefixDeclarations
	 * @return
	 */
	public static String getQName(IRI input, Map<String, String> prefixDeclarations) {
		return prefixDeclarations.entrySet().stream()
				.filter(e -> Objects.equals(input.getNamespace(), e.getValue())).map(Map.Entry::getKey)
				.map(prefix -> prefix + ":" + input.getLocalName()).findAny().orElse(input.stringValue());
	}
}
