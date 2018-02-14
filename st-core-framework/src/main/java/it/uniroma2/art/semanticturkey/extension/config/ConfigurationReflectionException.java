package it.uniroma2.art.semanticturkey.extension.config;

public class ConfigurationReflectionException extends RuntimeException {

	private static final long serialVersionUID = -296004768736444186L;

	public ConfigurationReflectionException() {
		super();
	}

	public ConfigurationReflectionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConfigurationReflectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationReflectionException(String message) {
		super(message);
	}

	public ConfigurationReflectionException(Throwable cause) {
		super(cause);
	}

}
