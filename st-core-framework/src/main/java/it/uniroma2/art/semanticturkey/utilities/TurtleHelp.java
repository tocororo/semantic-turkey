package it.uniroma2.art.semanticturkey.utilities;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.eclipse.rdf4j.model.Value;

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
}
