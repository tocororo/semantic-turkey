package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Definition of a variable to be used inside a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class VariableDefinition {
	private List<IRI> propertyPath;

	public VariableDefinition(@JsonProperty("propertyPath") List<IRI> propertyPath) {
		this.propertyPath = propertyPath;
	}

	public List<IRI> getPropertyPath() {
		return propertyPath;
	}
}
