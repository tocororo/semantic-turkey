package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;

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

	private final RDFSerializingExporterConfiguration conf;

	public RDFSerializingExporter(RDFSerializingExporterConfiguration conf) {
		this.conf = conf;
	}

	private WriterConfig createRDF4JConfig() {
		WriterConfig writerConfig = new WriterConfig();
		if (conf.prettyPrint != null) {
			writerConfig.set(BasicWriterSettings.PRETTY_PRINT, conf.prettyPrint);
		}

		if (conf.inlineBlankNodes != null) {
			writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, conf.inlineBlankNodes);
		}

		if (conf.xsdStringToPlainLiteral != null) {
			writerConfig.set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, conf.xsdStringToPlainLiteral);
		}

		if (conf.rdfLangStringToLangLiteral != null) {
			writerConfig.set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL,
					conf.rdfLangStringToLangLiteral);
		}

		if (conf.baseDirective != null) {
			writerConfig.set(BasicWriterSettings.BASE_DIRECTIVE, conf.baseDirective);
		}

		return writerConfig;
	}

	@Override
	public ClosableFormattedResource export(RepositoryConnection sourceRepositoryConnection, IRI[] graphs,
			@Nullable String format, ExporterContext exporterContext) throws IOException {
		Objects.requireNonNull(format, "Format must be specified");
		RDFFormat rdfFormat = RDF4JUtilities.getRDFFormat(format);

		return ClosableFormattedResource.build(outputStream -> {
			RDFWriter rdfWriter;

			if (RDFFormat.RDFXML.equals(rdfFormat) && (Boolean.TRUE.equals(conf.prettyPrint)
					|| Boolean.TRUE.equals(conf.inlineBlankNodes))) {
				try {
					RDFWriter baseWriter = new RDFXMLPrettyWriterFactory().getWriter(outputStream);
					Class<?> arrangedWriter = TurtleWriterFactory.class.getClassLoader()
							.loadClass("org.eclipse.rdf4j.rio.turtle.ArrangedWriter");
					Constructor<?> arrangedWriterConstructor = arrangedWriter.getConstructor(RDFWriter.class);
					arrangedWriterConstructor.setAccessible(true);
					rdfWriter = (RDFWriter) arrangedWriterConstructor
							.newInstance(baseWriter);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
					throw new RuntimeException(e);
				}
			} else {
				rdfWriter = Rio.createWriter(rdfFormat, outputStream);
			}

			rdfWriter.setWriterConfig(createRDF4JConfig());
			sourceRepositoryConnection.export(rdfWriter, graphs);
		}, rdfFormat.getDefaultMIMEType(), rdfFormat.getDefaultFileExtension(), rdfFormat.getCharset(), null);
	}

}
