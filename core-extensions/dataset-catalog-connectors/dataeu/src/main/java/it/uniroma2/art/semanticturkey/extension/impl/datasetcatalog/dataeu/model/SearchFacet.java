package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

public class SearchFacet {

    private String id;
    @JsonDeserialize(using=LocalizedStringDeserializer.class)
    private Map<String, String> title;
    private List<FacetItem> items;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public List<FacetItem> getItems() {
        return items;
    }

    public void setItems(List<FacetItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", id).add("items", items).add("title", title).toString();
    }

}
