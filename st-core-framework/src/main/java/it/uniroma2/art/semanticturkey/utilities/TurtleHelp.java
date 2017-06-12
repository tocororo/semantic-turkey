package it.uniroma2.art.semanticturkey.utilities;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

/***
 * A utility class for Turtle
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class TurtleHelp {
	/**
	 * Serializes a list of {@link Value}s according to the Turtle syntax for collections.
	 * 
	 * @param items
	 * @return
	 */
	public static String serializeCollection(List<? extends Value> items) {
		return items.stream().map(SPARQLHelp::toSPARQL).collect(joining(" ", "(", ")"));
	}

	/**
	 * Returns a qname for the provided IRI, if possible; otherwise, returns the NT serialization of the IRI.
	 * 
	 * @param resource
	 * @param ns2prefixMapping
	 * @return
	 */
	public static String toQname(IRI resource, Map<String, String> ns2prefixMapping) {
		String prefix = ns2prefixMapping.get(((IRI) resource).getNamespace());

		if (prefix == null) {
			return NTriplesUtil.toNTriplesString(resource);
		} else {
			return prefix + ":" + ((IRI) resource).getLocalName();
		}
	}

	/**
	 * Serializes a literal, possibly abbreviating the datatype with a qname.
	 * 
	 * @param literal
	 * @param ns2prefixMapping
	 * @return
	 */
	public static String serializeLiteral(Literal literal, Map<String, String> ns2prefixMapping) {
		if (literal.getLanguage().isPresent()) {
			return NTriplesUtil.toNTriplesString(literal);
		} else {
			return "\"" + NTriplesUtil.escapeString(literal.getLabel()) + "\"^^"
					+ toQname(literal.getDatatype(), ns2prefixMapping);
		}
	}
}
