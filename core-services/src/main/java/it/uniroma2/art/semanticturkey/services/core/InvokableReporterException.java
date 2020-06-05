package it.uniroma2.art.semanticturkey.services.core;

public class InvokableReporterException extends Exception {
	private static final long serialVersionUID = -5700075315828141720L;

	/**
	 * 
	 */
	public InvokableReporterException() {
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public InvokableReporterException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvokableReporterException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public InvokableReporterException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public InvokableReporterException(Throwable cause) {
		super(cause);
	}

}
