package it.uniroma2.art.semanticturkey.extension.impl.rendering.impls;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;

/**
 * A component of a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class TemplateComponent {

	public abstract String instantiate(Map<String, Value> variableValues);

}
