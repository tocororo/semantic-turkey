package it.uniroma2.art.semanticturkey.ontology.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
import it.uniroma2.art.semanticturkey.exceptions.ImportManagementException;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.ontology.STOntologyManager;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.utilities.RDF4JMigrationUtils;

/**
 * Compatibility implementation of {@link OntologyManager} based on {@link STOntologyManager}.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
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
			RDFFormat rdfFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports) throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromWebToMirror(baseUriToBeImported, url, destLocalFile,
				RDF4JMigrationUtils.convert2art(rdfFormat));
	}

	@Override
	public void addOntologyImportFromWeb(String baseUriToBeImported, String url, RDFFormat rdfFormat,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromWeb(baseUriToBeImported, url,
				RDF4JMigrationUtils.convert2art(rdfFormat));
	}

	@Override
	public void addOntologyImportFromLocalFile(String baseUriToBeImported, String sourceForImport,
			String destLocalFile, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports) throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromLocalFile(baseUriToBeImported, sourceForImport, destLocalFile);
	}

	@Override
	public void addOntologyImportFromMirror(String baseUriToBeImported, String destLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, ModelUpdateException {
		stOntManager.addOntologyImportFromMirror(baseUriToBeImported, destLocalFile);
	}

	@Override
	public void downloadImportedOntologyFromWebToMirror(String baseURI, String altURL, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws ModelUpdateException, ImportManagementException {
		stOntManager.downloadImportedOntologyFromWebToMirror(baseURI, altURL, toLocalFile);
	}

	@Override
	public void downloadImportedOntologyFromWeb(String baseURI, String altURL,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		stOntManager.downloadImportedOntologyFromWeb(baseURI, altURL);
	}

	@Override
	public void getImportedOntologyFromLocalFile(String baseURI, String fromLocalFilePath, String toLocalFile,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws MalformedURLException, ModelUpdateException, ImportManagementException {
		stOntManager.getImportedOntologyFromLocalFile(baseURI, fromLocalFilePath, toLocalFile);
	}

	@Override
	public void mirrorOntology(String baseURI, String toLocalFile)
			throws ImportManagementException, ModelUpdateException {
		stOntManager.mirrorOntology(baseURI, toLocalFile);
	}

	@Override
	public Map<String, String> getNSPrefixMappings(boolean explicit) throws OntologyManagerException {
		try {
			return stOntManager.getNSPrefixMappings(explicit);
		} catch (ModelAccessException e) {
			throw new OntologyManagerException(e);
		}
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

	@Override
	public void setBaseURI(String baseURI) throws OntologyManagerException {
		try {
			stOntManager.getOntModel().setBaseURI(baseURI);
		} catch (ModelUpdateException e) {
			throw new OntologyManagerException(e);
		}
	}

	@Override
	public String getBaseURI() {
		return stOntManager.getOntModel().getBaseURI();
	}

	@Override
	public void loadOntologyData(File inputFile, String baseURI, RDFFormat format, Resource graph,
			TransitiveImportMethodAllowance transitiveImportAllowance, Set<IRI> failedImports)
			throws FileNotFoundException, IOException, RDF4JException {
		try {
			stOntManager.loadOntologyData(inputFile, baseURI, RDF4JMigrationUtils.convert2art(format));
		} catch (ModelAccessException | ModelUpdateException | UnsupportedRDFFormatException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void clearData() throws RDF4JException {
		try {
			stOntManager.clearData();
		} catch (ModelCreationException | ModelUpdateException e) {
			throw new RepositoryException(e);
		}
	}
}
