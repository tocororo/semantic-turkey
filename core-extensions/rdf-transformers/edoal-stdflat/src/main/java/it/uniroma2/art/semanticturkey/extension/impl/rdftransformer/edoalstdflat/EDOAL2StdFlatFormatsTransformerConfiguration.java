package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalstdflat;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class EDOAL2StdFlatFormatsTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalstdflat.EDOAL2StdFlatFormatsTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String mappingProperties$description = keyBase + ".mappingProperties.description";
		public static final String mappingProperties$displayName = keyBase + ".mappingProperties.displayName";
		public static final String subjectPosition$description = keyBase + ".subjectPosition.description";
		public static final String subjectPosition$displayName = keyBase + ".subjectPosition.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.mappingProperties$description + "}", displayName = "{" + MessageKeys.mappingProperties$displayName + "}")
	public Set<@HasRole(RDFResourceRole.property) IRI> mappingProperties = new HashSet<>();

	@STProperty(description = "{" + MessageKeys.subjectPosition$description + "}", displayName = "{" + MessageKeys.subjectPosition$displayName + "}")
	public EDOAL2StdFlatFormatsTransformer.SubjectPosition subject_position = EDOAL2StdFlatFormatsTransformer.SubjectPosition.entity1;

}
