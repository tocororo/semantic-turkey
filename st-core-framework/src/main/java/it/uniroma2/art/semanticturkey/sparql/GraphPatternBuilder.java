package it.uniroma2.art.semanticturkey.sparql;

import java.util.HashMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.services.support.GraphPatternBuilderException;

public class GraphPatternBuilder {
	private final HashMap<String, IRI> prefixMapping;
	private String pattern;
	private final ValueFactory vf;
	private ProjectionElement projectionElement;

	public GraphPatternBuilder() {
		this.prefixMapping = new HashMap<>();
		this.pattern = null;
		this.vf = SimpleValueFactory.getInstance();
	}
	
	public static GraphPatternBuilder create() {
		return new GraphPatternBuilder();
	}

	public GraphPatternBuilder prefix(String prefix, String namespace)
			throws IllegalArgumentException, GraphPatternBuilderException {
		return prefix(prefix, vf.createIRI(namespace));
	}

	public GraphPatternBuilder prefix(String prefix, IRI namespace) throws GraphPatternBuilderException {
		if (prefixMapping.containsKey(namespace)) {
			throw new GraphPatternBuilderException("Prefix '" + prefix + "' aready bound to a namespace");
		}
		prefixMapping.put(prefix, namespace);
		return this;
	}
	
	public GraphPatternBuilder projection(ProjectionElement projectionElement) {
		if (this.projectionElement != null) {
			throw new GraphPatternBuilderException("Projection element already bound");
		}
		this.projectionElement = projectionElement;
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
		
		if (projectionElement == null) {
			throw new GraphPatternBuilderException("Projection element to set");
		}
		
		return new GraphPattern(prefixMapping, projectionElement, pattern);
	}
	
}

