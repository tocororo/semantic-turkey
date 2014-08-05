package it.uniroma2.art.semanticturkey.data.access.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.LinkedDataResolver;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.PropertyPatternDataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PropertyPatternDataAccessDereferencingImpl implements PropertyPatternDataAccess {

	private LinkedDataResolver linkedDataResolver;

	public PropertyPatternDataAccessDereferencingImpl(LinkedDataResolver linkedDataResolver) {
		this.linkedDataResolver = linkedDataResolver;
	}

	@Override
	public Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> retrieveInformationAbout(
			Collection<ARTURIResource> uriResources, Multimap<ARTURIResource, ARTNode> propertyPattern)
			throws DataAccessException {


		Collection<ARTStatement> statements = new ArrayList<ARTStatement>();

		// TODO: better quota management
		int remainingResouces = MAXIMUM_RESOURCE_COUNT_FOR_BATCH;

		for (ARTURIResource uriRes : uriResources) {
			if (remainingResouces == 0) {
				break;
			} else {
				remainingResouces--;
			}
			
			try {
				statements.addAll(linkedDataResolver.lookup(uriRes));
			} catch (ModelAccessException | IOException e) {
				// Ignores exception
			}
		}

		Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> result = new HashMap<ARTURIResource, Multimap<ARTURIResource, ARTNode>>();

		for (ARTStatement stmt : statements) {
			ARTURIResource subj = stmt.getSubject().asURIResource();
			ARTURIResource pred = stmt.getPredicate();
			ARTNode obj = stmt.getObject();

			Multimap<ARTURIResource, ARTNode> subjDescr = result.get(subj);

			if (subjDescr == null) {
				subjDescr = HashMultimap.<ARTURIResource, ARTNode> create();
				result.put(subj, subjDescr);
			}

			subjDescr.put(pred, obj);
		}

		return result;

	}

}
