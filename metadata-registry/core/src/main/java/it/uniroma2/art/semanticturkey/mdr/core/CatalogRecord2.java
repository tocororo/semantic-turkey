package it.uniroma2.art.semanticturkey.mdr.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;
import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;
import it.uniroma2.art.semanticturkey.utilities.String2IRIConverter;
import org.eclipse.rdf4j.model.IRI;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A catalog record inside the metadata registry.
 */
public class CatalogRecord2 {

	private final IRI identity;
	private final ZonedDateTime issued;
	private final ZonedDateTime modified;
	private final DatasetMetadata2 dataset;

	@JsonCreator
	public CatalogRecord2(
			@JsonProperty("identity") @JsonDeserialize(converter = String2IRIConverter.class) IRI identity,
			@JsonProperty("issued") ZonedDateTime issued,
			@JsonProperty("modified") @Nullable ZonedDateTime modified,
			@JsonProperty("dataset") DatasetMetadata2 dataset) {
		this.identity = identity;
		this.issued = issued;
		this.modified = modified;
		this.dataset = dataset;
	}

	@JsonSerialize(converter = IRI2StringConverter.class)
	public IRI getIdentity() {
		return identity;
	}

	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public ZonedDateTime getIssued() {
		return issued;
	}

	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@Nullable
	public ZonedDateTime getModified() {
		return modified;
	}

	public DatasetMetadata2 getDataset() {
		return dataset;
	}

	public String toString() {
		return MoreObjects.toStringHelper(this).add("identity", identity).add("issued", issued)
				.add("modified", modified).add("dataset", dataset)
				.toString();
	}

}
