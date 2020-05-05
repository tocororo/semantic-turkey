package it.uniroma2.art.semanticturkey.customservice;

public abstract class CustomServiceException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public CustomServiceException() {
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public CustomServiceException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CustomServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CustomServiceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public CustomServiceException(Throwable cause) {
		super(cause);
	}

}
