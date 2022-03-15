package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.SingleValueUpdate;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class AdvSingleValueView extends CustomView {

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.adv_single_value;
    }

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.AbstractSparqlBasedCustomView";

        public static final String shortName = keyBase + ".shortName";
        public static final String retrieve$description = keyBase + ".retrieve.description";
        public static final String retrieve$displayName = keyBase + ".retrieve.displayName";
        public static final String update$description = keyBase + ".update.description";
        public static final String update$displayName = keyBase + ".update.displayName";
    }

    @Required
    @STProperty(description = "{" + MessageKeys.retrieve$description + "}", displayName = "{" + MessageKeys.retrieve$displayName + "}")
    public String retrieve;

    @Required
    @STProperty(description = "{" + MessageKeys.update$description + "}", displayName = "{" + MessageKeys.update$displayName + "}")
    public SingleValueUpdate update;

}
