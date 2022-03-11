package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

public class CustomViewAssociationStore implements ProjectScopedConfigurableComponent<CustomViewAssociation> {

    @Override
    public String getId() {
        return CustomViewAssociationStore.class.getName();
    }
}