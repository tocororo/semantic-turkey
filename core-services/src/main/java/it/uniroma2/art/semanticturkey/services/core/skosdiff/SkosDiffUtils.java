package it.uniroma2.art.semanticturkey.services.core.skosdiff;


import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SkosDiffUtils {
    private static SkosDiffUtils skosDiff = null;

    private SkosDiffUtils(){}

    public static SkosDiffUtils getInstance(){
        if(skosDiff==null){
            skosDiff = new SkosDiffUtils();
        }
        return skosDiff;
    }


    public String generateRandId(String endpoint1, String endpoint2, List<String> existingIdList){
        boolean newStringGenerated = false;
        String generatedString="";
        while(!newStringGenerated) {
            generatedString = DigestUtils.sha1Hex(endpoint1+endpoint2+Math.random()).substring(0, 20);
            if(!existingIdList.contains(generatedString)){
                newStringGenerated = true;
            }
        }
        return generatedString;
    }

    public static IRI createIRI(String iriString){
        if(iriString.startsWith("<")){
            iriString = iriString.substring(1, iriString.length()-1);
        }
        return SimpleValueFactory.getInstance().createIRI(iriString);
    }

    public enum TaskStatus {completed, execution, error}

    public enum LexicalizationType {
        SKOS_CORE("http://www.w3.org/2004/02/skos/core"),
        SKOS_XL("http://www.w3.org/2008/05/skos-xl");
        private String value;

        LexicalizationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static LexicalizationType fromValue(String text) {
            for (LexicalizationType lexicalizationType : LexicalizationType.values()) {
                if (String.valueOf(lexicalizationType.value).equals(text)) {
                    return lexicalizationType;
                }
            }
            return null;
        }
    }

    public static String toNTriplesString(Value value) {
        String valueString = "";
        if(value instanceof IRI) {
            //"<" + NTriplesUtil.escapeString(value.toString()) + ">";
            valueString = NTriplesUtil.toNTriplesString(value);
        } else if(value instanceof BNode) {
            valueString = NTriplesUtil.toNTriplesString(value);
        }
        else if (value instanceof Literal) {
            Literal literal = (Literal) value;
            //valueString = "\""+ NTriplesUtil.escapeString(literal.getLabel()) + "\"";
            valueString = "\""+ literal.getLabel() + "\"";
            if(Literals.isLanguageLiteral(literal)) {
                valueString += "@"+literal.getLanguage().get();
            } else {
                valueString += "^^"+toNTriplesString(literal.getDatatype());
            }
        } else {
            throw new IllegalArgumentException("Unknown value type: " + value.getClass());
        }
        return valueString;
    }

    public static String prettyPrintTime(long millis) {
        String prettyPrint;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minute = TimeUnit.MILLISECONDS.toMinutes(millis) - (TimeUnit.MILLISECONDS.toHours(millis)* 60);
        long second = TimeUnit.MILLISECONDS.toSeconds(millis) - (TimeUnit.MILLISECONDS.toMinutes(millis) *60);
        prettyPrint = String.format("%02d h %02d m %02d s", hours, minute, second);
        return prettyPrint;
    }

    public static long getTime() {
        return  System.currentTimeMillis();
    }
}
