package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class WidgetAssociation implements Configuration {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.visualizationwidgets.WidgetAssociation";

        public static final String shortName = keyBase + ".shortName";
        public static final String trigger$description = keyBase + ".trigger.description";
        public static final String trigger$displayName = keyBase + ".trigger.displayName";
        public static final String widgetReference$description = keyBase + ".widgetReference.description";
        public static final String widgetReference$displayName = keyBase + ".widgetReference.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.trigger$description + "}", displayName = "{" + MessageKeys.trigger$displayName + "}")
    @Required
    public IRI trigger;

    @STProperty(description = "{" + MessageKeys.widgetReference$description + "}", displayName = "{" + MessageKeys.widgetReference$displayName + "}")
    @Required
    public String widgetReference;


}
