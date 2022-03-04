package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ontology {
    private String acronym;
    private String ontologyType;
    private String name;
    private List<String> hasDomain;
    private List<String> group;
    private String viewOf;

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOntologyType() {
        return ontologyType;
    }

    public void setOntologyType(String ontologyType) {
        this.ontologyType = ontologyType;
    }

    public List<String> getHasDomain() {
        return hasDomain;
    }

    public void setHasDomain(List<String> hasDomain) {
        this.hasDomain = hasDomain;
    }

    public List<String> getGroup() {
        return group;
    }

    public void setGroup(List<String> group) {
        this.group = group;
    }

    public String getViewOf() {
        return viewOf;
    }

    public void setViewOf(String viewOf) {
        this.viewOf = viewOf;
    }
}
