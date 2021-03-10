package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

public class SHACLNoTargetShapeFromClassIriException extends SHACLGenericException {
	private static final long serialVersionUID = 4637975120202463678L;
	private IRI classIri;

	public SHACLNoTargetShapeFromClassIriException(IRI classIri) {
		super(SHACLNoTargetShapeFromClassIriException.class.getName() + ".message",
				new Object[] { NTriplesUtil.toNTriplesString(classIri) });
		this.classIri = classIri;
	}

	public IRI getClassIri() {
		return classIri;
	}
}
