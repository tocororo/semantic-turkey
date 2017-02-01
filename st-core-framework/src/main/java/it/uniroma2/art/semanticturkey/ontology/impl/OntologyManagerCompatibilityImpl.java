package it.uniroma2.art.semanticturkey.ontology.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.STOntologyManager;

/**
 * Compatibility implementation of {@link OntologyManager} based on {@link STOntologyManager}.
 *
 */
public class OntologyManagerCompatibilityImpl implements OntologyManager {
	private STOntologyManager<? extends RDFModel> stOntManager;

	public OntologyManagerCompatibilityImpl(STOntologyManager<? extends RDFModel> stOntManager) {
		this.stOntManager = stOntManager;
	}

	@Override
	public boolean isSupportOntNamespace(String ns) {
		return stOntManager.isSupportOntNamespace(ns);
	}

	@Override
	public boolean isApplicationOntNamespace(String ns) {
		return stOntManager.isApplicationOntNamespace(ns);
	}

	@Override
	public ImportStatus getImportStatus(String baseURI) {
		return stOntManager.getImportStatus(baseURI);
	}

	@Override
	public void addOntologyImportFromWebToMirror(String baseUriToBeImported, String url, String destLocalFile,
			RDFFormat rdfFormat) throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromWebToMirror(baseUriToBeImported, url, destLocalFile, rdfFormat);
	}

	@Override
	public void addOntologyImportFromWeb(String baseUriToBeImported, String url, RDFFormat rdfFormat)
			throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromWeb(baseUriToBeImported, url, rdfFormat);
	}

	@Override
	public void addOntologyImportFromLocalFile(String baseUriToBeImported, String sourceForImport,
			String destLocalFile) throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromLocalFile(baseUriToBeImported, sourceForImport, destLocalFile);
	}

	@Override
	public void addOntologyImportFromMirror(String baseUriToBeImported, String destLocalFile)
			throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromMirror(baseUriToBeImported, destLocalFile);
	}

	@Override
	public void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile)
			throws ModelUpdateException, ImportManagementException {
		stOntManager.downloadImportedOntologyFromWebToMirror(baseURI, altURL, toLocalFile);
	}

	@Override
	public void downloadImportedOntologyFromWeb(String baseURI, String altURL)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		stOntManager.downloadImportedOntologyFromWeb(baseURI, altURL);
	}

	@Override
	public void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		stOntManager.getImportedOntologyFromLocalFile(baseURI, fromLocalFilePath, toLocalFile);
	}

	@Override
	public void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException {
		stOntManager.mirrorOntology(baseURI, toLocalFile);
	}

	@Override
	public Map<String, String> getNSPrefixMappings(boolean explicit) throws ModelAccessException {
		return stOntManager.getNSPrefixMappings(explicit);
	}

	@Override
	public void removeNSPrefixMapping(String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException {
		stOntManager.removeNSPrefixMapping(namespace);
	}

	@Override
	public void removeOntologyImport(String uriToBeRemoved)
			throws IOException, ModelUpdateException, ModelAccessException {
		stOntManager.removeOntologyImport(uriToBeRemoved);
	}

	@Override
	public void setNSPrefixMapping(String prefix, String namespace)
			throws NSPrefixMappingUpdateException, ModelUpdateException {
		stOntManager.setNSPrefixMapping(prefix, namespace);
	}

	@Override
	public Repository getRepository() {
		RDFModel model = stOntManager.getOntModel();

		try {
			Method m = model.getClass().getMethod("getRDF4JRepository");
			return (Repository) m.invoke(model);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalStateException("Cannot retrieve an RDF4J Repository for the project", e);
		}
	}

	@Override
	public void startOntModel(String baseURI, File projectCoreRepoDir, RepositoryConfig repoConfig)
			throws RDF4JException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void declareApplicationOntology(IRI ont, boolean ng, boolean ns) {
		stOntManager.declareApplicationOntology(
				VocabUtilities.nodeFactory.createURIResource(ont.stringValue()), ng, ns);
	}

	@Override
	public void initializeMappingsPersistence(NSPrefixMappings nsPrefixMappings)
			throws ModelUpdateException, ModelAccessException {
		stOntManager.initializeMappingsPersistence(nsPrefixMappings);
	}
}