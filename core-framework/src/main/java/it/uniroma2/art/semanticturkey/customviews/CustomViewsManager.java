package it.uniroma2.art.semanticturkey.customviews;

import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.customview.CustomView;
import it.uniroma2.art.semanticturkey.config.customview.CustomViewAssociation;
import it.uniroma2.art.semanticturkey.config.customview.CustomViewAssociationStore;
import it.uniroma2.art.semanticturkey.config.customview.CustomViewStore;
import it.uniroma2.art.semanticturkey.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import org.eclipse.rdf4j.model.IRI;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomViewsManager {

    private Project project;

    //maps relative reference -> CustomView/PropertyAssociation
    private Map<String, CustomView> customViews;
    private Map<String, CustomViewAssociation> associations;

    private CustomViewStore customViewsStore;
    private CustomViewAssociationStore customViewsAssociationsStore;

    public CustomViewsManager(Project project, ExtensionPointManager exptManager) throws STPropertyAccessException, NoSuchConfigurationManager {
        this.project = project;
        this.initCustomViews(exptManager);
        this.initPropertyAssociations(exptManager);
    }

    public Map<String, CustomViewAssociation> listRefAssociationsMap() {
        return new HashMap<>(associations);
    }

    // =========== VIEWS ===========

    private void initCustomViews(ExtensionPointManager exptManager) throws STPropertyAccessException, NoSuchConfigurationManager {
        customViews = new HashMap<>();
        customViewsStore = (CustomViewStore) exptManager.getConfigurationManager(CustomViewStore.class.getName());
        Collection<Reference> wRefs = customViewsStore.getConfigurationReferences(project, null);
        for (Reference ref : wRefs) {
            customViews.put(ref.getRelativeReference(), customViewsStore.getConfiguration(ref));
        }
    }

    /**
     * Returns the identifiers (configurations references) of CVs
     *
     * @return
     */
    public Set<String> getCustomViewsIdentifiers() {
        return customViews.keySet();
    }

    /**
     * Given a reference returns the CV
     *
     * @param reference
     * @return
     */
    public CustomView getCustomView(String reference) {
        return customViews.get(reference);
    }

    public File getCustomViewFile(String id) {
        File folder = ConfigurationSupport.getConfigurationFolder(customViewsStore, project);
        return new File(folder, id + ".cfg");
    }

    /**
     * Given a property, returns the CV associated, if any, null otherwise
     *
     * @param property
     * @return
     */
    public CustomView getCustomViewForProperty(IRI property) {
        CustomViewAssociation wa = getAssociationForProperty(property);
        if (wa != null) {
            return customViews.get(wa.customViewRef);
        } else {
            return null;
        }
    }

    public void storeCustomView(Reference reference, CustomView customView) throws STPropertyUpdateException, WrongPropertiesException, IOException {
        customViewsStore.storeConfiguration(reference, customView);
        customViews.put(reference.getRelativeReference(), customView); //update the cached list
    }

    public void storeCustomViewFromFile(Reference reference, File srcFile) throws IOException, STPropertyAccessException {
        File cvFile = getCustomViewFile(reference.getIdentifier());
        if (!cvFile.getParentFile().exists()) { // if path doesn't exist, first create it
            cvFile.getParentFile().mkdirs();
        }
        Utilities.copy(srcFile, cvFile);
        CustomView customView = customViewsStore.getConfiguration(reference);
        customViews.put(reference.getRelativeReference(), customView);
    }

    public void deleteCustomView(Reference reference) throws ConfigurationNotFoundException {
        customViews.remove(reference.getRelativeReference());
        customViewsStore.deleteConfiguration(reference);
    }

    /**
     * Returns true if a custom view with the given reference exists
     *
     * @param reference
     * @return
     */
    public boolean customViewExists(String reference) {
        return customViews.containsKey(reference);
    }

    // =========== ASSOCIATIONS ===========

    private void initPropertyAssociations(ExtensionPointManager exptManager) throws NoSuchConfigurationManager, STPropertyAccessException {
        associations = new HashMap<>();
        customViewsAssociationsStore = (CustomViewAssociationStore) exptManager.getConfigurationManager(CustomViewAssociationStore.class.getName());
        Collection<Reference> waRefs = customViewsAssociationsStore.getConfigurationReferences(project, null);
        for (Reference ref : waRefs) {
            associations.put(ref.getRelativeReference(), customViewsAssociationsStore.getConfiguration(ref));
        }
    }

    /**
     * Lists the available associations
     *
     * @return
     */
    public Collection<CustomViewAssociation> listAssociation() {
        return associations.values();
    }

    /**
     * Given a property, returns the association, if any, null otherwise
     *
     * @param property
     * @return
     */
    public CustomViewAssociation getAssociationForProperty(IRI property) {
        for (CustomViewAssociation a : associations.values()) {
            if (a.property.equals(property)) {
                return a;
            }
        }
        return null;
    }

    public void storeAssociation(IRI property, String customViewRef, ViewsEnum defaultView) throws ConfigurationNotFoundException, STPropertyUpdateException, WrongPropertiesException, IOException {
        //make sure that the same association does not exists
        if (associationExists(property)) {
            throw new IllegalArgumentException("An association for the same property already exists");
        }

        //make sure that the pattern exists
        if (!customViewExists(customViewRef)) {
            throw new ConfigurationNotFoundException("CustomView " + customViewRef + " not found");
        }

        //create the association
        CustomViewAssociation association = new CustomViewAssociation();
        association.property = property;
        association.customViewRef = customViewRef;
        association.defaultView = defaultView;

        //generate a random ID for the configuration to be stored
        String id = UUID.randomUUID().toString();
        Reference ref = new Reference(project, null, id);

        //store the association configuration
        customViewsAssociationsStore.storeConfiguration(ref, association);

        associations.put(ref.getRelativeReference(), association); //update the cached list
    }

    public void deleteAssociation(Reference reference) throws ConfigurationNotFoundException {
        associations.remove(reference.getRelativeReference());
        customViewsAssociationsStore.deleteConfiguration(reference);
    }

    /**
     * Returns true if an association for the given property already exists,
     * namely if the given property has a CustomView
     *
     * @param property
     * @return
     */
    public boolean associationExists(IRI property) {
        return associations.values().stream().anyMatch(a -> a.property.equals(property));
    }

}
