package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class VocabularySearchResult {

	@JsonProperty("_index")
	private String index;
	
	@JsonProperty("_type")
	private String type;

	@JsonProperty("_id")
	private String id;

	@JsonProperty("_score")
	private double score;

	@JsonProperty("_source")
	private VocabularySource source;

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public VocabularySource getSource() {
		return source;
	}

	public void setSource(VocabularySource source) {
		this.source = source;
	}

	
}
