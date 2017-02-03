package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.impl.OntologyManagerException;

/**
 * Ontology manager.
 *
 */
public interface OntologyManager {

	void addOntologyImportFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, RDF4JException, ModelUpdateException, OntologyManagerException;

	void addOntologyImportFromMirror(String baseURI, String mirFileString)
			throws MalformedURLException, ModelUpdateException, RDF4JException, OntologyManagerException;

	void addOntologyImportFromWeb(String baseUriToBeImported, String url, RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException, RDF4JException, OntologyManagerException;

	void addOntologyImportFromWebToMirror(String baseURI, String sourceURL, String toLocalFile,
			RDFFormat rdfFormat) throws MalformedURLException, ModelUpdateException, RDF4JException,
					OntologyManagerException;

	void declareApplicationOntology(IRI iri, boolean b, boolean c);

	void downloadImportedOntologyFromWeb(String baseURI, String altURL)
			throws MalformedURLException, ModelUpdateException, ImportManagementException;

	void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException;

	String getBaseURI();

	void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, ModelUpdateException, ImportManagementException;

	ImportStatus getImportStatus(String baseURI);

	Map<String, String> getNSPrefixMappings(boolean explicit) throws ModelAccessException;

	Repository getRepository();

	void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappingsPersistence)
			throws ModelUpdateException, ModelAccessException;

	boolean isApplicationOntNamespace(String ns);

	boolean isSupportOntNamespace(String ns);

	void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException;

	void removeNSPrefixMapping(String namespace) throws NSPrefixMappingUpdateException, ModelUpdateException;

	void removeOntologyImport(String uri) throws IOException, ModelUpdateException, ModelAccessException;

	void setBaseURI(String baseURI);

	void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException;

	void startOntModel(String baseURI, File repoDir, RepositoryConfig repoConfig) throws RDF4JException;
}
