package it.uniroma2.art.semanticturkey.properties;

import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Locale;

public class InvalidConfigurationUpdateException extends STPropertyUpdateException {
    private final String interpolatedMessage;

    public InvalidConfigurationUpdateException(String interpolatedMessage) {
        this.interpolatedMessage = interpolatedMessage;
    }

    @Override
    public String getMessage() {
        return STMessageSource.getMessage(InvalidConfigurationUpdateException.class.getName() + ".message", new Object[] {interpolatedMessage}, Locale.ROOT);
    }

    @Override
    public String getLocalizedMessage() {
        return STMessageSource.getMessage(InvalidConfigurationUpdateException.class.getName() + ".message", new Object[] {interpolatedMessage});
    }}
