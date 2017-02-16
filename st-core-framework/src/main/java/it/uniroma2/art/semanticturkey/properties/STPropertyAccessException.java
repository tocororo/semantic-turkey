package it.uniroma2.art.semanticturkey.properties;

import java.io.File;

public class STPropertyAccessException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6855976479999941724L;
	
	public STPropertyAccessException(Throwable e) {
		super(e);
	}
	
	public STPropertyAccessException(String msg) {
		super(msg);
	}
	
	public STPropertyAccessException(File file, Throwable e) {
		super("Failed in updating property file: " + file + " due to exception: " + e + ":\n" + e.getMessage());
	}

}
