package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Definition of a variable to be used inside a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class VariableDefinition implements STProperties {
	@Override
	public String getShortName() {
		return "Variable definition";
	}

	@STProperty(description = "", displayName =  "Property path")
	public List<IRI> propertyPath;

}
