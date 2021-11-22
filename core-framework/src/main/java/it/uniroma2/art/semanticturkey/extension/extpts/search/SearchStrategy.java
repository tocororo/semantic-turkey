package it.uniroma2.art.semanticturkey.extension.extpts.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.TripleForSearch;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * Common interface abstracting different search mechanisms.
 */
public interface SearchStrategy extends Extension {

	public enum StatusFilter {
		NOT_DEPRECATED, ONLY_DEPRECATED, UNDER_VALIDATION, UNDER_VALIDATION_FOR_DEPRECATION, ANYTHING
	}

	/**
	 * Performs initialization steps, such as the creation of indexes. It may be a no-op method, if no
	 * specific initialization is required.
	 */
	void initialize(RepositoryConnection connection, boolean forceCreation) throws Exception;

	/**
	 * Updates support resources (usually created inside {@link SearchStrategy#initialize(RepositoryConnection)}).
	 */
	void update(RepositoryConnection connection) throws Exception;

	String searchResource(STServiceContext stServiceContext, String searchString, String[] rolesArray,
			boolean useLexicalizations, boolean useLocalName, boolean useURI, boolean useNotes, SearchMode searchMode,
			@Nullable List<IRI> schemes, String schemeFilter, @Nullable List<String> langs, boolean includeLocales, IRI lexModel,
			boolean searchInRDFSLabel, boolean searchInSKOSLabel, boolean searchInSKOSXLLabel,
			boolean searchInOntolex, Map<String, String> prefixToNamespaceMap) throws IllegalStateException, STPropertyAccessException;

	Collection<String> searchStringList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, boolean useLocalName, SearchMode searchMode,
			@Nullable List<IRI> schemes, String schemeFilter, @Nullable List<String> langs, @Nullable IRI cls,
			boolean includeLocales) throws IllegalStateException, STPropertyAccessException;

	Collection<String> searchURIList(STServiceContext stServiceContext, String searchString,
			@Optional String[] rolesArray, SearchMode searchMode, @Nullable List<IRI> schemes,
			String schemeFilter, @Nullable IRI cls, Map<String, String> prefixToNamespaceMap, int maxNumResults) throws IllegalStateException, STPropertyAccessException;

	String searchInstancesOfClass(STServiceContext stServiceContext, List<List<IRI>> clsListList,
			String searchString, boolean  useLexicalizations, boolean useLocalName, boolean useURI, boolean useNotes,
			SearchMode searchMode, @Nullable List<String> langs, boolean includeLocales,
			boolean searchStringCanBeNull, boolean searchInSubTypes, IRI lexModel, boolean searchInRDFSLabel,
			boolean searchInSKOSLabel, boolean searchInSKOSXLLabel, boolean searchInOntolex,
			@Nullable List<List<IRI>> schemes, StatusFilter statusFilter,
			@Nullable List<Pair<IRI, List<Value>>> outgoingLinks,
			@Nullable List<TripleForSearch<IRI, String, SearchMode>> outgoingSearch,
			@JsonSerialized List<Pair<IRI, List<Value>>> ingoingLinks, SearchStrategy searchStrategy,
			String baseURI, Map<String, String> prefixToNamespaceMap) throws IllegalStateException, STPropertyAccessException;

	public String searchSpecificModePrepareQuery(String variable, String value, SearchMode searchMode,
			String indexToUse, List<String> langs, boolean includeLocales, boolean forLocalName);

	String searchLexicalEntry(STServiceContext stServiceContext, String searchString,
			boolean useLexicalizations, boolean useLocalName,
			boolean useURI, boolean useNotes, SearchMode searchMode, List<IRI> lexicons, List<String> langs,
			boolean includeLocales, IRI iri, boolean searchInRDFSLabel, boolean searchInSKOSLabel,
			boolean searchInSKOSXLLabel, boolean searchInOntolex, Map<String, String> prefixToNamespaceMap)
			throws IllegalStateException, STPropertyAccessException;
	
	//common methods used by its implementations
	public default String getAllPathRestToLexicalEntry() {
		//construct the complex path from a resource to a LexicalEntry
		// see https://www.w3.org/community/ontolex/wiki/Final_Model_Specification
		String directResToLexicalEntry = 
				"^"+NTriplesUtil.toNTriplesString(ONTOLEX.DENOTES)+
				"|"+ NTriplesUtil.toNTriplesString(ONTOLEX.IS_DENOTED_BY) +
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.EVOKES) +
				"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_EVOKED_BY);
		String doubleStepResToLexicalEntry = "("+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICALIZED_SENSE) +
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_LEXICALIZED_SENSE_OF)+
				"|^"+NTriplesUtil.toNTriplesString(ONTOLEX.REFERENCE)+
				"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_REFERENCE_OF)+")"+
				"/(^"+NTriplesUtil.toNTriplesString(ONTOLEX.SENSE)+
				"|"+NTriplesUtil.toNTriplesString(ONTOLEX.IS_SENSE_OF)+")";
		String allResToLexicalEntry = directResToLexicalEntry+"|"+doubleStepResToLexicalEntry;
		return allResToLexicalEntry;	
	}
}
