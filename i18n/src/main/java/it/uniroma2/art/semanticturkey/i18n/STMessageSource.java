package it.uniroma2.art.semanticturkey.i18n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * A source of localized messages for Semantic Turkey
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STMessageSource {
	private static final MessageSource msgSource;
	static {
		ResourceBundleMessageSource tmpMsgSource = new ResourceBundleMessageSource();
		tmpMsgSource.setBasename(ResourceBundles.MESSAGES_BUNDLE);
		tmpMsgSource.setBeanClassLoader(STMessageSource.class.getClassLoader());
		tmpMsgSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
		tmpMsgSource.setFallbackToSystemLocale(false);
		tmpMsgSource.setUseCodeAsDefaultMessage(true);

		msgSource = tmpMsgSource;
	}

	public static String getMessage(String key, Object[] args, Locale locale) {
		return msgSource.getMessage(key, args, LocaleContextHolder.getLocale());
	}
	public static String getMessage(String key, Object... args) {
		return getMessage(key, args, LocaleContextHolder.getLocale());
	}
}
