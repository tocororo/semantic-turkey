package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.uniroma2.art.semanticturkey.resources.DataFormat;

/**
 * Exposes the formats supported by an implementation of {@link ReformattingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface FormatCapabilityProvider {
	@JsonIgnore
	List<DataFormat> getFormats();
}
