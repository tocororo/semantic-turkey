package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.customviews.ViewsEnum;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class CustomViewAssociation implements Configuration {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.CustomViewAssociation";

        public static final String shortName = keyBase + ".shortName";
        public static final String property$description = keyBase + ".property.description";
        public static final String property$displayName = keyBase + ".property.displayName";
        public static final String customViewRef$description = keyBase + ".customViewRef.description";
        public static final String customViewRef$displayName = keyBase + ".customViewRef.displayName";
        public static final String defaultView$description = keyBase + ".defaultView.description";
        public static final String defaultView$displayName = keyBase + ".defaultView.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.property$description + "}", displayName = "{" + MessageKeys.property$displayName + "}")
    @Required
    public IRI property;

    @STProperty(description = "{" + MessageKeys.customViewRef$description + "}", displayName = "{" + MessageKeys.customViewRef$displayName + "}")
    @Required
    public String customViewRef;


    @STProperty(description = "{" + MessageKeys.defaultView$description + "}", displayName = "{" + MessageKeys.defaultView$displayName + "}")
    @Required
    public ViewsEnum defaultView;

}
