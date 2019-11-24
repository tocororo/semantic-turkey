package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import org.eclipse.rdf4j.model.Literal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManchesterDatatypeRestriction extends ManchesterClassInterface {

    private ManchesterClassInterface datatypeMCI;
    private List<String> facetStringList;
    private List<Literal> literalList;

    public ManchesterDatatypeRestriction(ManchesterClassInterface dataTypeMCI, List<String> facetStringList, List<Literal> literalList) {
        super(PossType.DATATYPERESTRICTION);
        this.datatypeMCI = dataTypeMCI;
        this.facetStringList = facetStringList;
        this.literalList = literalList;

    }

    public ManchesterClassInterface getDatatypeMCI() {
        return datatypeMCI;
    }

    public List<String> getFacetStringList() {
        return facetStringList;
    }

    public List<Literal> getLiteralList() {
        return literalList;
    }

    @Override
    public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, boolean useUppercaseSyntax) {
        String manchExpr = datatypeMCI.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
        manchExpr += " [ ";
        boolean first = true;
        for(int i=0; i<facetStringList.size(); ++i){
            if(!first){
                manchExpr += ", ";
            }
            first = false;
            if(useUppercaseSyntax){
                manchExpr += facetStringList.get(i).toUpperCase() + " ";
            } else {
                manchExpr += facetStringList.get(i).toLowerCase()+ " ";
            }
            manchExpr += printLiteral(getPrefixName, namespaceToPrefixsMap, literalList.get(i));
        }
        manchExpr += " ] ";
        return manchExpr;
    }

    @Override
    public String print(String tab) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n"+tab+getType());
        sb.append(datatypeMCI.print("\t"+tab));
        for(int i=0; i<facetStringList.size(); ++i){
            String facet = facetStringList.get(i);
            sb.append("\n"+tab+"\tFACET: "+facet);
            sb.append("\n" + tab + "\t" + printLiteral(false, new HashMap<String, String>(), literalList.get(i)));
        }
        return sb.toString();
    }
}
