package it.uniroma2.art.semanticturkey.extension.impl.rendering.impls;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;

import com.google.common.base.MoreObjects;

/**
 * Literal text inside a rendering template
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class LiteralText extends TemplateComponent {
	private String text;

	public LiteralText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("text", text).toString();
	}

	@Override
	public String instantiate(Map<String, Value> variableValues) {
		return text;
	}
}
