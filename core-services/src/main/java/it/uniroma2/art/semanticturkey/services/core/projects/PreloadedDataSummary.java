package it.uniroma2.art.semanticturkey.services.core.projects;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import it.uniroma2.art.semanticturkey.properties.DataSize;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

public class PreloadedDataSummary {

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
	public static interface PreloadWarning {
		String getMessage();
	}

	public static class ProfilerSizeTresholdExceeded implements PreloadWarning {

		private DataSize profilerDataSizeThreshold;
		private String message;

		public ProfilerSizeTresholdExceeded(DataSize profilerDataSizeThreshold) {
			this.profilerDataSizeThreshold = profilerDataSizeThreshold;
			this.message = "The preloaded data size exceeds the profiler threshold: "
					+ profilerDataSizeThreshold.stringValue();
		}

		public DataSize getProfilerDataSizeThreshold() {
			return profilerDataSizeThreshold;
		}

		@Override
		public String getMessage() {
			return message;
		}
	}

	private Optional<String> baseURI;
	private Optional<IRI> model;
	private Optional<IRI> lexicalizationModel;
	private File preloadedDataFile;
	private RDFFormat preloadedDataFormat;
	private List<PreloadWarning> warnings;

	public PreloadedDataSummary(@Nullable String baseURI, @Nullable IRI model,
			@Nullable IRI lexicalizationModel, File preloadedDataFile, RDFFormat preloadedDataFormat,
			List<PreloadWarning> warnings) {
		this.baseURI = Optional.ofNullable(baseURI);
		this.model = Optional.ofNullable(model);
		this.lexicalizationModel = Optional.ofNullable(lexicalizationModel);
		this.preloadedDataFile = Objects.requireNonNull(preloadedDataFile,
				"Preloaded data file must be non null");
		this.preloadedDataFormat = Objects.requireNonNull(preloadedDataFormat,
				"Preloaded data format must be non null");
		this.warnings = Objects.requireNonNull(warnings,
				"The list of warnings must be non-null; if there is no warning to report, please provide an empty list");
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

	@JsonSerialize(converter = GetFileNameConverter.class)
	public File getPreloadedDataFile() {
		return preloadedDataFile;
	}

	@JsonSerialize(converter = GetRDFFormatNameConverter.class)
	public RDFFormat getPreloadedDataFormat() {
		return preloadedDataFormat;
	}

	public List<PreloadWarning> getWarnings() {
		return warnings;
	}

	private static class GetRDFFormatNameConverter extends StdConverter<RDFFormat, String> {

		@Override
		public String convert(RDFFormat value) {
			return value.getName();
		}

	}
	
	private static class GetFileNameConverter extends StdConverter<File, String> {

		@Override
		public String convert(File value) {
			return value.getName();
		}
		
	}

}
