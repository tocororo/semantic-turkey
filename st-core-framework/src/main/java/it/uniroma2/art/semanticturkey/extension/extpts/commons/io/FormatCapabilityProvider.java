package it.uniroma2.art.semanticturkey.extension.extpts.commons.io;

import java.util.List;

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
}
