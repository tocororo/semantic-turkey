package it.uniroma2.art.semanticturkey.services.core.history;

import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;

/**
 * Resuming metadata about a commit.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class CommitInfo {
	private IRI commit;
	private AnnotatedValue<IRI> user;
	private AnnotatedValue<IRI> operation;
	private GregorianCalendar startTime;
	private GregorianCalendar endTime;
	private List<ParameterInfo> operationParameters;

	
	
	public AnnotatedValue<IRI> getUser() {
		return user;
	}

	public void setUser(AnnotatedValue<IRI> user) {
		this.user = user;
	}

	public AnnotatedValue<IRI> getOperation() {
		return operation;
	}

	public void setOperation(AnnotatedValue<IRI> operation) {
		this.operation = operation;
	}

	public void setOperationParameters(List<ParameterInfo> operationParameters) {
		this.operationParameters = operationParameters;
	}

	public List<ParameterInfo> getOperationParameters() {
		return operationParameters;
	}

	@JsonSerialize(converter=IRI2StringConverter.class)
	public IRI getCommit() {
		return commit;
	}

	public void setCommit(IRI commit) {
		this.commit = commit;
	}

	public void setStartTime(GregorianCalendar startTime) {
		this.startTime = startTime;
	}

	@JsonFormat(shape=Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public GregorianCalendar getStartTime() {
		return startTime;
	}
	
	public void setEndTime(GregorianCalendar endTime) {
		this.endTime = endTime;
	}

	@JsonFormat(shape=Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public GregorianCalendar getEndTime() {
		return endTime;
	}

}
