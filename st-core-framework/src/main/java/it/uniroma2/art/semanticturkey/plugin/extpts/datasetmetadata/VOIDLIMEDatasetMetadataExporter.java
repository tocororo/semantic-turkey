package it.uniroma2.art.semanticturkey.plugin.extpts.datasetmetadata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.profiler.LIMEProfiler;
import it.uniroma2.art.lime.profiler.ProfilerException;
import it.uniroma2.art.lime.profiler.ProfilerOptions;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.project.Project;

/**
 * A {@link DatasetMetadataExporter} for the
 * <a href="https://www.w3.org/2016/05/ontolex/#metadata-lime">Linguistic Metadata vocabulary (LIME)</a> and
 * the <a href="https://www.w3.org/TR/void/">Vocabulary of Interlinked Datasets (VoID)</a>
 */
public class VOIDLIMEDatasetMetadataExporter implements DatasetMetadataExporter {

	@Override
	public Model produceDatasetMetadata(Project<?> project, RepositoryConnection conn, IRI dataGraph)
			throws DatasetMetadataExporterException {
		Repository tempMetadataRepository = new SailRepository(new MemoryStore());
		tempMetadataRepository.initialize();
		try {
			try (RepositoryConnection metadataConnection = tempMetadataRepository.getConnection()) {
				LIMEProfiler profiler = new LIMEProfiler(metadataConnection,
						SimpleValueFactory.getInstance().createIRI("http://example.org/"), conn, dataGraph);
				ProfilerOptions options = new ProfilerOptions();
				options.setMainDatasetName("http://example.org/void.ttl#MyDataset");
				try {
					profiler.profile(options);
				} catch (ProfilerException e) {
					throw new DatasetMetadataExporterException(e);

				}
				Model metadataModel = new LinkedHashModel();
				metadataModel.setNamespace(VOID.NS);
				metadataModel.setNamespace(LIME.NS);
				metadataModel.setNamespace(FOAF.NS);
				metadataModel.setNamespace(DCTERMS.NS);

				StatementCollector stmtCollector = new StatementCollector(metadataModel);
				metadataConnection.export(stmtCollector);
				return metadataModel;
			}
		} finally {
			tempMetadataRepository.shutDown();
		}
	}

}
