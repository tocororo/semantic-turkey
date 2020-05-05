package it.uniroma2.art.semanticturkey.config;

public class InvalidConfigurationException extends Exception {

	private static final long serialVersionUID = 6091042689041760439L;

	public InvalidConfigurationException() {
		super();
	}

	public InvalidConfigurationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidConfigurationException(String message) {
		super(message);
	}

	public InvalidConfigurationException(Throwable cause) {
		super(cause);
	}

}
