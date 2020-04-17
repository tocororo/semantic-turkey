package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.errors;

public abstract class ManchesterGenericError {
    private String msg;
 
    public ManchesterGenericError(String msg) {
        this.msg = msg;
    }
 
    public abstract boolean isSemanticError();

    public String getMsg() {
        return msg;
    }
}
