package it.uniroma2.art.semanticturkey.exceptions.shacl;

public class SHACLNotEnabledException extends SHACLGenericException {

    public SHACLNotEnabledException() {

    }


    @Override
    public String getMessage() {
        return "SHACL is not enabled in the selected Project";
    }

}
