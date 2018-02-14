package it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

/**
 * Extension point for the dataset metadata exporters
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface DatasetMetadataExporter extends Extension {

	Model produceDatasetMetadata(Project project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException, STPropertyAccessException;
}