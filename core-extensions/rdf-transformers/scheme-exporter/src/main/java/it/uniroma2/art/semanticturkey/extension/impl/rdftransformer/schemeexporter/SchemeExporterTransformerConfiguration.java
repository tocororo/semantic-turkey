package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.schemeexporter;


import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SchemeExporterTransformerConfiguration implements Configuration {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.schemeexporter.SchemeExporterTransformerConfiguration";

        public static final String shortName = keyBase + ".shortName";
        public static final String scheme$description = keyBase + ".scheme.description";
        public static final String scheme$displayName = keyBase + ".scheme.displayName";

    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }


    @STProperty(description = "{" + MessageKeys.scheme$description + "}", displayName = "{" + MessageKeys.scheme$displayName + "}")
    @Required
    @HasRole(RDFResourceRole.conceptScheme)
    public IRI scheme;

}
