package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ExporterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingExporter;
import it.uniroma2.art.semanticturkey.utilities.RDF4JUtilities;

/**
 * A {@link ReformattingExporter} that serializes RDF data according to the provided serialization format
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDFSerializingExporter implements ReformattingExporter {

	@Override
	public ClosableFormattedResource export(RepositoryConnection sourceRepositoryConnection, IRI[] graphs,
			@Nullable String format, ExporterContext exporterContext) throws IOException {
		Objects.requireNonNull(format, "Format must be specified");
		RDFFormat rdfFormat = RDF4JUtilities.getRDFFormat(format);

		return ClosableFormattedResource.build(
				outputStream -> sourceRepositoryConnection.export(Rio.createWriter(rdfFormat, outputStream),
						graphs),
				rdfFormat.getDefaultMIMEType(), rdfFormat.getDefaultFileExtension(), rdfFormat.getCharset());
	}

}
