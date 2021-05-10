package it.uniroma2.art.semanticturkey.properties;

import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Locale;

public class InvalidSettingsUpdateException extends STPropertyUpdateException {

    private final String interpolatedMessage;

    public InvalidSettingsUpdateException(String interpolatedMessage) {
        this.interpolatedMessage = interpolatedMessage;
    }

    @Override
    public String getMessage() {
        return STMessageSource.getMessage(InvalidSettingsUpdateException.class.getName() + ".message", new Object[] {interpolatedMessage}, Locale.ROOT);
    }

    @Override
    public String getLocalizedMessage() {
        return STMessageSource.getMessage(InvalidSettingsUpdateException.class.getName() + ".message", new Object[] {interpolatedMessage});
    }
}
