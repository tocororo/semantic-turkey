package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.List;
import java.util.Map;

public class ManchesterDataConjunction extends ManchesterClassInterface {

    private List<ManchesterClassInterface> dataPrimaryList;

    public ManchesterDataConjunction(List<ManchesterClassInterface> dataPrimaryList) {
        super(PossType.DATACONJUCTION);
        this.dataPrimaryList = dataPrimaryList;
    }

    public List<ManchesterClassInterface> getDataPrimaryList() {
        return dataPrimaryList;
    }

    @Override
    public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, boolean useUppercaseSyntax) {
        String manchExpr = "";

        boolean first = true;
        for(ManchesterClassInterface dataPrimary : dataPrimaryList){
            if(!first){
                manchExpr += " AND ";
            }
            first = false;
            dataPrimary.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
        }
        return manchExpr;
    }

    @Override
    public String print(String tab) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n"+tab+getType());
        for(ManchesterClassInterface manchesterdataPrimay : dataPrimaryList){
            sb.append(manchesterdataPrimay.print("\t"+tab));
        }
        return sb.toString();
    }
}
