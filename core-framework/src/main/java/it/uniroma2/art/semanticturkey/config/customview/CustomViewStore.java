package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

public class CustomViewStore implements ProjectScopedConfigurableComponent<CustomView> {

    @Override
    public String getId() {
        return CustomViewStore.class.getName();
    }
}
