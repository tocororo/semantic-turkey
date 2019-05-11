package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model;

import org.eclipse.rdf4j.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import it.uniroma2.art.semanticturkey.utilities.RDFXML2ModelConverter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ODPDatasetSearchResult {
	@JsonDeserialize(converter = RDFXML2ModelConverter.class)
	private Model rdf;

	public Model getRdf() {
		return rdf;
	}

	public void setRdf(Model rdf) {
		this.rdf = rdf;
	}
}
