package it.uniroma2.art.semanticturkey.exceptions.manchester;

/**
 * @author Andrea Turbati
 *
 */
public class ManchesterPrefixNotDefinedRuntimeException extends RuntimeException{

    private String prefix;

    public ManchesterPrefixNotDefinedRuntimeException(String prefix) {
        super("There is no prefix for the namespace: "+prefix);
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
