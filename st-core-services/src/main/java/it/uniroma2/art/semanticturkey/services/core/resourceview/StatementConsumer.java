package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;

public interface StatementConsumer {
	Map<String, ResourceViewSection> consumeStatements(Project project, RepositoryConnection repoConn,
			ResourcePosition resourcePosition, Resource resource, Model statements, Set<Statement> processedStatements,
			Resource workingGraph,
			Map<Resource, Map<String, Value>> resource2attributes, Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, Model propertyModel);

	default Set<IRI> getMatchedProperties() {
		return Collections.emptySet();
	}
}
