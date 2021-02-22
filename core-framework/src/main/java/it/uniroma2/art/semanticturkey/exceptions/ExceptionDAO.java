package it.uniroma2.art.semanticturkey.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDAO {
    private final String type;
    private final String message;
    private final String stacktrace;

    /**
     * @param type
     * @param message
     * @param stacktrace
     */
    public ExceptionDAO(String type, String message, String stacktrace) {
        this.type = type;
        this.message = message;
        this.stacktrace = stacktrace;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the stacktrace
     */
    public String getStacktrace() {
        return stacktrace;
    }

    public static ExceptionDAO valueOf(Exception e) {
        if (e == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return new ExceptionDAO(e.getClass().getSimpleName(), e.getMessage(), writer.toString());
    }
}
