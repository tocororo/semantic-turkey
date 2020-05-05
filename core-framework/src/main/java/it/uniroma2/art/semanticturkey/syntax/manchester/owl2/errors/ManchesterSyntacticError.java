package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.errors;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used for syntactic errors in a Manchester Expression (prefix used but not defined is considered a Syntactic error)
 */
public class ManchesterSyntacticError extends ManchesterGenericError{
    private int pos;
    private String offendingTerm;
    private List<String> exptectedTokenList;
    private String prefix; // instead of offendedToken and exptectedTokenList

    public ManchesterSyntacticError(String msg, int pos, String offendingTerm, List<String> exptectedTokenList) {
        super(msg);
        this.pos = pos;
        this.offendingTerm = offendingTerm;
        this.exptectedTokenList = exptectedTokenList;
    }


    public ManchesterSyntacticError(String msg, String prefix, int pos) {
        super(msg);
        this.pos = pos;
        this.prefix = prefix;
        this.exptectedTokenList = new ArrayList<>();
    }

    public boolean isSemanticError() {
        return false;
    }

    public int getPos() {
        return pos;
    }

    public String getOffendingTerm() {
        return offendingTerm;
    }

    public List<String> getExptectedTokenList() {
        return exptectedTokenList;
    }

    public String getPrefix() {
        return prefix;
    }

}
