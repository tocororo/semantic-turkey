package it.uniroma2.art.semanticturkey.config.invokablereporter;

import java.util.Map;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ServiceInvocation implements STProperties {

	@Override
	public String getShortName() {
		return "Service Invocation";
	}

	@STProperty(displayName = "Service", description = "The name of service being invoked")
	@Required
	public String service;

	@STProperty(displayName = "Operation", description = "the name of the specific operation invoked")
	@Required
	public String operation;

	@STProperty(displayName = "Arguments", description = "the arguments passed to the invoked service operation")
	public Map<String, String> arguments;

	@STProperty(description = "The label of this section", displayName = "Label")
	public String label;

	@STProperty(description = "A description of this section", displayName = "Description")
	public String description;

	@STProperty(description = "A template for rendering an individual section. The template must conform to Mustache templating language.", displayName = "Template")
	public String template;

}
