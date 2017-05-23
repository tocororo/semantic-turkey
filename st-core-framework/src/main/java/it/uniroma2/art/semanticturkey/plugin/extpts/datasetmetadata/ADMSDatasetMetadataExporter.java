package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.AbstractPlugin;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.project.Project;

/**
 * A {@link DatasetMetadataExporter} for the <a href="https://www.w3.org/TR/vocab-adms/">Asset Description
 * Metadata Schema (ADMS)</a>
 */
public class ADMSDatasetMetadataExporter extends
		AbstractPlugin<DatasetMetadataExporterSettings, ADMSDatasetMetadataExporterSettings, ADMSDatasetMetadataExporterFactory>
		implements DatasetMetadataExporter {

	public ADMSDatasetMetadataExporter(ADMSDatasetMetadataExporterFactory factory) {
		super(factory);
	}

	@Override
	public Model produceDatasetMetadata(Project<?> project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException {
		return new LinkedHashModel();
	}

}