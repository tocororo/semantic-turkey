package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Multimap;

public interface PropertyPatternDataAccess extends DataAccess {

	Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> retrieveInformationAbout(Collection<ARTURIResource> uriResources,
			Multimap<ARTURIResource, ARTNode> propertyPattern) throws DataAccessException;
}
