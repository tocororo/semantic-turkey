package it.uniroma2.art.semanticturkey.i18n;

import java.util.Locale;

/**
 * Convenience base class for designing internationalized runtime exceptions.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class InternationalizedRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 5821341171105735301L;

	private final String key;
	private final Object[] args;

	public InternationalizedRuntimeException(Throwable cause) {
		this(null, null, cause);
	}

	public InternationalizedRuntimeException(String key, Object[] args) {
		this(key, args, null);
	}

	public InternationalizedRuntimeException(String key, Object[] args, Throwable cause) {
		super(cause);
		this.key = key;
		this.args = args;
	}

	@Override
	public String getMessage() {
		return key != null ? STMessageSource.getMessage(key, args, Locale.ROOT) : null;
	}

	@Override
	public String getLocalizedMessage() {
		return key != null ? STMessageSource.getMessage(key, args) : null;
	}

}
