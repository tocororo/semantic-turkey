package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;

import java.util.Collection;

public interface DescribeDataAccess extends DataAccess {
	
	Collection<ARTStatement> retrieveInformationAbout(Collection<ARTURIResource> uriResources) throws DataAccessException;

}
