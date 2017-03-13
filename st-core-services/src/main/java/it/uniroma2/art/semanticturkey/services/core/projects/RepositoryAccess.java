package it.uniroma2.art.semanticturkey.services.core.projects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
		@JsonSubTypes.Type(value = CreateLocal.class, name = "CreateLocal"),
		@JsonSubTypes.Type(value = CreateRemote.class, name = "CreateRemote"),
		@JsonSubTypes.Type(value = AccessExistingRemote.class, name = "AccessExistingRemote")
})
public abstract class RepositoryAccess {
}
