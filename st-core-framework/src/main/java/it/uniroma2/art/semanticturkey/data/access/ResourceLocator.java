package it.uniroma2.art.semanticturkey.data.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * This class is used to locate a resource either as belonging to a currently open project or to a remote
 * dataset.
 */
public class ResourceLocator {

	@Autowired
	private MetadataRegistryBackend datasetMetadataRepository;

	public static final UnknownResourcePosition UNKNOWN_RESOURCE_POSITION = new UnknownResourcePosition();

	/**
	 * Locate a resource. The locator implements the following algorithm:
	 * <ol>
	 * <li>if <code>resource</code> is a bnode ({@link ARTNode#isBlank()} returns <code>true</code>), then
	 * assumes it belongs to the provided project</li>
	 * <li>otherwise; <code>resource</code> is a uri, then do the following
	 * <ol>
	 * <li>if the namespace of <code>resource</code> is equal to the default namespace of <code>project</code>
	 * or <code>resource</code> is defined in any graph of <code>project</code> (see
	 * {@link RDFModel#isLocallyDefined(ARTResource, ARTResource...)}, then assumes that <code>resource</code>
	 * belongs to <code>project</code></li>
	 * <li>for each open and accessible project <code>p</code>, if the namespace of <code>resource</code> is
	 * equal to the default namespace of <code>p</code></li>, then assumes that <code>resource</code> belongs
	 * to <code>p</code></li>
	 * <li>attempt to locate <code>resource</code> in a remote dataset (see
	 * {@link DatasetMetadataRepositoryImpl#findDatasetForResource(IRI)}</li>
	 * <li>otherwise; states that the position is unknown</li>
	 * </ol>
	 * </li>
	 * </ol>
	 * 
	 * @param project
	 *            the current project
	 * @param projectRepository
	 *            the repository holding the data inside the project
	 * @param resource
	 *            the resource to be located
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @return
	 * @throws ModelAccessException
	 * @throws ProjectAccessException
	 */
	public ResourcePosition locateResource(Project project, Repository projectRepository,
			Resource resource, AccessLevel requestedAccessLevel, LockLevel requestedLockLevel)
			throws ProjectAccessException {
		if (resource instanceof BNode) {
			return new LocalResourcePosition(project); // TODOprojectRepository: implement a better condition
		}

		IRI iriResource = (IRI) resource;

		RepositoryConnection repoConn = RDF4JRepositoryUtils.getConnection(projectRepository);
		try {
			if (Objects.equals(repoConn.getNamespace(""), iriResource.getNamespace())
					|| repoConn.hasStatement(iriResource, null, null, false)) {
				return new LocalResourcePosition(project);
			}

		} finally {
			RDF4JRepositoryUtils.releaseConnection(repoConn, projectRepository);
		}

		for (AbstractProject abstrProj : ProjectManager.listProjects()) {
			if (!ProjectManager.isOpen(abstrProj.getName()))
				continue;

			Project proj = ProjectManager.getProject(abstrProj.getName());

			AccessResponse accessResponse = ProjectManager.checkAccessibility(project, proj,
					requestedAccessLevel, requestedLockLevel);

			if (!accessResponse.isAffirmative())
				continue;

			String ns = proj.getDefaultNamespace();

			if (iriResource.getNamespace().startsWith(ns)) {
				return new LocalResourcePosition((Project) proj);
			}
		}

		DatasetMetadata meta = datasetMetadataRepository.findDatasetForResource(iriResource);

		if (meta != null) {
			return new RemoteResourcePosition(meta);
		} else {
			return UNKNOWN_RESOURCE_POSITION;
		}
	}

	/**
	 * An overload of {@link #locateResource(Project, Repository, Resource)}, with the last two
	 * parameters set to {@link AccessLevel#R} and {@link LockLevel#NO}, respectively.
	 * 
	 * @param project
	 * @param projectRepository
	 * @param resource
	 * @return
	 * @throws ProjectAccessException
	 * @throws ModelAccessException
	 */
	public ResourcePosition locateResource(Project project, Repository projectRepository,
			Resource resource) throws ProjectAccessException {
		return locateResource(project, projectRepository, resource, AccessLevel.R, LockLevel.NO);
	}
	
