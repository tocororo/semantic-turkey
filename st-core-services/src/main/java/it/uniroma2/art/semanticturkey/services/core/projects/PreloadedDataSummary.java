package it.uniroma2.art.semanticturkey.services.core.projects;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class PreloadedDataSummary {

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
	public static interface PreloadWarning {
		String getMessage();
	}

	public static class ProfilerSizeTresholdExceeded implements PreloadWarning {

		private long profilerDataSizeTreshold;
		private String message;

		public ProfilerSizeTresholdExceeded(long profilerDataSizeTreshold) {
			this.profilerDataSizeTreshold = profilerDataSizeTreshold;
			this.message = "The preloaded data size exceeds the profiler treshold: "
					+ profilerDataSizeTreshold + " bytes";
		}

		public long getProfilerDataSizeTreshold() {
			return profilerDataSizeTreshold;
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

	public File getPreloadedDataFile() {
		return preloadedDataFile;
	}

	@JsonSerialize(using = ToStringSerializer.class)
	public RDFFormat getPreloadedDataFormat() {
		return preloadedDataFormat;
	}

	public List<PreloadWarning> getWarnings() {
		return warnings;
	}

}
