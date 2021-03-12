package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class DatasetDescription {
	private String id;
	private IRI ontologyIRI;
	private URL datasetPage;
	private List<Literal> titles;
	private List<Literal> descriptions;
	private Map<String, List<String>> facets;
	private String uriPrefix;
	private List<DownloadDescription> dataDumps;
	private URL sparqlEndpoint;
	private IRI model;
	private IRI lexicalizationModel;

	@JsonCreator
	public DatasetDescription(@JsonProperty("id") String id, @JsonProperty("ontologyIRI") IRI ontologyIRI,
			@JsonProperty("datasetPage") URL datasetPage, @JsonProperty("titles") List<Literal> titles,
			@JsonProperty("descriptions") List<Literal> descriptions,
			@JsonProperty("facets") Map<String, List<String>> facets,
			@JsonProperty("uriPrefix") String uriPrefix,
			@JsonProperty("dataDumps") List<DownloadDescription> dataDumps,
			@JsonProperty("sparqlEndpoint") URL sparqlEndpoint, @JsonProperty("model") IRI model,
			@JsonProperty("lexicalizationModel") IRI lexicalizationModel) {
		this.id = id;
		this.ontologyIRI = ontologyIRI;
		this.datasetPage = datasetPage;
		this.titles = titles;
		this.descriptions = descriptions;
		this.facets = facets;
		this.uriPrefix = uriPrefix;
		this.dataDumps = dataDumps;
		this.sparqlEndpoint = sparqlEndpoint;
		this.model = model;
		this.lexicalizationModel = lexicalizationModel;
	}

	public String getId() {
		return id;
	}

	public IRI getOntologyIRI() {
		return ontologyIRI;
	}

	public URL getDatasetPage() {
		return datasetPage;
	}

	public List<Literal> getTitles() {
		return titles;
	}

	public List<Literal> getDescriptions() {
		return descriptions;
	}

	public Map<String, List<String>> getFacets() {
		return facets;
	}

	public String getUriPrefix() {
		return uriPrefix;
	}

	public List<DownloadDescription> getDataDumps() {
		return dataDumps;
	}

	public URL getSparqlEndpoint() {
		return sparqlEndpoint;
	}

	public IRI getModel() {
		return model;
	}

	public IRI getLexicalizationModel() {
		return lexicalizationModel;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("id", id).add("ontologyIRI", ontologyIRI)
				.add("datasetPage", datasetPage).add("titles", titles).add("descriptions", descriptions)
				.add("facets", facets).add("uriPrefix", uriPrefix).add("dataDumps", dataDumps)
				.add("sparqlEndpoint", sparqlEndpoint).add("model", model)
				.add("lexicalizationModel", lexicalizationModel).toString();
	}

}
