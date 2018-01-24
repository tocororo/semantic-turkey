package it.uniroma2.art.semanticturkey.resources;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * provides a unique reference to stored information addressing the system/user/project dimension
 * 
 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
public class Reference {

	private final Optional<STUser> user;
	private final Optional<Project> project;
	private final String identifier;

	public Reference(@Nullable STUser user, @Nullable Project project, String identifier) {
		super();
		this.user = Optional.ofNullable(user);
		this.project = Optional.ofNullable(project);
		this.identifier = Objects.requireNonNull(identifier, "identifier must be not null");
	}

	public Optional<STUser> getUser() {
		return user;
	}

	public Optional<Project> getProject() {
		return project;
	}

	public String getIdentifier() {
		return identifier;
	}

}
