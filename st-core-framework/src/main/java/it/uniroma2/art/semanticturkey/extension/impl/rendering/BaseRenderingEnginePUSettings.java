package it.uniroma2.art.semanticturkey.extension.impl.rendering;

import java.util.LinkedHashMap;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class BaseRenderingEnginePUSettings implements Settings {

	@STProperty(description = "The template for the redering of resources", displayName = "template")
	public String template;

	@STProperty(description = "Definition of the variables that can be used inside the template", displayName = "variables")
	public LinkedHashMap<String, VariableDefinition> variables;

	@STProperty(description = "Tells whether the rendering engine should ignore the fact that validation is enabled", displayName = "ignore validation")
	public Boolean ignoreValidation;
}
