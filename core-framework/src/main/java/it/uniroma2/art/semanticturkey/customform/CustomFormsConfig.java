package it.uniroma2.art.semanticturkey.customform;

import org.eclipse.rdf4j.model.IRI;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Collection of {@link FormsMapping}. This class represent a model in memory of <code>customFormsConfig.xml</code>
 * @author Tiziano
 *
 */
public class CustomFormsConfig {
	
	private Collection<FormsMapping> mappings;
	
	protected static final String CUSTOM_FORMS_CONFIG_FILENAME = "customFormConfig.xml";
	
	public CustomFormsConfig(Collection<FormsMapping> mappings) {
		this.mappings = new ArrayList<>(mappings);
	}

	public CustomFormsConfig() {
		this(new ArrayList<>());
	}
	
	/**
	 * Returns the FormCollectionMapping of the given resource
	 * @param resource
	 * @return
	 */
	public FormsMapping getFormsMapping(IRI resource) {
		for (FormsMapping mapping : mappings) {
			if (mapping.getResource().equals(resource.stringValue())) {
				return mapping;
			}
		}
		return null;
	}
	
	public Collection<FormsMapping> getFormsMappings(){
		return mappings;
	}
	
	/**
	 * Given a resource returns the {@link FormCollection} associated to that resource. <code>null</code> if no
	 * {@link FormCollection} is specified for it.
	 * @param resource
	 * @return
	 */
	public FormCollection getFormCollectionForResource(IRI resource) {
		for (FormsMapping m : mappings){
			if (m.getResource().equals(resource.stringValue())) {
				return m.getFormCollection();
			}
		}
		return null;
	}
	
	/**
	 * Adds a {@link FormsMapping} to the configuration
	 * @param formMapping
	 */
	public void addFormsMapping(FormsMapping formMapping){
		mappings.add(formMapping);
	}
	
	/**
	 * Remove a {@link FormsMapping} from the configuration
	 * @param resource
	 */
	public void removeMappingOfResource(IRI resource){
		mappings.removeIf(mapping -> mapping.getResource().equals(resource.stringValue()));
	}
	
	/**
	 * Remove resource-FormCollection pair with the given FormCollection ID
	 * @param formColl
	 */
	public void removeMappingOfFormCollection(FormCollection formColl){
		mappings.removeIf(mapping -> mapping.getFormCollection().getId().equals(formColl.getId()));
	}
	
	/**
	 * Tells if the CustomForm should replace the "classic" form for the given resource.
	 * @param resource
	 * @return
	 */
	public boolean getReplace(IRI resource){
		for (FormsMapping m : mappings){
			if (m.getResource().equals(resource.stringValue())) {
				return m.getReplace();
			}
		}
		return false;
	}
	
	/**
	 * Serialize the CustomFormsConfig on a xml file.
	 */
	public void save(File file){
		CustomFormXMLHelper.serializeCustomFormsConfig(this, file);
	}
	
}
