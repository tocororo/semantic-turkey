package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;

import com.google.common.base.MoreObjects;

/**
 * A template for resource rendering
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class Template {
	private List<TemplateComponent> components;

	public Template(List<TemplateComponent> components) {
		this.components = components;
	}

	public List<TemplateComponent> getComponents() {
		return components;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("components", components).toString();
	}

	public String instantiate(Map<String, Value> variableValues) {
		if (components.size() == 1) {
			return components.iterator().next().instantiate(variableValues);
		} else {
			StringBuilder sb = new StringBuilder();

			for (TemplateComponent comp : components) {
				String compString = comp.instantiate(variableValues);

				if (compString == null) {
					return null;
				}

				sb.append(compString);
			}

			return sb.toString();
		}
	}

}
