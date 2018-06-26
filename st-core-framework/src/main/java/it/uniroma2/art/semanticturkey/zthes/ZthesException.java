package it.uniroma2.art.semanticturkey.zthes;

public class ZthesException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1394309398823228398L;

	public ZthesException(){
		super();
	}
	
	public ZthesException(String msg){
		super(msg);
	}

	public ZthesException(Throwable e){
		super(e);
	}
	
}
