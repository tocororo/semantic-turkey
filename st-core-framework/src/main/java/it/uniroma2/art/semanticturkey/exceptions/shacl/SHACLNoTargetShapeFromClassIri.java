package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

public class SHACLNoTargetShapeFromClassIri extends SHACLGenericException {
    private IRI classIri;

    public SHACLNoTargetShapeFromClassIri(IRI classIri) {
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
