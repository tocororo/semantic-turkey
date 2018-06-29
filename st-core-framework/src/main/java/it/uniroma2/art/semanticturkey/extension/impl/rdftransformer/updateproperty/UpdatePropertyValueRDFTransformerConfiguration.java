package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.updateproperty;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class UpdatePropertyValueRDFTransformerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Update Property Value RDF Transformer";
	}

	@STProperty(description = "The subject of the filtered triple")
	@Required
	public Resource resource;

	@STProperty(description = "The predicate of the filtered triple")
	@Required
	@HasRole(RDFResourceRole.property)
	public IRI property;

	@STProperty(description = "The new value to be set")
	@Required
	public Value value = null;

	@STProperty(description = "if set, the triple <resource, property, oldValue> is "
			+ "replaced by <resource, property, value>. If not set, then all <resource, property, *> are "
			+ "deleted and <resource, property, value> is written")
	public String oldValue = null;

}
