package it.uniroma2.art.semanticturkey.mdr.core;

import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;

/**
 * A catalog record inside the metadata registry.
 */
public class CatalogRecord {
	private final IRI identity;
	private final GregorianCalendar issued;
	private final GregorianCalendar modified;
	private final DatasetMetadata abstractDataset;
	private final List<DatasetMetadata> versions;

	@JsonCreator
	public CatalogRecord(
			@JsonProperty("identity") @JsonDeserialize(converter = String2IRIConverter.class) IRI identity,
			@JsonProperty("issued") GregorianCalendar issued,
			@JsonProperty("modified") @Nullable GregorianCalendar modified,
			@JsonProperty("abstractDataset") DatasetMetadata abstractDataset,
			@JsonProperty("versions") List<DatasetMetadata> versions) {
		this.identity = identity;
		this.issued = issued;
		this.modified = modified;
		this.abstractDataset = abstractDataset;
		this.versions = versions;
	}

	@JsonSerialize(converter = IRI2StringConverter.class)
	public IRI getIdentity() {
		return identity;
	}

	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public GregorianCalendar getIssued() {
		return issued;
	}

	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@Nullable
	public GregorianCalendar getModified() {
		return modified;
	}

	public DatasetMetadata getAbstractDataset() {
		return abstractDataset;
	}

	public List<DatasetMetadata> getVersions() {
		return versions;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("identity", identity).add("issued", null)
				.add("modified", null).add("abstractDataset", abstractDataset).add("versions", versions)
				.toString();
	}

}
