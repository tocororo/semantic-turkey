package it.uniroma2.art.semanticturkey.exceptions;

public class InvalidAccountDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8035581990582540026L;
	
	public InvalidAccountDataException(){
		super();
	}
	
	public InvalidAccountDataException(String msg){
		super(msg);
	}

	public InvalidAccountDataException(Throwable e){
		super(e);
	}

}
