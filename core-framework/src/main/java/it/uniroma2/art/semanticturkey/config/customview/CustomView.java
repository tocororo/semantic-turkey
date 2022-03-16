package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.customviews.CustomViewData;
import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.customviews.ViewsEnum;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;

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

    public abstract CustomViewData getData(RepositoryConnection connection, Resource resource, IRI property, IRI workingGraph);

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
