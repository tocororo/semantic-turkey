package it.uniroma2.art.semanticturkey.exceptions;

public class MalformedURIException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 105894146480755775L;
	
	public MalformedURIException(){
		super();
	}
	
	public MalformedURIException(String msg){
		super(msg);
	}

	public MalformedURIException(Throwable e){
		super(e);
	}
}
