package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model;

import java.util.List;

import com.google.common.base.MoreObjects;

public class SearchFacet {

	private List<FacetItem> items;
	private String title;

	public List<FacetItem> getItems() {
		return items;
	}

	public void setItems(List<FacetItem> items) {
		this.items = items;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("items", items).add("title", title).toString();
	}

}
