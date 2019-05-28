package it.uniroma2.art.semanticturkey.services.core.genoma.backend;

import java.net.URL;
import java.util.Date;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;

public class MatchingStatus {
	private String id;
	@JsonSerialize(converter = IRI2StringConverter.class)
	@JsonDeserialize(converter = String2IRIConverter.class)
	private IRI ontology1;
	@JsonSerialize(converter = IRI2StringConverter.class)
	@JsonDeserialize(converter = String2IRIConverter.class)
	private IRI ontology2;
	private URL engine;
	private String status;
	@JsonFormat(shape=Shape.STRING, pattern="EEE MMM dd HH:mm:ss Z yyyy", locale="us")
	private Date startTime;
	@JsonFormat(shape=Shape.STRING, pattern="EEE MMM dd HH:mm:ss Z yyyy", locale="us")
	private Date endTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public IRI getOntology1() {
		return ontology1;
	}
	
	public void setOntology1(IRI ontology1) {
		this.ontology1 = ontology1;
	}
	
	public IRI getOntology2() {
		return ontology2;
	}
	
	public void setOntology2(IRI ontology2) {
		this.ontology2 = ontology2;
	}

	public URL getEngine() {
		return engine;
	}

	public void setEngine(URL engine) {
		this.engine = engine;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
}
