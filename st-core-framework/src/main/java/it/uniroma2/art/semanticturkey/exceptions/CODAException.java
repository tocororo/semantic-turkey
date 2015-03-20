package it.uniroma2.art.semanticturkey.exceptions;

public class CODAException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9017530510063968088L;

	public CODAException(){
		super();
	}
	
	public CODAException(String msg){
		super(msg);
	}

	public CODAException(Throwable e){
		super(e);
	}
	
}
