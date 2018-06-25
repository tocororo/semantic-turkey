package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import java.io.IOException;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;

/**
 * Extension point for reformatting exporters. These components can be placed inside an export pipeline, just
 * before a {@link Deployer}, in order to convert RDF data into (usually a non-RDF) format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface ReformattingExporter extends Extension {
	public ClosableFormattedResource export(RepositoryConnection sourceRepositoryConnection, IRI[] graphs,
			@Nullable String format, ExporterContext exporterContext) throws ReformattingException, IOException;
}
