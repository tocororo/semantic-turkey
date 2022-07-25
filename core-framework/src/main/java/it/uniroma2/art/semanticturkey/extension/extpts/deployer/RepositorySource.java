package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import it.uniroma2.art.semanticturkey.project.Project;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A {@link Source} for a {@link Deployer} that wraps an {@link RepositoryConnection}. This kind of source is
 * used, for example, when data are deployed as is without apply any transformation to a non-RDF format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RepositorySource extends Source {
	private final RepositoryConnection sourceRepository;
	private final IRI[] graphs;
	private final Project project;

	public RepositorySource(RepositoryConnection sourceRepository, IRI[] graphs, Project project) {
		this.sourceRepository = sourceRepository;
		this.graphs = graphs;
		this.project = project;
	}

	public RepositoryConnection getSourceRepositoryConnection() {
		return sourceRepository;
	}

	public IRI[] getGraphs() {
		return graphs;
	}

	public Project getProject() {
		return project;
	}
}
