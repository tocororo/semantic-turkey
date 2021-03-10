package it.uniroma2.art.semanticturkey.exceptions.shacl;

public class SHACLNotEnabledException extends SHACLGenericException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -6823740185761531331L;


	public SHACLNotEnabledException() {
    	super(SHACLNotEnabledException.class.getName() + ".message", new Object[0]);
    }

}
