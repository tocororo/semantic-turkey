package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;

/**
 * Ontology manager.
 *
 */
public interface OntologyManager {

	boolean isSupportOntNamespace(String ns);

	boolean isApplicationOntNamespace(String ns);

	ImportStatus getImportStatus(String baseURI);

	void addOntologyImportFromWebToMirror(String baseUriToBeImported, String url, String destLocalFile,
			RDFFormat rdfFormat) throws MalformedURLException, ModelUpdateException;

	void addOntologyImportFromWeb(String baseUriToBeImported, String url, RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException;

	void addOntologyImportFromLocalFile(String baseUriToBeImported, String sourceForImport,
			String destLocalFile) throws MalformedURLException, ModelUpdateException;

	void addOntologyImportFromMirror(String baseUriToBeImported, String destLocalFile)
			throws MalformedURLException, ModelUpdateException;

	void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException;

	void downloadImportedOntologyFromWeb(String baseURI, String altURL)
			throws MalformedURLException, ModelUpdateException, ImportManagementException;

	void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, ModelUpdateException, ImportManagementException;

	void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException;

	Map<String, String> getNSPrefixMappings(boolean explicit) throws ModelAccessException;

	void removeNSPrefixMapping(String namespace) throws NSPrefixMappingUpdateException, ModelUpdateException;

	void removeOntologyImport(String uri) throws IOException, ModelUpdateException, ModelAccessException;

	void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException;

	Repository getRepository();

	void startOntModel(String baseURI, File repoDir, RepositoryConfig repoConfig) throws RDF4JException;

	void declareApplicationOntology(IRI iri, boolean b, boolean c);

	void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappingsPersistence) throws ModelUpdateException, ModelAccessException;
}
