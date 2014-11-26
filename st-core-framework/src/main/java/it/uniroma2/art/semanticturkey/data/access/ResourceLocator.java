package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;

public class ResourceLocator {
	public static ResourcePosition locateResource(Project<?> project, ARTResource resource) throws ModelAccessException {
		

		if (resource.isBlank()) {
			return new LocalResourcePosition(project); // TODO: implement a better condition
		}
		
		ARTURIResource uriResource = resource.asURIResource();
		
		RDFModel model = project.getOntModel();
		
		if (model.getDefaultNamespace() != null && model.getDefaultNamespace().equals(uriResource.getNamespace()) || model.existsResource(uriResource, NodeFilters.ANY)) {
			return new LocalResourcePosition(project);
		}
		
		DatasetMetadata meta = DatasetMetadataRepository.getInstance().findDatasetForResource(uriResource);
		
		if (meta != null) {
			return new RemoteResourcePosition(meta);
		} else {
			return new UnknownResourcePosition();
		}
	}
}
