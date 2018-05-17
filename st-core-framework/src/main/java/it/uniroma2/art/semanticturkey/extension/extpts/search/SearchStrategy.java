package it.uniroma2.art.semanticturkey.extension.extpts.search;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;

/**
 * Common interface abstracting different search mechanisms.
 */
public interface SearchStrategy extends Extension {

	/**
	 * Performs initialization steps, such as the creation of indexes. It may be a no-op method, if no
	 * specific initialization is required.
	 */
	void initialize(RepositoryConnection connection) throws Exception;

	/**
	 * Updates support resources (usually created inside {@link SearchStrategy#initialize(Project)}).
	 */
	void update(RepositoryConnection connection) throws Exception;

	String searchResource(STServiceContext stServiceContext,
			String searchString, String[] rolesArray, boolean useLocalName, boolean useURI, boolean useNotes,
			SearchMode searchMode, @Nullable List<IRI> schemes, @Nullable List<String> langs, 
			boolean includeLocales, IRI lexModel, boolean searchInRDFSLabel, boolean searchInSKOSLabel, 
			boolean searchInSKOSXLLabel, boolean searchInOntolex)
			throws IllegalStateException, STPropertyAccessException;

	Collection<String> searchStringList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, boolean useLocalName, SearchMode searchMode,
			@Nullable List<IRI> schemes, @Nullable List<String> langs, @Nullable IRI cls, boolean includeLocales)
					throws IllegalStateException, 
			STPropertyAccessException;

	Collection<String> searchURIList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, SearchMode searchMode,
			@Nullable List<IRI> schemes, @Nullable IRI cls) throws IllegalStateException, 
			STPropertyAccessException;
	
	String searchInstancesOfClass(STServiceContext stServiceContext, List<List<IRI>> clsListList,
			String searchString, boolean useLocalName, boolean useURI, boolean useNotes, SearchMode searchMode,
			@Nullable List<String> langs, boolean includeLocales, boolean searchStringCanBeNull,
			boolean searchInSubTypes,  IRI lexModel, boolean searchInRDFSLabel, boolean searchInSKOSLabel, 
			boolean searchInSKOSXLLabel, boolean searchInOntolex) 
					throws IllegalStateException, 
			STPropertyAccessException;
	
	public String searchSpecificModePrepareQuery(String variable, String value, SearchMode searchMode, 
			String indexToUse, List<String> langs, boolean includeLocales, boolean forLocalName);

	String searchLexicalEntry(STServiceContext stServiceContext,
			String searchString, boolean useLocalName, boolean useURI, boolean useNotes, SearchMode searchMode, 
			List<IRI> lexicons, List<String> langs, boolean includeLocales, IRI iri, 
			boolean searchInRDFSLabel, boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, 
			boolean searchInOntolex) 
					throws IllegalStateException, STPropertyAccessException;
}
