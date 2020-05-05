package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;

import com.google.common.base.MoreObjects;

/**
 * A variable usage inside a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class NestedTemplateComponent extends TemplateComponent {
	private Template template;

	public NestedTemplateComponent(Template template) {
		this.template = template;
	}

	public Template getTemplate() {
		return template;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("template", template).toString();
	}

	@Override
	public String instantiate(Map<String, Value> variableValues) {
		String rv = template.instantiate(variableValues);

		if (rv == null) {
			return "";
		} else {
			return rv;
		}
	}

}
