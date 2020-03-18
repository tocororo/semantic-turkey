package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

public class SHACLTargetShapeNotExisting extends SHACLGenericException {
    private IRI targetShape;

    public SHACLTargetShapeNotExisting(IRI targetShape) {
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
