package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

public class SHACLTargetShapeNotExistingException extends SHACLGenericException {
    private IRI targetShape;

    public SHACLTargetShapeNotExistingException(IRI targetShape) {
        this.targetShape = targetShape;
    }


    @Override
    public String getMessage() {
        return NTriplesUtil.toNTriplesString(targetShape)+" does not exist";
    }

    public IRI getTargetShape() {
        return targetShape;
    }
}
