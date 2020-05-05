package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

public class SHACLNotEnabledException extends SHACLGenericException {

    public SHACLNotEnabledException() {

    }


    @Override
    public String getMessage() {
        return "SHACL is not enabled in the selected Project";
    }

}
