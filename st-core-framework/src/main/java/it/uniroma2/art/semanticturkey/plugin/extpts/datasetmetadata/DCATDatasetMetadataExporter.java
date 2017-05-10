package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.project.Project;

/**
 * A {@link DatasetMetadataExporter} for the <a href="https://www.w3.org/TR/vocab-dcat/">Data Catalog
 * Vocabulary (DCAT)</a>
 */
public class DCATDatasetMetadataExporter implements DatasetMetadataExporter {

	@Override
	public Model produceDatasetMetadata(Project<?> project, RepositoryConnection conn) {
		return new LinkedHashModel();
	}

}
