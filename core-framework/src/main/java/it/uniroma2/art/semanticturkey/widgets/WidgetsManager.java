package it.uniroma2.art.semanticturkey.widgets;

import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.Widget;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.WidgetAssociation;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.WidgetAssociationStore;
import it.uniroma2.art.semanticturkey.config.visualizationwidgets.WidgetStore;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WidgetsManager {

    private Project project;

    //maps relative reference -> Widget/WidgetAssociation
    private Map<String, Widget> widgets;
    private Map<String, WidgetAssociation> associations;

    private WidgetStore widgetStore;
    private WidgetAssociationStore widgetAssociationStore;

    public WidgetsManager(Project project, ExtensionPointManager exptManager) throws STPropertyAccessException, NoSuchConfigurationManager {
        this.project = project;
        this.initWidgets(exptManager);
        this.initWidgetAssociations(exptManager);
    }

    private void initWidgets(ExtensionPointManager exptManager) throws STPropertyAccessException, NoSuchConfigurationManager {
        widgets = new HashMap<>();
        widgetStore = (WidgetStore) exptManager.getConfigurationManager(WidgetStore.class.getName());
        Collection<Reference> wRefs = widgetStore.getConfigurationReferences(project, null);
        for (Reference ref : wRefs) {
            widgets.put(ref.getRelativeReference(), widgetStore.getConfiguration(ref));
        }
    }

    private void initWidgetAssociations(ExtensionPointManager exptManager) throws NoSuchConfigurationManager, STPropertyAccessException {
        associations = new HashMap<>();
        widgetAssociationStore = (WidgetAssociationStore) exptManager.getConfigurationManager(WidgetAssociationStore.class.getName());
        Collection<Reference> waRefs = widgetAssociationStore.getConfigurationReferences(project, null);
        for (Reference ref : waRefs) {
            associations.put(ref.getRelativeReference(), widgetAssociationStore.getConfiguration(ref));
        }
    }

    public Set<String> getWidgetIdentifiers() {
        return widgets.keySet();
    }

    public Widget getWidget(String reference) {
        return widgets.get(reference);
    }

    public Widget getWidgetForTrigger(IRI trigger) {
        WidgetAssociation wa = getAssociationForTrigger(trigger);
        if (wa != null) {
            return widgets.get(wa.widgetReference);
        } else {
            return null;
        }
    }

    public void deleteWidget(Reference reference) throws ConfigurationNotFoundException {
        widgets.remove(reference.getRelativeReference());
        widgetStore.deleteConfiguration(reference);
    }

    public void storeWidget(Reference reference, Widget widget) throws STPropertyUpdateException, WrongPropertiesException, IOException {
        widgetStore.storeConfiguration(reference, widget);
        widgets.put(reference.getRelativeReference(), widget); //update the cached list
    }

    /**
     * Returns true if a widget with the given reference exists
     * @param reference
     * @return
     */
    public boolean widgetExists(String reference) {
        return widgets.containsKey(reference);
    }

    public Map<String, WidgetAssociation> listRefAssociationsMap() {
        return new HashMap<>(associations);
    }

    public Collection<WidgetAssociation> listAssociation() {
        return associations.values();
    }

    public WidgetAssociation getAssociationForTrigger(IRI trigger) {
        for (WidgetAssociation a : associations.values()) {
            if (a.trigger.equals(trigger)) {
                return a;
            }
        }
        return null;
    }

    public void deleteAssociation(Reference reference) throws ConfigurationNotFoundException {
        associations.remove(reference.getRelativeReference());
        widgetAssociationStore.deleteConfiguration(reference);
    }

    public Set<IRI> listTriggers()  {
        Set<IRI> triggers = new HashSet<>();
        for (WidgetAssociation a : associations.values()) {
            triggers.add(a.trigger);
        }
        return triggers;
    }

    public void storeAssociation(IRI predicate, String widgetRef) throws ConfigurationNotFoundException, STPropertyUpdateException, WrongPropertiesException, IOException {
        //make sure that the same association does not exists
        if (associationExists(predicate)) {
            throw new IllegalArgumentException("An association for the same predicate already exists");
        }

        //make sure that the pattern exists
        if (!widgetExists(widgetRef)) {
            throw new ConfigurationNotFoundException("Widget " + widgetRef + " not found");
        }

        //create the association
        WidgetAssociation association = new WidgetAssociation();
        association.trigger = predicate;
        association.widgetReference = widgetRef;

        //generate a randomic ID for the configuration to be stored
        String id = UUID.randomUUID().toString();
        Reference ref = new Reference(project, null, id);

        //store the association configuration
        widgetAssociationStore.storeConfiguration(ref, association);

        associations.put(ref.getRelativeReference(), association); //update the cached list
    }

    /**
     * Returns true if an association for the given predicate already exists
     * @param predicate
     * @return
     */
    public boolean associationExists(IRI predicate) {
        return associations.values().stream().anyMatch(a -> a.trigger.equals(predicate));
    }

}
