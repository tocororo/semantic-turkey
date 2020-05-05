package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.zthesserializer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ExporterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingExporter;
import it.uniroma2.art.semanticturkey.zthes.RdfToZthesMapper;
import it.uniroma2.art.semanticturkey.zthes.XmlSerializer;
import it.uniroma2.art.semanticturkey.zthes.Zthes;
import it.uniroma2.art.semanticturkey.zthes.ZthesSerializationException;

/**
 * A {@link ReformattingExporter} that serializes RDF data in Zthes format
 * 
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class ZthesSerializingExporter implements ReformattingExporter {

	private ZthesSerializingExporterConfiguration config;

	public ZthesSerializingExporter(ZthesSerializingExporterConfiguration config) {
		this.config = config;
	}

	@Override
	public ClosableFormattedResource export(RepositoryConnection sourceRepositoryConnection, IRI[] graphs,
			@Nullable String format, ExporterContext exporterContext) throws IOException {
		Objects.requireNonNull(format, "Format must be specified");
		try {
			RdfToZthesMapper mapper = new RdfToZthesMapper(sourceRepositoryConnection, exporterContext);
			if (this.config.pivotLanguages != null) {
				mapper.setLanguagePriorityList(new ArrayList<String>(this.config.pivotLanguages));
			}
			Zthes zthes = mapper.map();
			File tempServerFile = File.createTempFile("zthes", ".xml");
			new XmlSerializer().serialize(zthes, tempServerFile);
			return new ClosableFormattedResource(tempServerFile, "xml", "application/xml",
					StandardCharsets.UTF_8, null);
		} catch (ZthesSerializationException e) {
			throw new IOException(e);
		}
	}

}
