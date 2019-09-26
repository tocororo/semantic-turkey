package it.uniroma2.art.semanticturkey.user;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ORG;

import java.util.*;

public class UserForm {

	private Map<IRI, Boolean> optionalFields; //for each fields tells if it visible or not
	private Map<IRI, UserFormCustomField> customFields;

	public static final List<IRI> optionalFieldsProperties = Arrays.asList(
			FOAF.PHONE, ORG.MEMBER_OF, UserVocabulary.ADDRESS, UserVocabulary.URL);
	
	public static final List<IRI> customFieldsProperties = Arrays.asList(
			UserVocabulary.USER_CUSTOM_PROP_1,UserVocabulary.USER_CUSTOM_PROP_2, 
			UserVocabulary.USER_CUSTOM_PROP_3, UserVocabulary.USER_CUSTOM_PROP_4);

	public UserForm() {
		customFields = new LinkedHashMap<IRI, UserFormCustomField>();
		optionalFields = new HashMap<>();
		for (IRI f: optionalFieldsProperties) {
			optionalFields.put(f, true); //optional fields are visible by default, eventually changed during the initialization of the form
		}
	}

	/**
	 * OPTIONAL FIELDS
	 * @return
	 */

	public Map<IRI, Boolean> getOptionalFields() {
		return optionalFields;
	}

	public void setOptionalFieldVisibility(IRI field, boolean visible) {
		//update the visibility only if the property is in the form (prevent setting visibility to a not existing fields)
		if (optionalFields.containsKey(field)) {
			optionalFields.put(field, visible);
		}
	}

	/**
	 * CUSTOM FIELDS
	 */

	public Collection<UserFormCustomField> getCustomFields() {
		return customFields.values();
	}
	
	public UserFormCustomField getCustomField(IRI prop) {
		return customFields.get(prop);
	}
	
	public void addField(UserFormCustomField field) {
		customFields.put(field.getIri(), field);
	}
	
	public void removeCustomField(IRI iri) {
		UserFormCustomField field = getCustomField(iri);
		int removedPosition = field.getPosition();
		customFields.remove(field.getIri());
		//update positions
		for (UserFormCustomField f: customFields.values()) {
			if (f != null && f.getPosition() > removedPosition) {
				f.setPosition(f.getPosition()-1);
			}
		}
	}

	public IRI getFirstAvailableProperty() {
		for (IRI p: customFieldsProperties) {
			if (customFields.get(p) == null) {
				return p;
			}
		}
		return null; //this should never be returned since there should be integrity controls that prevent it
	}
	
	public List<UserFormCustomField> getOrderedCustomFields() {
		Collection<UserFormCustomField> fields = customFields.values();
		UserFormCustomField orderedFields[] = new UserFormCustomField[fields.size()];
		for (UserFormCustomField f: customFields.values()) {
			orderedFields[f.getPosition()] = f;
		}
		return Arrays.asList(orderedFields);
	}
	
}


