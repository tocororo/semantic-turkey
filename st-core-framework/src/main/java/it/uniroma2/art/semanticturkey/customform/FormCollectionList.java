package it.uniroma2.art.semanticturkey.customform;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FormCollectionList {

	// Map of id-FormCollection object pairs (contains all the {@link FormCollection}, also the ones that are not
	// reachable from the config tree, namely the ones that are not associated to any property or class)
	private Map<String, FormCollection> formCollMap;
	
	public FormCollectionList() {
		formCollMap = new HashMap<>();
	}
	
	public void add(FormCollection formCollection) throws DuplicateIdException {
		if (formCollMap.get(formCollection.getId()) != null) {
			throw new DuplicateIdException("Cannot add the FormCollection " + formCollection.getId() 
				+ ". Another FormCollection with the same ID exists.");
		}
		formCollMap.put(formCollection.getId(), formCollection);
	}
	
	public FormCollection get(String id) {
		return formCollMap.get(id);
	}
	
	public void remove(String id) {
		formCollMap.remove(id);
	}
	
	public Collection<FormCollection> getAllFormCollections() {
		return formCollMap.values();
	}

}
