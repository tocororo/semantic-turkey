package it.uniroma2.art.semanticturkey.extension.extpts.metadatarepository;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import com.google.common.base.MoreObjects;

public class DatasetSearchResult {
	private String id;
	private IRI ontologyIRI;
	private URL datasetPage;
	private double score;
	private List<Literal> titles;
	private List<Literal> descriptions;
	private Map<String, List<String>> facets;

	public DatasetSearchResult(String id, IRI ontologyIRI, double score, URL datasetPage,
			List<Literal> titles, List<Literal> descriptions, Map<String, List<String>> facets) {
		this.id = id;
		this.ontologyIRI = ontologyIRI;
		this.score = score;
		this.datasetPage = datasetPage;
		this.titles = titles;
		this.descriptions = descriptions;
		this.facets = facets;
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

	public double getScore() {
		return score;
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

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("id", id).add("ontologyIRI", ontologyIRI)
				.add("score", score).add("datasetPage", datasetPage).add("titles", titles)
				.add("descriptions", descriptions).add("facets", facets).toString();
	}
}
