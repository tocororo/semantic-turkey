package it.uniroma2.art.semanticturkey.services.core.resourceview;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class DatasetNotAccessibleException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7360901144305217140L;

	public DatasetNotAccessibleException(IRI identity) {
		super(DatasetNotAccessibleException.class.getName() + ".message",
				new Object[] { identity.stringValue() });
	}
}
