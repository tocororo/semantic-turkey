package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.Collection;
import java.util.Map;

/**
 * A component able to compute the rendering of a collection of resources.
 */
public interface RenderingEngine {

	Map<ARTResource, String> render(Project<?> project, ResourcePosition subjectPosition, ARTResource subject,
			OWLModel statements, Collection<ARTResource> resources, Collection<TupleBindings> bindings, String varPrefix) throws ModelAccessException, DataAccessException;

	String getGraphPatternForDescribe(ResourcePosition resourcePosition, ARTResource resourceToBeRendered, String varPrefix);
	
}
