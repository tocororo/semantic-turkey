package it.uniroma2.art.semanticturkey.plugin.configuration;

public class UnsupportedPluginConfigurationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4315063693795455444L;

	public UnsupportedPluginConfigurationException() {
		super();
	}

	public UnsupportedPluginConfigurationException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnsupportedPluginConfigurationException(String message,
			Throwable cause) {
		super(message, cause);
	}

	public UnsupportedPluginConfigurationException(String message) {
		super(message);
	}

	public UnsupportedPluginConfigurationException(Throwable cause) {
		super(cause);
	}

}
