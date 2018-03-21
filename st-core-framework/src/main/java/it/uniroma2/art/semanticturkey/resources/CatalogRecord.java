package it.uniroma2.art.semanticturkey.resources;

import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.utilities.IRI2StringConverter;

/**
 * A catalog record inside the metadata registry.
 */
public class CatalogRecord {
	private final IRI identity;
	private final GregorianCalendar issued;
	private final GregorianCalendar modified;
	private final DatasetMetadata abstractDataset;
	private final List<DatasetMetadata> versions;
	
	public CatalogRecord(IRI identity, GregorianCalendar issued, GregorianCalendar modified,
			DatasetMetadata abstractDataset, List<DatasetMetadata> versions) {
		this.identity = identity;
		this.issued = issued;
		this.modified = modified;
		this.abstractDataset = abstractDataset;
		this.versions = versions;
	}
	
	@JsonSerialize(converter=IRI2StringConverter.class)
	public IRI getIdentity() {
		return identity;
	}

	@JsonFormat(shape=Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public GregorianCalendar getIssued() {
		return issued;
	}
	
	@JsonFormat(shape=Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public GregorianCalendar getModified() {
		return modified;
	}
	
	public DatasetMetadata getAbstractDataset() {
		return abstractDataset;
	}
	
	public List<DatasetMetadata> getVersions() {
		return versions;
	}
	
	
}
