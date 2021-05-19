package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;
import java.util.Set;

public class ResourceViewCustomSectionSettings implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ResourceViewCustomSectionSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String matchedProperties$description = keyBase + ".matchedProperties.description";
        public static final String matchedProperties$displayName = keyBase + ".matchedProperties.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + ResourceViewProjectSettings.MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.matchedProperties$description + "}", displayName = "{" + MessageKeys.matchedProperties$displayName + "}")
    @Required
    public Set<@HasRole(RDFResourceRole.property) IRI> matchedProperties;

}
