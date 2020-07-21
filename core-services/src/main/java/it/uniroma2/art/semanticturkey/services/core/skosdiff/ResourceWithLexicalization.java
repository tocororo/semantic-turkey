package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import java.util.ArrayList;
import java.util.List;

public class ResourceWithLexicalization {
    private String resourceIri; //SkosDiffUtils.toNTriplesString(resource)
    private List<String> resourceTypeList; // normally this should e only one value
    private List<String> lexicalizationList; // SkosDiffUtils.toNTriplesString(literal)

    @JsonCreator
    public ResourceWithLexicalization(@JsonProperty("resourceIri")String resourceIri,
            @JsonProperty("resourceTypeList")List<String> resourceTypeList,
            @JsonProperty("lexicalizationList")List<String> lexicalizationList) {
        this.resourceIri = resourceIri;
        this.resourceTypeList = resourceTypeList;
        this.lexicalizationList = lexicalizationList;
    }

    public ResourceWithLexicalization(IRI resourceIri) {
        this.resourceIri = SkosDiffUtils.toNTriplesString(resourceIri);
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
            if(existingLexicalization.equals(SkosDiffUtils.toNTriplesString(lexicalization))){
                return;
            }
        }
        lexicalizationList.add(SkosDiffUtils.toNTriplesString(lexicalization));
    }

}
