package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;

@STService
public class SkosDiffing extends STServiceAdapter {

	@STServiceOperation(method = RequestMethod.POST)
	public void runDiffing(String leftProjectName, @Optional String leftVersionRepoId,
			String rightProjectName, @Optional String rightVersionRepoId) {

		Project leftProject = ProjectManager.getProject(leftProjectName);
		IRI leftSparqlEndpoint = getSparqlEndpoint(leftProject, leftVersionRepoId);
		System.out.println("leftSparqlEndpoint " + leftSparqlEndpoint);
		if (leftSparqlEndpoint == null) {
			throw new IllegalStateException("Missing SPARQL endpoint for the left dataset");
		}

		Project rightProject = ProjectManager.getProject(rightProjectName);
		IRI rightSparqlEndpoint = getSparqlEndpoint(rightProject, rightVersionRepoId);
		System.out.println("rightSparqlEndpoint " + rightSparqlEndpoint);
		if (rightSparqlEndpoint == null) {
			throw new IllegalStateException("Missing SPARQL endpoint for the right dataset");
		}
	}

	private IRI getSparqlEndpoint(Project project, String versionId) {
		IRI sparqlEndpoint = null;
		if (versionId == null) {
			versionId = Project.CORE_REPOSITORY;
		}
		RepositoryImplConfig coreRepoImplConfig = STLocalRepositoryManager.getUnfoldedRepositoryImplConfig(
				project.getRepositoryManager().getRepositoryConfig(versionId));
		if (coreRepoImplConfig instanceof HTTPRepositoryConfig) {
			sparqlEndpoint = SimpleValueFactory.getInstance().createIRI(((HTTPRepositoryConfig) coreRepoImplConfig).getURL());
		}
		return sparqlEndpoint;
	}

}
