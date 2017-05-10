package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.project.Project;

/**
 * A {@link DatasetMetadataExporter} for the
 * <a href="https://www.w3.org/2016/05/ontolex/#metadata-lime">Linguistic Metadata vocabulary (LIME)</a> and
 * the <a href="https://www.w3.org/TR/void/">Vocabulary of Interlinked Datasets (VoID)</a>
 */
public class VOIDLIMEDatasetMetadataExporter implements DatasetMetadataExporter {

	@Override
	public Model produceDatasetMetadata(Project<?> project, RepositoryConnection conn) {
		return new LinkedHashModel();
	}

}
