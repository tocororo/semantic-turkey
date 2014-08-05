package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.Collection;
import java.util.Map;

/**
 * A component able to compute the rendering of a collection of resources.
 */
public interface RenderingEngine {

	Map<ARTResource, String> render(Project<?> project, ARTResource subject,
			Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException, DataAccessException;

}
