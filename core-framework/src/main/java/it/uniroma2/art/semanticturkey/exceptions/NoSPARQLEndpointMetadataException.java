package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class NoSPARQLEndpointMetadataException extends DatasetMetadataException {

	private static final long serialVersionUID = 4801132399639013355L;

	public NoSPARQLEndpointMetadataException(IRI identity) {
		super(NoSPARQLEndpointMetadataException.class.getName() + ".message", new Object[] { identity });
	}
}
