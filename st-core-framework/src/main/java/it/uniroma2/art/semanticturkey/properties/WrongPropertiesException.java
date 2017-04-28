package it.uniroma2.art.semanticturkey.properties;

/**
 * Have to change the name of this class, it comes from the old BadConfigurationException, and need to make
 * more clear
 * 
 * @author Armando Stellato
 *
 */
public class WrongPropertiesException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4759454918851129781L;

	public WrongPropertiesException(String message) {
		super(message);
	}

	public WrongPropertiesException(Throwable cause) {
		super(cause);
	}

	public WrongPropertiesException(String message, Throwable cause) {
		super(message, cause);
	}

}
