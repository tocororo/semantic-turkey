package it.uniroma2.art.semanticturkey.extension.extpts.urigen;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.services.STServiceContext;

/**
 * Extension point for the generation of URIs. Such a generation is performed, when it is not possible or
 * desired to generate a URI based on a provided local name.
 */
public interface URIGenerator {

	public static final class Roles {
		public static final String concept = "concept";
		public static final String conceptScheme = "conceptScheme";
		public static final String xLabel = "xLabel";
		public static final String xNote = "xNote";
		public static final String skosCollection = "skosCollection";
	}

	public static final class Parameters {
		public static final String label = "label";
		public static final String schemes = "schemes";
		public static final String lexicalForm = "lexicalForm";
		public static final String lexicalizedResource = "lexicalizedResource";
		public static final String lexicalizationProperty = "lexicalizationProperty";
		public static final String noteProperty = "noteProperty";
		public static final String value = "value";
		public static final String annotatedResource = "annotatedResource";
	}

	/**
	 * Returns a resource identifier produced randomly from the given inputs. The parameter {@code xRole}
	 * holds the nature of the resource that will be identified with the given URI. Depending on the value of
	 * the parameter {@code xRole}, a conforming converter may generate differently shaped URIs, possibly
	 * using specific arguments passed via the map {@code args}.
	 * 
	 * For each specific {@code xRole}, the client should provide some agreed-upon parameters to the
	 * converters. This contract defines the following parameters:
	 * 
	 * <ul>
	 * <li><code>concept</code> (for <code>skos:Concept</code>s)
	 * <ul>
	 * <li><code>label</code> (optional): the accompanying preferred label of the <i>skos:Concept</i> (or
	 * literal form of the accompanying xLabel)</li> <i>skos:Concept</i></li>
	 * <li><code>schemes</code> (optional): the concept schemes to which the concept is being attached at the
	 * moment of its creation (serialized as a Turtle collection)</li>
	 * </ul>
	 * </li>
	 * <li><code>conceptScheme</code> (for <code>skos:ConceptScheme</code>s)
	 * <ul>
	 * <li><code>label</code> (optional): the accompanying preferred label of the <i>skos:Concept</i> (or
	 * literal form of the accompanying xLabel)</li> <i>skos:Concept</i></li>
	 * </ul>
	 * </li>
	 * <li><code>skosCollection</code> (for <code>skos:Collection</code>s)
	 * <ul>
	 * <li><code>label</code> (optional): the accompanying preferred label of the <i>skos:Collection</i> (or
	 * literal form of the accompanying xLabel)</li>
	 * </ul>
	 * </li>
	 * <li><code>xLabel</code> (for <code>skosxl:Labels</code>s)
	 * <ul>
	 * <li><code>lexicalForm</code>: the lexical form of the <i>skosxl:Label</i></li>
	 * <li><code>lexicalizedResource</code>: the resource to which the <i>skosxl:Label</i> will be attached to
	 * </li>
	 * <li><code>lexicalizationProperty</code>: the property used for attaching the label</li>
	 * </ul>
	 * </li>
	 * <li><code>xNote</code></li> (for reified <code>skos:note</code>s)
	 * <ul>
	 * <li><code>value</code>: the content of the note</li>
	 * <li><code>annotatedResource</code>: the resource being annotated</li>
	 * <li><code>noteProperty</code>: the property used for annotation</li>
	 * </ul>
	 * </li></li>
	 * </ul>
	 *
	 * The parameters requested by additional <code>xRole</code>s are defined elsewhere by the party defining
	 * that <code>xRole</code>.
	 * 
	 * Users of this extension point should always supply values for the required parameters associated with
	 * an <code>xRole</code>; therefore, they should not attempt to generate a URI for an <code>xRole</code>
	 * unless they known what arguments are requested.
	 * 
	 * Conversely, it is a duty of the specific implementation of this extension point to verify that all
	 * relevant information has been provided by the client. In fact, it is suggested that extension points
	 * are implemented defensively, that is to say they should:
	 * 
	 * <ul>
	 * <li>complain only about the absence of absolutely required parameters</li>
	 * <li>handle unknown <code>xRole</code>s gracefully, by means some fallback strategy</li>
	 * </ul>
	 * *
	 * 
	 * @param stServiceContext
	 * @param xRole
	 * @param args
	 * @return
	 * @throws URIGenerationException
	 */
	IRI generateIRI(STServiceContext stServiceContext, String xRole, Map<String, Value> args)
			throws URIGenerationException;
}
