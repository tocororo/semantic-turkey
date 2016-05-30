package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.LinkedHashMap;
import java.util.Map;

public interface StatementConsumer {
	LinkedHashMap<String, ResourceViewSection> consumeStatements(Project<?> project, ARTResource resource,
			ResourcePosition resourcePosition, ARTResource workingGraph, RDFResourceRolesEnum resourceRole,
			StatementCollector stmtCollector, Map<ARTResource, RDFResourceRolesEnum> resource2Role,
			Map<ARTResource, String> resource2Rendering, Map<ARTResource, ARTLiteral> xLabel2LiteralForm)
					throws ModelAccessException;
}
