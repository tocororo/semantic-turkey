package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalflattenizer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class EdoalFlattenizerTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalflattenizer.EdoalFlattenizerTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String mappingProperties$description = keyBase + ".mappingProperties.description";
		public static final String mappingProperties$displayName = keyBase + ".mappingProperties.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.mappingProperties$description + "}", displayName = "{" + MessageKeys.mappingProperties$displayName + "}")
	public Set<@HasRole(RDFResourceRole.property) IRI> mappingProperties = new HashSet<>();

}
