package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

public class AlignmentServiceException extends Exception {

	/**
	 * 
	 */
	public AlignmentServiceException() {
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public AlignmentServiceException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AlignmentServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public AlignmentServiceException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

	public AlignmentServiceException(String message) {
		super(message);
	}
}
