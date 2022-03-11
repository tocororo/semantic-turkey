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
        public static final String propertyChain$description = keyBase + ".propertyChain.description";
        public static final String propertyChain$displayName = keyBase + ".propertyChain.displayName";
    }

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.property_chain;
    }

    @STProperty(description = "{" + MessageKeys.propertyChain$description + "}", displayName = "{" + MessageKeys.propertyChain$displayName + "}")
    @Required
    public List<IRI> propertyChain;

}
