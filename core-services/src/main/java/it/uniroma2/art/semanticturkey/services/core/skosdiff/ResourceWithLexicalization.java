package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.util.ArrayList;
import java.util.List;

public class ResourceWithLexicalization {
    private String resourceIri; //NTriplesUtil.toNTriplesString(resource)
    private List<String> resourceTypeList; // normally this should e only one value
    private List<String> lexicalizationList; // NTriplesUtil.toNTriplesString(literal)

    public ResourceWithLexicalization() {
    }

    public ResourceWithLexicalization(IRI resourceIri) {
        this.resourceIri = NTriplesUtil.toNTriplesString(resourceIri);
        this.resourceTypeList = new ArrayList<>();
        this.lexicalizationList = new ArrayList<>();
    }

    public String getResourceIri() {
        return resourceIri;
    }

    public List<String> getResourceTypeList() {
        return resourceTypeList;
    }

    public void addResourceType(String resourceType){
       if(!resourceTypeList.contains(resourceType)){
           resourceTypeList.add(resourceType);
       }
    }

    public List<String> getLexicalizationList() {
        return lexicalizationList;
    }

    public  void addLexicalization(Literal lexicalization){
        for(String existingLexicalization : lexicalizationList){
            if(existingLexicalization.equals(NTriplesUtil.toNTriplesString(lexicalization))){
                return;
            }
        }
        lexicalizationList.add(NTriplesUtil.toNTriplesString(lexicalization));
    }

}
