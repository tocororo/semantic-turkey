package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.errors;

import org.eclipse.rdf4j.model.IRI;

public class ManchesterSemanticError extends ManchesterGenericError{
    private IRI resource;
    private String qname;
    private int pos;


    public ManchesterSemanticError(String msg, IRI resource, String qname, int pos) {
        super(msg);
        this.resource = resource;
        this.qname = qname;
        this.pos = pos;
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
        return true;
    }
}
