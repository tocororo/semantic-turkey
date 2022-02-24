package it.uniroma2.art.semanticturkey.config.visualizationwidgets;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;

public class WidgetStore implements ProjectScopedConfigurableComponent<Widget> {

    @Override
    public String getId() {
        return WidgetStore.class.getName();
    }
}
