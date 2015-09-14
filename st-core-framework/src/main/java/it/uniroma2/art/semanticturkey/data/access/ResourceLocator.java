package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is used to locate a resource either as belonging to a currently open project or to a remote
 * dataset.
 */
public class ResourceLocator {

	@Autowired
	private DatasetMetadataRepository datasetMetadataRepository;

	public static final UnknownResourcePosition UNKNOWN_RESOURCE_POSITION = new UnknownResourcePosition();

	/**
	 * Locate a resource. The locator implements the following algorithm:
	 * <ol>
	 * <li>
	 * if <code>resource</code> is a bnode ({@link ARTNode#isBlank()} returns <code>true</code>), then assumes
	 * it belongs to the provided project</li>
	 * <li>
	 * otherwise; <code>resource</code> is a uri, the do the following
	 * <ol>
	 * <li>
	 * if the namespace of <code>resource</code> is equal to the default namespace of <code>project</code> or
	 * <code>resource</code> is defined in any graph of <code>project</code>, then assumes that
	 * <code>resource</code> belongs to <code>project</code></li>
	 * <li>
	 * for each open project <code>p</code>, if the namespace of <code>resource</code> is equal to the default
	 * namespace of <code>p</code></li>, then assumes that <code>resource</code> belongs to <code>p</code></li>
	 * <li>
	 * attempt to locate <code>resource</code> in a remote dataset (see
	 * {@link DatasetMetadataRepository#findDatasetForResource(ARTURIResource)}</li>
	 * <li>
	 * otherwise; states that the position is unknown</li>
	 * </ol>
	 * </li> </ol>
	 * 
	 * @param project
	 *            the current project
	 * @param resource
	 *            the resource to be located
	 * @return
	 * @throws ModelAccessException
	 * @throws ProjectAccessException
	 */
	public ResourcePosition locateResource(Project<?> project, ARTResource resource)
			throws ModelAccessException, ProjectAccessException {

		if (resource.isBlank()) {
			return new LocalResourcePosition(project); // TODO: implement a better condition
		}

		ARTURIResource uriResource = resource.asURIResource();

		RDFModel model = project.getOntModel();

		if (model.getDefaultNamespace() != null
				&& model.getDefaultNamespace().equals(uriResource.getNamespace())
				|| model.isLocallyDefined(uriResource, NodeFilters.ANY)) {
			return new LocalResourcePosition(project);
		}

		for (AbstractProject abstrProj : ProjectManager.listProjects()) {
			if (!ProjectManager.isOpen(abstrProj.getName()))
				continue;

			Project<?> proj = ProjectManager.getProject(abstrProj.getName());

			String ns = proj.getDefaultNamespace();

			if (ns.equals(uriResource.getNamespace())) {
				return new LocalResourcePosition((Project<?>) proj);
			}
		}

		DatasetMetadata meta = datasetMetadataRepository.findDatasetForResource(uriResource);

		if (meta != null) {
			return new RemoteResourcePosition(meta);
		} else {
			return UNKNOWN_RESOURCE_POSITION;
		}
	}
}
