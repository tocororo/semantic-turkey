package it.uniroma2.art.semanticturkey.extension.extpts.commons.io;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.RDFLifter;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingExporter;
import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * This interface allows to expose the notion of supported formats. Examples of application include
 * {@link ReformattingExporter}s and {@link RDFLifter}s. This interface should be implemented by the
 * {@link ExtensionFactory}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface FormatCapabilityProvider {
	@JsonIgnore
	List<DataFormat> getFormats();

	@JsonIgnore
	default Optional<DataFormat> getFormatForFilename(String filename) {
		String filenameWithoutPath = FilenameUtils.getName(filename);

		// try with last dot
		int lastDot = filenameWithoutPath.lastIndexOf(".");
		String suffixExt; // extension after last dot
		String prefixExt; // extension between first and second dot (e.g. to handle ttl.gz)

		if (lastDot == -1) {
			suffixExt = "";
		} else {
			suffixExt = filenameWithoutPath.substring(lastDot + 1);
		}

		int firstDot = filenameWithoutPath.indexOf(".");

		if (firstDot < lastDot) { // note firstDot == -1 iff lastDot == -1
			int secondDot = filenameWithoutPath.indexOf(".", firstDot + 1);

			if (secondDot != -1) {
				prefixExt = filenameWithoutPath.substring(firstDot + 1, secondDot);
			} else {
				prefixExt = "";
			}
		} else {
			prefixExt = "";
		}

		Optional<DataFormat> dataFormat = getFormats().stream()
				.filter(f -> suffixExt.equals(f.getDefaultFileExtension())).findAny();

		if (!dataFormat.isPresent()) {
			dataFormat = getFormats().stream().filter(f -> prefixExt.equals(f.getDefaultFileExtension()))
					.findAny();

		}

		return dataFormat;
	}

	default Optional<DataFormat> getFormatForMIME(String mime) {
		return getFormats().stream().filter(f -> Objects.equals(mime, f.getDefaultMimeType())).findAny();
	}
}
