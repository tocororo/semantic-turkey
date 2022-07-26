package it.uniroma2.art.semanticturkey.settings.contentnegotiation;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.Collection;

public class ContentNegotiationSettings implements Settings {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.contentnegotiation.ContentNegotiationSettings";

        public static final String shortName = keyBase + ".shortName";

        public static final String rewritingRules$description = keyBase
                + ".rewritingRules.description";
        public static final String rewritingRules$displayName = keyBase
                + ".rewritingRules.displayName";

        public static final String inverseRewritingRules$description = keyBase
                + ".inverseRewritingRules.description";
        public static final String inverseRewritingRules$displayName = keyBase
                + ".inverseRewritingRules.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.rewritingRules$description + "}", displayName = "{"
            + MessageKeys.rewritingRules$displayName + "}")
    public Collection<RewritingRule> rewritingRules;

    @STProperty(description = "{" + MessageKeys.inverseRewritingRules$description + "}", displayName = "{"
            + MessageKeys.inverseRewritingRules$displayName + "}")
    public Collection<InverseRewritingRule> inverseRewritingRules;

}
