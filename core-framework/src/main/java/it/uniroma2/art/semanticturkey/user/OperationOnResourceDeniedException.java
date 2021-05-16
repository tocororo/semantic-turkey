package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedRuntimeException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

public class OperationOnResourceDeniedException extends InternationalizedRuntimeException {

	public OperationOnResourceDeniedException(String key, Object[] args) {
		super(key, args);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2146605441785396178L;

	public static OperationOnResourceDeniedException missingSchemeOwnership(String group) {
		return new OperationOnResourceDeniedException(
				OperationOnResourceDeniedException.class.getName() + ".messages.missing_scheme_ownership",
				new Object[] { group });
	}

	public static OperationOnResourceDeniedException missingSchemesOwnership(String group) {
		return new OperationOnResourceDeniedException(
				OperationOnResourceDeniedException.class.getName() + ".messages.missing_schemes_ownership",
				new Object[] { group });
	}

	public static OperationOnResourceDeniedException resourceModificationForbidden(IRI resource,
			String group) {
		return new OperationOnResourceDeniedException(
				OperationOnResourceDeniedException.class.getName() + ".messages.resource_modification_forbidden",
				new Object[] { NTriplesUtil.toNTriplesString(resource), group });
	}

	
	public static OperationOnResourceDeniedException resourceDeletionForbidden(IRI resource,
			String group) {
		return new OperationOnResourceDeniedException(
				OperationOnResourceDeniedException.class.getName() + ".messages.resource_deletion_forbidden",
				new Object[] { NTriplesUtil.toNTriplesString(resource), group });
	}
}
