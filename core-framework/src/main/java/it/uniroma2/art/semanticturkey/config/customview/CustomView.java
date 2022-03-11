package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.ViewsEnum;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public abstract class CustomView implements Configuration {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.CustomView";

        public static final String shortName = keyBase + ".shortName";
        public static final String suggestedView$description = keyBase + ".suggestedView.description";
        public static final String suggestedView$displayName = keyBase + ".suggestedView.displayName";
    }

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.suggestedView$description + "}", displayName = "{" + MessageKeys.suggestedView$displayName + "}")
    @Required
    public ViewsEnum suggestedView;

    public abstract CustomViewModelEnum getModelType();

//    /**
//     * Returns the list of bindings that the retrieve query must return
//     * @return
//     */
//    public abstract Set<WidgetDataBindings> getBindingSet();
//
//    /**
//     * Returns the list of bindings that are mandatory in the update
//     * @return
//     */
//    public abstract Set<WidgetDataBindings> getUpdateMandatoryBindings();

}
