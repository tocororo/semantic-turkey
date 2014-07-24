package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;

import java.util.Collection;
import java.util.Map;

/**
 * A component able to compute the rendering of a collection of resources.
 */
public interface RenderingEngine {
	
	Map<ARTResource, String> render(DatasetMetadata datasetMetadata, Collection<ARTResource> resources);

}
