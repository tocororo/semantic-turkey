package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.Map;

import it.uniroma2.art.semanticturkey.properties.ExtensionSpecification;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A <em>custom service operation</em> definition.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class OperationDefintion implements STProperties {

	@Override
	public String getShortName() {
		return "Custom Service Operation Definition";
	}

	@STProperty(description = "Definitions of the parameters of a custom service operation", displayName = "Parameters")
	public Map<String, ParameterDefinition> parameters;

	@STProperty(description = "Return value", displayName = "Returns")
	public TypeDescription returns;
	
	@STProperty(description = "The implementation as an extension", displayName="Implementation")
	public ExtensionSpecification implementation;
}
