package it.uniroma2.art.semanticturkey.customform;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomFormList {

	// Map of id-CustomForm object pairs (contains all the {@link CustomForm}, also the ones that are not reachable
	// from the config tree, namely the ones that are not contained in any {@link FormCollection})
	private Map<String, CustomForm> formsMap = new HashMap<>();

	public CustomFormList() {
		formsMap = new HashMap<>();
	}

	public void add(CustomForm customForm) throws DuplicateIdException {
		if (formsMap.get(customForm.getId()) != null) {
			throw new DuplicateIdException("Cannot add the CustomForm " + customForm.getId() 
				+ ". Another CustomForm with the same ID exists.");
		}
		formsMap.put(customForm.getId(), customForm);
	}

	public CustomForm get(String id) {
		return formsMap.get(id);
	}

	public void remove(String id) {
		formsMap.remove(id);
	}
	
	public Collection<CustomForm> getAllCustomForms() {
		return formsMap.values();
	}

}
