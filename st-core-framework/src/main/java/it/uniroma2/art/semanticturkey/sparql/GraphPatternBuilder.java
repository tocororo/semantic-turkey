package it.uniroma2.art.semanticturkey.sparql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.services.support.GraphPatternBuilderException;

/**
 * Builder for the construction of {@link GraphPattern} objects.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class GraphPatternBuilder {
	private final HashMap<String, IRI> prefixMapping;
	private String pattern;
	private final ValueFactory vf;
	private List<ProjectionElement> projection;

	public GraphPatternBuilder() {
		this.prefixMapping = new HashMap<>();
		this.pattern = null;
		this.vf = SimpleValueFactory.getInstance();
	}

	public static GraphPatternBuilder create() {
		return new GraphPatternBuilder();
	}

	public GraphPatternBuilder prefix(Namespace namespace) {
		return prefix(namespace.getPrefix(), vf.createIRI(namespace.getName()));
	}

	public GraphPatternBuilder prefix(String prefix, String namespace)
			throws IllegalArgumentException, GraphPatternBuilderException {
		return prefix(prefix, vf.createIRI(namespace));
	}

	public GraphPatternBuilder prefix(String prefix, IRI namespace) throws GraphPatternBuilderException {
		if (prefixMapping.containsKey(prefix)) {
			throw new GraphPatternBuilderException("Prefix '" + prefix + "' aready bound to a namespace");
		}
		prefixMapping.put(prefix, namespace);
		return this;
	}

	public GraphPatternBuilder projection(ProjectionElement... projection) {
		return projection(Arrays.asList(projection));
	}

	public GraphPatternBuilder projection(List<ProjectionElement> projection) {
		if (this.projection != null) {
			throw new GraphPatternBuilderException("Projection element already bound");
		}
		this.projection = projection;
		return this;
	}

	public GraphPatternBuilder pattern(String pattern) throws GraphPatternBuilderException {
		if (this.pattern != null) {
			throw new GraphPatternBuilderException("Graph pattern already bound");
		}
		this.pattern = pattern;
		return this;
	}

	public GraphPattern graphPattern() throws GraphPatternBuilderException {
		if (pattern == null) {
			throw new GraphPatternBuilderException("Graph pattern to set");
		}

		if (projection == null) {
			throw new GraphPatternBuilderException("Projection to set");
		}

		return new GraphPattern(prefixMapping, projection, pattern);
	}

}