	/**
	 * Locate a resource. The locator implements the following algorithm:
	 * <ol>
	 * <li>if <code>resource</code> is a bnode ({@link ARTNode#isBlank()} returns <code>true</code>), then
	 * assumes it belongs to the provided project</li>
	 * <li>otherwise; <code>resource</code> is a uri, then do the following
	 * <ol>
	 * <li>if the namespace of <code>resource</code> is equal to the default namespace of <code>project</code>
	 * or <code>resource</code> is defined in any graph of <code>project</code> (see
	 * {@link RDFModel#isLocallyDefined(ARTResource, ARTResource...)}, then assumes that <code>resource</code>
	 * belongs to <code>project</code></li>
	 * <li>for each open and accessible project <code>p</code>, if the namespace of <code>resource</code> is
	 * equal to the default namespace of <code>p</code></li>, then assumes that <code>resource</code> belongs
	 * to <code>p</code></li>
	 * <li>attempt to locate <code>resource</code> in a remote dataset (see
	 * {@link DatasetMetadataRepositoryImpl#findDatasetForResource(IRI)}</li>
	 * <li>otherwise; states that the position is unknown</li>
	 * </ol>
	 * </li>
	 * </ol>
	 * 
	 * @param project
	 *            the current project
	 * @param projectRepository
	 *            the repository holding the data inside the project
	 * @param resource
	 *            the resource to be located
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @return
	 * @throws ModelAccessException
	 * @throws ProjectAccessException
	 */
	public List<ResourcePosition> listResourceLocations(Project project, Repository projectRepository,
			Resource resource, AccessLevel requestedAccessLevel, LockLevel requestedLockLevel)
			throws ProjectAccessException {
		List<ResourcePosition> resourcePositionList = new ArrayList<>();
		
		if (resource instanceof BNode) {
			resourcePositionList.add(new LocalResourcePosition(project));
			return resourcePositionList;
			//return new LocalResourcePosition(project); // TODOprojectRepository: implement a better condition
		}

		IRI iriResource = (IRI) resource;
		RepositoryConnection repoConn = RDF4JRepositoryUtils.getConnection(projectRepository);
		try {
			if (Objects.equals(repoConn.getNamespace(""), iriResource.getNamespace())
					|| repoConn.hasStatement(iriResource, null, null, false)) {
				//return new LocalResourcePosition(project);
				resourcePositionList.add(new LocalResourcePosition(project));
			}

		} finally {
			RDF4JRepositoryUtils.releaseConnection(repoConn, projectRepository);
		}

		for (AbstractProject abstrProj : ProjectManager.listProjects()) {
			if (!ProjectManager.isOpen(abstrProj.getName()))
				continue;

			Project proj = ProjectManager.getProject(abstrProj.getName());
			
			AccessResponse accessResponse = ProjectManager.checkAccessibility(project, proj,
					requestedAccessLevel, requestedLockLevel);

			if (!accessResponse.isAffirmative())
				continue;

			String ns = proj.getDefaultNamespace();

			if (iriResource.getNamespace().startsWith(ns)) {
				//return new LocalResourcePosition((Project) proj);
				resourcePositionList.add(new LocalResourcePosition(proj));
			}
		}

		DatasetMetadata meta = datasetMetadataRepository.findDatasetForResource(iriResource);

		if (meta != null) {
			//return new RemoteResourcePosition(meta);
			resourcePositionList.add(new RemoteResourcePosition(meta));
		} else {
			//return UNKNOWN_RESOURCE_POSITION;
		}
		
		return resourcePositionList;
	}
	
	/**
	 * An overload of {@link #listResourceLocations(Project, Repository, Resource)}, with the last two
	 * parameters set to {@link AccessLevel#R} and {@link LockLevel#NO}, respectively.
	 * 
	 * @param project
	 * @param projectRepository
	 * @param resource
	 * @return
	 * @throws ProjectAccessException
	 * @throws ModelAccessException
	 */
	public List<ResourcePosition> listResourceLocations(Project project, Repository projectRepository,
			Resource resource) throws ProjectAccessException {
		return listResourceLocations(project, projectRepository, resource, AccessLevel.R, LockLevel.NO);
	}
}
