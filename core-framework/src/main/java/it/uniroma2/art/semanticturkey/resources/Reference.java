package it.uniroma2.art.semanticturkey.resources;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * provides a unique reference to stored information addressing the system/user/project dimension
 * 
 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
@JsonSerialize(using = Reference.ReferenceSerializer.class)
public class Reference {

	private final Optional<Project> project;
	private final Optional<STUser> user;
	private final String identifier;

	public Reference(@Nullable Project project, @Nullable STUser user, String identifier) {
		super();
		this.project = Optional.ofNullable(project);
		this.user = Optional.ofNullable(user);
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

	@JsonIgnore
	public String getRelativeReference() {
		return Scope.computeScope(this.project.orElse(null), this.user.orElse(null)).getSerializationCode() + ":"
				+ this.identifier;
	}

	public static Collection<Reference> liftIdentifiers(@Nullable Project project, @Nullable STUser user,
			Collection<String> identifiers) {
		return identifiers.stream().map(identifier -> new Reference(project, user, identifier))
				.collect(Collectors.toList());
	}

	public static class ReferenceSerializer extends StdSerializer<Reference> {

		private static final long serialVersionUID = 1L;

		protected ReferenceSerializer(Class<Reference> t) {
			super(t);
		}

		protected ReferenceSerializer() {
			super(Reference.class);
		}

		@Override
		public void serialize(Reference value, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			gen.writeStartObject();

			gen.writeStringField("user", value.user.map(STUser::getUsername).orElse(null));
			gen.writeStringField("project", value.project.map(Project::getName).orElse(null));
			gen.writeStringField("identifier", value.identifier);
			gen.writeStringField("relativeReference", value.getRelativeReference());
			gen.writeEndObject();
		}

	}
}
