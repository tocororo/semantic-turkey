package it.uniroma2.art.semanticturkey.services.core.history;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

/**
 * Resuming metadata about a commit.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class CommitInfo {
	private IRI commit;
	private AnnotatedValue<IRI> user;
	private AnnotatedValue<IRI> operation;
	private AnnotatedValue<Resource> subject;

	
	
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

	public AnnotatedValue<Resource> getSubject() {
		return subject;
	}

	public void setSubject(AnnotatedValue<Resource> subject) {
		this.subject = subject;
	}

	@JsonSerialize(converter=IRI2StringConverter.class)
	public IRI getCommit() {
		return commit;
	}

	public void setCommit(IRI commit) {
		this.commit = commit;
	}

}
