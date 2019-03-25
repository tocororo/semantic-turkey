package it.uniroma2.art.semanticturkey.extension.impl.rendering.impls;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import com.google.common.base.MoreObjects;

/**
 * A variable usage inside a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class VariableComponent extends TemplateComponent {
	private String variableName;

	public VariableComponent(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("variableName", variableName).toString();
	}

	@Override
	public String instantiate(Map<String, Value> variableValues) {
		Value rv = variableValues.get(variableName);

		if (rv == null) {
			return null;
		} else if (rv instanceof IRI) {
			return ((IRI) rv).getLocalName();
		} else {
			return rv.stringValue();
		}
	}
}
