package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.stdflatedoal;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class StdFlatFormats2EDOALTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.stdflatedoal.StdFlatFormats2EDOALTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String mappingProperties$description = keyBase + ".mappingProperties.description";
		public static final String mappingProperties$displayName = keyBase + ".mappingProperties.displayName";
		public static final String entity1Position$description = keyBase + ".entity1Position.description";
		public static final String entity1Position$displayName = keyBase + ".entity1Position.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.mappingProperties$description + "}", displayName = "{" + MessageKeys.mappingProperties$displayName + "}")
	public Set<@HasRole(RDFResourceRole.property) IRI> mappingProperties = new HashSet<>();

	@STProperty(description = "{" + MessageKeys.entity1Position$displayName + "}", displayName = "{" + MessageKeys.entity1Position$description + "}")
	public StdFlatFormats2EDOALTransformer.Entity1Position entity1_position = StdFlatFormats2EDOALTransformer.Entity1Position.subject;

}
