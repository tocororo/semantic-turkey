package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import org.eclipse.rdf4j.model.Literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManchesterDataRange extends ManchesterClassInterface {


    private List<ManchesterClassInterface> dataConjunctionList;

    public ManchesterDataRange( List<ManchesterClassInterface> dataConjunctionList ) {
        super(PossType.DATARANGE);
        this.dataConjunctionList = dataConjunctionList;
    }

    public List<ManchesterClassInterface> getDataConjunctionList() {
        return dataConjunctionList;
    }

    public void setDataConjunctionList(List<ManchesterClassInterface> dataConjunctionList) {
        this.dataConjunctionList = dataConjunctionList;
    }

    @Override
    public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, boolean useUppercaseSyntax) {

        String manchExpr = " ( ";

        boolean first = true;
        for(ManchesterClassInterface dataConjunction : dataConjunctionList){
            if(!first){
                manchExpr += " OR ";
            }
            first = false;
            dataConjunction.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
        }
        manchExpr += " ) ";
        return manchExpr;
    }

    @Override
    public String print(String tab) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n"+tab+getType());
        for(ManchesterClassInterface manchesterDataConjunction : dataConjunctionList){
            sb.append(manchesterDataConjunction.print("\t"+tab));
        }
        return sb.toString();
    }
}
