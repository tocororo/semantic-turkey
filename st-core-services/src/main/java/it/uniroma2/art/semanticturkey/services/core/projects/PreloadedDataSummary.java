package it.uniroma2.art.semanticturkey.services.core.projects;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class PreloadedDataSummary {
	private Optional<String> baseURI;
	private Optional<IRI> model;
	private Optional<IRI> lexicalizationModel;
	private File preloadedDataFile;
	private RDFFormat preloadedDataFormat;

	public PreloadedDataSummary(@Nullable String baseURI, @Nullable IRI model,
			@Nullable IRI lexicalizationModel, File preloadedDataFile, RDFFormat preloadedDataFormat) {
		this.baseURI = Optional.ofNullable(baseURI);
		this.model = Optional.ofNullable(model);
		this.lexicalizationModel = Optional.ofNullable(lexicalizationModel);
		this.preloadedDataFile = Objects.requireNonNull(preloadedDataFile,
				"Preloaded data file must be non null");
		this.preloadedDataFormat = Objects.requireNonNull(preloadedDataFormat,
				"Preloaded data format must be non null");
	}

	public Optional<String> getBaseURI() {
		return baseURI;
	}

	public Optional<IRI> getModel() {
		return model;
	}

	public Optional<IRI> getLexicalizationModel() {
		return lexicalizationModel;
	}

	public File getPreloadedDataFile() {
		return preloadedDataFile;
	}

	@JsonSerialize(using = ToStringSerializer.class)
	public RDFFormat getPreloadedDataFormat() {
		return preloadedDataFormat;
	}
}
