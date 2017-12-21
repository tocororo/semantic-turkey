package it.uniroma2.art.semanticturkey.plugin.extpts;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;

/**
 * Common interface abstracting different search mechanisms.
 */
public interface SearchStrategy {

	/**
	 * Performs initialization steps, such as the creation of indexes. It may be a no-op method, if no
	 * specific initialization is required.
	 */
	void initialize(RepositoryConnection connection) throws Exception;

	/**
	 * Updates support resources (usually created inside {@link SearchStrategy#initialize(Project)}).
	 */
	void update(RepositoryConnection connection) throws Exception;

	Collection<AnnotatedValue<Resource>> searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLocalName, boolean useURI,
			SearchMode searchMode, @Nullable List<IRI> schemes, @Nullable List<String> langs)
			throws IllegalStateException, STPropertyAccessException;

	Collection<String> searchStringList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, boolean useLocalName, SearchMode searchMode,
			@Nullable List<IRI> schemes, @Nullable List<String> langs, @Nullable IRI cls) throws IllegalStateException, 
			STPropertyAccessException;

	Collection<String> searchURIList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, SearchMode searchMode,
			@Nullable List<IRI> schemes, @Nullable IRI cls) throws IllegalStateException, 
			STPropertyAccessException;
	
	Collection<AnnotatedValue<Resource>> searchInstancesOfClass(STServiceContext stServiceContext, IRI cls,
			String searchString, boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Nullable List<String> langs) throws IllegalStateException, STPropertyAccessException;
}
