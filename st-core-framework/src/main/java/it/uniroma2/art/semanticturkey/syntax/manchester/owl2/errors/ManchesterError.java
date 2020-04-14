package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.errors;

import org.eclipse.rdf4j.model.IRI;

public class ManchesterError {
    private String msg;
    private IRI resource;
    private String qname;
    private int pos;
    private boolean isSemanticError;

    public  ManchesterError(String msg) {
        this.msg = msg;
        isSemanticError = false;
    }

    public ManchesterError(String msg, IRI resource, String qname, int pos) {
        this.msg = msg;
        this.resource = resource;
        this.qname = qname;
        this.pos = pos;
        this.isSemanticError = true;
    }

    public String getMsg() {
        return msg;
    }

    public IRI getResource() {
        return resource;
    }

    public String getQname() {
        return qname;
    }

    public int getPos() {
        return pos;
    }

    public boolean isSemanticError() {
        return isSemanticError;
    }
}
