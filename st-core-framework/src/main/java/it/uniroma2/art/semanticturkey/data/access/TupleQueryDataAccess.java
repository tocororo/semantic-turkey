package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;

import java.util.Collection;

public interface TupleQueryDataAccess extends DataAccess {
	public TupleBindingsIterator retrieveInformationAbout(Collection<ARTURIResource> uriResources, String graphPattern) throws DataAccessException;
}
