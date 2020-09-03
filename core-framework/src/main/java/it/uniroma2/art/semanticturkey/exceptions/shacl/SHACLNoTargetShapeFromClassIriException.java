package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

public class SHACLNoTargetShapeFromClassIriException extends SHACLGenericException {
    private IRI classIri;

    public SHACLNoTargetShapeFromClassIriException(IRI classIri) {
        this.classIri = classIri;
    }


    @Override
    public String getMessage() {
        return NTriplesUtil.toNTriplesString(classIri)+" does not exist";
    }

    public IRI getClassIri() {
        return classIri;
    }
}
