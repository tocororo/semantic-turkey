package it.uniroma2.art.semanticturkey.project;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class STRepositoryInfo {
	private final String backendType;
	private final String username;
	private final String password;
	private final IsolationLevel defaultReadIsolationLevel;
	private final IsolationLevel defaultWriteIsolationLevel;
	private final SearchStrategies searchStrategy;

	public static enum SearchStrategies {
		REGEX, GRAPH_DB
	}

	@JsonCreator
	public STRepositoryInfo(@JsonProperty("backendType") String backendType,
			@JsonProperty("username") String username, @JsonProperty("password") String password,
			@JsonProperty("defaultReadIsolationLevel") @JsonDeserialize(using = IsolationLevelDeserializer.class) IsolationLevel defaultReadIsolationLevel,
			@JsonProperty("defaultWriteIsolationLevel") @JsonDeserialize(using = IsolationLevelDeserializer.class) IsolationLevel defaultWriteIsolationLevel,
			@JsonProperty("searchStrategy") SearchStrategies searchStrategy) {
		this.backendType = backendType;
		this.username = username;
		this.password = password;
		this.defaultReadIsolationLevel = defaultReadIsolationLevel;
		this.defaultWriteIsolationLevel = defaultWriteIsolationLevel;
		this.searchStrategy = searchStrategy;
	}

	public @Nullable String getBackendType() {
		return backendType;
	}

	public @Nullable String getUsername() {
		return username;
	}

	public @Nullable String getPassword() {
		return password;
	}

	@JsonSerialize(using = IsolationLevelSerializer.class)
	public IsolationLevel getDefaultReadIsolationLevel() {
		return defaultReadIsolationLevel;
	}

	@JsonSerialize(using = IsolationLevelSerializer.class)
	public IsolationLevel getDefaultWriteIsolationLevel() {
		return defaultWriteIsolationLevel;
	}

	public @Nullable SearchStrategies getSearchStrategy() {
		return searchStrategy;
	}

	private static class IsolationLevelSerializer extends StdSerializer<IsolationLevel> {

		private static final long serialVersionUID = 1L;

		protected IsolationLevelSerializer() {
			super(IsolationLevel.class);
		}

		@Override
		public void serialize(IsolationLevel value, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			gen.writeString(value.getURI().toString());
		}

	}

	private static class IsolationLevelDeserializer extends StdDeserializer<IsolationLevel> {

		private static final long serialVersionUID = 1L;

		protected IsolationLevelDeserializer() {
			super(IsolationLevel.class);
		}

		@Override
		public IsolationLevel deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			String levelURI = p.getValueAsString();

			if (levelURI == null)
				return null;

			return Arrays.stream(IsolationLevels.values())
					.filter(level -> level.getURI().toString().equals(levelURI)).findAny()
					.orElseThrow(() -> new JsonMappingException(p,
							"Not a recognized isolation level URI: " + levelURI));
		}

	}

	public static STRepositoryInfo createDefault() {
		return new STRepositoryInfo(null, null, null, null, null, null);
	}

	public STRepositoryInfo withNewAccessCredentials(String newUsername, String newPassword) {
		return new STRepositoryInfo(backendType, newUsername, newPassword, defaultReadIsolationLevel,
				defaultWriteIsolationLevel, searchStrategy);
	}

}