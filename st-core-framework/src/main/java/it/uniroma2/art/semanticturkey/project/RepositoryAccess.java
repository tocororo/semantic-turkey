package it.uniroma2.art.semanticturkey.project;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract base class of concrete repository access options: {@link CreateLocal}, {@link CreateRemote},
 * {@link AccessExistingRemote}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = CreateLocal.class, name = "CreateLocal"),
		@JsonSubTypes.Type(value = CreateRemote.class, name = "CreateRemote"),
		@JsonSubTypes.Type(value = AccessExistingRemote.class, name = "AccessExistingRemote") })
public abstract class RepositoryAccess {

	public abstract boolean isLocal();
	public abstract boolean isRemote();
	public abstract boolean isCreation();
}
