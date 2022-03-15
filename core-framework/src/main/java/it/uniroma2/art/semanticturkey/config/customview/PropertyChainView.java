package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class PropertyChainView extends CustomView {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.PropertyChainView";
        public static final String shortName = keyBase + ".shortName";
        public static final String properties$description = keyBase + ".properties.description";
        public static final String properties$displayName = keyBase + ".properties.displayName";
    }

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.property_chain;
    }

    @STProperty(description = "{" + MessageKeys.properties$description + "}", displayName = "{" + MessageKeys.properties$displayName + "}")
    @Required
    public List<IRI> properties;

}
