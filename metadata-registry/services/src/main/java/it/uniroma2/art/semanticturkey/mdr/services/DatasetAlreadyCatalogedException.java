package it.uniroma2.art.semanticturkey.mdr.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.queryrender.RenderUtils;

import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;

public class DatasetAlreadyCatalogedException extends DeniedOperationException {

	private static final long serialVersionUID = -3684883130516771582L;

	public DatasetAlreadyCatalogedException(IRI datasetIri, IRI datasetMetadataIdentity) {
		super(DatasetAlreadyCatalogedException.class.getName() + ".message",
				new Object[] { RenderUtils.toSPARQL(datasetIri), datasetMetadataIdentity.stringValue() });
	}

}
