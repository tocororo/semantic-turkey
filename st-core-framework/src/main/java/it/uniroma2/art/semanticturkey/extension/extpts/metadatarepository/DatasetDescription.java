package it.uniroma2.art.semanticturkey.extension.extpts.metadatarepository;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import com.google.common.base.MoreObjects;

public class DatasetDescription {
	private String id;
	private IRI ontologyIRI;
	private URL datasetPage;
	private List<Literal> titles;
	private List<Literal> descriptions;
	private Map<String, List<String>> facets;
	private String uriPrefix;
	private URL dataDump;
	private URL sparqlEndpoint;
	private IRI model;
	private IRI lexicalizationModel;

	public DatasetDescription(String id, IRI ontologyIRI, URL datasetPage, List<Literal> titles,
			List<Literal> descriptions, Map<String, List<String>> facets, String uriPrefix, URL dataDump,
			URL sparqlEndpoint, IRI model, IRI lexicalizationModel) {
		this.id = id;
		this.ontologyIRI = ontologyIRI;
		this.datasetPage = datasetPage;
		this.titles = titles;
		this.descriptions = descriptions;
		this.facets = facets;
		this.uriPrefix = uriPrefix;
		this.dataDump = dataDump;
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

	public URL getDataDump() {
		return dataDump;
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
				.add("facets", facets).add("uriPrefix", uriPrefix).add("dataDump", dataDump)
				.add("sparqlEndpoint", sparqlEndpoint).add("model", model)
				.add("lexicalizationModel", lexicalizationModel).toString();
	}

}
