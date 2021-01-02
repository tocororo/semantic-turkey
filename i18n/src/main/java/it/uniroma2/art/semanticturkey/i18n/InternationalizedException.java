package it.uniroma2.art.semanticturkey.i18n;

import java.util.Locale;

/**
 * Convenience base class for designing internationalized exceptions. Its use is not mandatory, since a
 * concrete class might implement {@link Exception#getMessage()} and {@link Exception#getLocalizedMessage()}
 * directly.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class InternationalizedException extends Exception {

	private static final long serialVersionUID = 5821341171105735301L;

	private final String key;
	private final Object[] args;

	public InternationalizedException(String key, Object[] args, Throwable cause) {
		super(cause);
		this.key = key;
		this.args = args;
	}

	@Override
	public String getMessage() {
		return STMessageSource.getMessage(key, args, Locale.ROOT);
	}

	@Override
	public String getLocalizedMessage() {
		return STMessageSource.getMessage(key, args);
	}

}
