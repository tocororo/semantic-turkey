package it.uniroma2.art.semanticturkey.properties;

public class STPropertyUpdateException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6855976479999941724L;
	
	public STPropertyUpdateException(Throwable e) {
		super(e);
	}
	
	public STPropertyUpdateException(String msg) {
		super(msg);
	}
	
}
