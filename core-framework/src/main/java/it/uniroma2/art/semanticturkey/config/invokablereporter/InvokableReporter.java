package it.uniroma2.art.semanticturkey.config.invokablereporter;

import java.util.List;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * An invokable reporter based on the invocation of custom services
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class InvokableReporter implements Configuration {

	@Override
	public String getShortName() {
		return "Custom Service";
	}

	@STProperty(description = "The label of this invokable reporter", displayName = "Label")
	@Required
	public String label;

	@STProperty(description = "A description of this invokable reporter", displayName = "Description")
	public String description;

	@STProperty(description = "The invocations of custom services that make up this invokable reporter", displayName = "Service invocations")
	public List<ServiceInvocation> sections;

	@STProperty(description = "A template for rendering the report that takes all results as input. The template must conform to Mustache templating language.", displayName = "Template")
	@Required
	public String template = "";

	@STProperty(description = "The mimetype of the generated rendering. If not specified, it is assumed text/plain", displayName = "mime")
	@Required
	public String mimeType = "text/plain";

}
