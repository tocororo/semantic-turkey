package it.uniroma2.art.semanticturkey.ontology.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.repository.util.Repositories;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.models.PrefixMapping;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;

public class OntologyManagerImpl implements OntologyManager {

	private Repository repository;
	private NSPrefixMappings nsPrefixMappings;
	private volatile String baseURI;

	@Override
	public boolean isSupportOntNamespace(String ns) {
		return false;
	}

	@Override
	public boolean isApplicationOntNamespace(String ns) {
		return false;
	}

	@Override
	public ImportStatus getImportStatus(String baseURI) {
		return new ImportStatus(ImportStatus.Values.NG, null); // TODO
	}

	@Override
	public void addOntologyImportFromWebToMirror(String baseUriToBeImported, String url, String destLocalFile,
			RDFFormat rdfFormat) throws MalformedURLException, ModelUpdateException {

	}

	@Override
	public void addOntologyImportFromWeb(String baseUriToBeImported, String url, RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOntologyImportFromLocalFile(String baseUriToBeImported, String sourceForImport,
			String destLocalFile) throws MalformedURLException, ModelUpdateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOntologyImportFromMirror(String baseUriToBeImported, String destLocalFile)
			throws MalformedURLException, ModelUpdateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException {
		// TODO Auto-generated method stub

	}

	@Override
	public void downloadImportedOntologyFromWeb(String baseURI, String altURL)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		// TODO Auto-generated method stub

	}

	@Override
	public void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		// TODO Auto-generated method stub

	}

	@Override
	public void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, String> getNSPrefixMappings(boolean explicit) throws ModelAccessException {
		return Collections.emptyMap();
	}

	@Override
	public void removeNSPrefixMapping(String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeOntologyImport(String uri)
			throws IOException, ModelUpdateException, ModelAccessException {
		// TODO Auto-generated method stub

	}

	// TODO Pay attention this is not being invoked by any method! check metadata ones!
	public void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException {
		Repositories.consume(repository, conn -> conn.setNamespace(prefix, namespace));
		nsPrefixMappings.setNSPrefixMapping(namespace, prefix);
	}

	/**
	 * a wrapper around OWLART {@link PrefixMapping} with an additional <code>overwrite</code> parameter
	 * which, if false, makes the method ignore calls if the namespace-prefix mapping already exists. If true,
	 * it still does not overwrite if the old and new values are the same
	 * 
	 * @param namespace
	 * @param prefix
	 * @param overwrite
	 * @throws ModelAccessException
	 * @throws ModelUpdateException
	 */
	private void setNsPrefix(String namespace, String prefix, boolean overwrite)
			throws ModelAccessException, ModelUpdateException {
		Repositories.consume(repository, conn -> {
			String oldPrefix = QueryResults.stream(conn.getNamespaces())
					.filter(ns -> ns.getName().equals(namespace)).findAny().map(Namespace::getPrefix)
					.orElse(null);
			if ((oldPrefix == null) || (overwrite && !oldPrefix.equals(prefix))) {
				conn.setNamespace(prefix, namespace);
			}
		});
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public void startOntModel(String baseURI, File repoDir, RepositoryConfig supportRepoConfig)
			throws OntologyManagerException {
		try {
			SailRepositoryFactory repoFactory = new SailRepositoryFactory();
			repository = repoFactory.getRepository(supportRepoConfig.getRepositoryImplConfig());
			repository.setDataDir(repoDir);
			this.baseURI = baseURI;
			repository.initialize();
		} catch (RDF4JException e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public void declareApplicationOntology(IRI ont, boolean ng, boolean ns) {
	}

	@Override
	public void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappings)
			throws ModelUpdateException, ModelAccessException {
		this.nsPrefixMappings = nsPrefixMappings;
		// owlModel nsPrefixMapping regeneration from persistenceNSPrefixMapping
		Map<String, String> nsPrefixMapTable = nsPrefixMappings.getNSPrefixMappingTable();
		Set<Map.Entry<String, String>> mapEntries = nsPrefixMapTable.entrySet();
		for (Map.Entry<String, String> entry : mapEntries) {
			setNsPrefix(entry.getValue(), entry.getKey(), true);
		}

	}

	@Override
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	@Override
	public String getBaseURI() {
		return baseURI;
	}

}
