package it.uniroma2.art.semanticturkey.mdr.core;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.Optional2StringConverter;

/**
 * Metadata describing a dataset.
 * 
 */
public class DatasetMetadata {

	private final IRI identity;
	private final Optional<String> uriSpace;
	private final Optional<String> title;
	private final Optional<IRI> dereferenciationSystem;
	private final Optional<SPARQLEndpointMedatadata> sparqlEndpoint;
	private final Optional<String> versionInfo;

	public DatasetMetadata(IRI identity, @Nullable String uriSpace, @Nullable String title,
			@Nullable IRI dereferenciationSystem, @Nullable SPARQLEndpointMedatadata endpointMetadata,
			@Nullable String versionInfo) {
		this.identity = identity;
		this.uriSpace = Optional.ofNullable(uriSpace);
		this.title = Optional.ofNullable(title);
		this.dereferenciationSystem = Optional.ofNullable(dereferenciationSystem);
		this.sparqlEndpoint = Optional.ofNullable(endpointMetadata);
		this.versionInfo = Optional.ofNullable(versionInfo);
	}

	@JsonSerialize(converter = IRI2StringConverter.class)
	public IRI getIdentity() {
		return identity;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<String> getUriSpace() {
		return uriSpace;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<String> getTitle() {
		return title;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<IRI> getDereferenciationSystem() {
		return dereferenciationSystem;
	}

	@JsonIgnore
	public Optional<IRI> getSparqlEndpoint() {
		return sparqlEndpoint.map(SPARQLEndpointMedatadata::getEndpoint);
	}

	public Optional<SPARQLEndpointMedatadata> getSparqlEndpointMetadata() {
		return sparqlEndpoint;
	}

	@JsonSerialize(converter = Optional2StringConverter.class)
	public Optional<String> getVersionInfo() {
		return versionInfo;
	}

	@JsonIgnore
	public boolean isAccessible() {
		return getSparqlEndpoint().isPresent() || getDereferenciationSystem().isPresent();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("uriSpace", uriSpace).add("title", title)
				.add("dereferenciationSystem", dereferenciationSystem).add("sparqlEndpoint", sparqlEndpoint)
				.add("versionInfo", versionInfo).toString();
	}

	public static class SPARQLEndpointMedatadata {
		private final IRI endpoint;
		private final Set<IRI> limitations;

		public SPARQLEndpointMedatadata(IRI endpoint, Set<IRI> limitations) {
			this.endpoint = endpoint;
			this.limitations = limitations;
		}

		@JsonProperty("@id")
		public IRI getEndpoint() {
			return endpoint;
		}

		public Set<IRI> getLimitations() {
			return limitations;
		}
	}
}
