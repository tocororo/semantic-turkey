package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

public class WidgetAssociationStore implements ProjectScopedConfigurableComponent<WidgetAssociation> {

    @Override
    public String getId() {
        return WidgetAssociationStore.class.getName();
    }
}