package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

public class SHACLTargetShapeNotExistingException extends SHACLGenericException {
	
	private static final long serialVersionUID = -5694320790418231971L;
	
	private IRI targetShape;

	public SHACLTargetShapeNotExistingException(IRI targetShape) {
		super(SHACLTargetShapeNotExistingException.class.getName() + ".message",
				new Object[] { NTriplesUtil.toNTriplesString(targetShape) });
		this.targetShape = targetShape;
	}

	public IRI getTargetShape() {
		return targetShape;
	}
}
