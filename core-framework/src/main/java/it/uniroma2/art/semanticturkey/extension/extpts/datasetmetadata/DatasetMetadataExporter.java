package it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryStateException;
import it.uniroma2.art.semanticturkey.mdr.core.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

import java.util.Collections;
import java.util.Map;

/**
 * Extension point for the dataset metadata exporters
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface DatasetMetadataExporter extends Extension {

	Model produceDatasetMetadata(Project project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException, STPropertyAccessException;

	default Map<Scope, Settings> importFromMetadataRegistry(Project project) throws NoSuchDatasetMetadataException, MetadataRegistryStateException {
		return Collections.emptyMap();
	}
}