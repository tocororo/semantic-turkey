package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.alignment.*;
import it.uniroma2.art.semanticturkey.alignment.AlignmentModel.Status;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryStateException;
import it.uniroma2.art.semanticturkey.mdr.core.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfoUtils;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.SimpleSTServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.*;
import it.uniroma2.art.semanticturkey.services.core.search.AdvancedSearch;
import it.uniroma2.art.semanticturkey.services.core.search.AdvancedSearch.InWhatToSearch;
import it.uniroma2.art.semanticturkey.services.core.search.AdvancedSearch.WhatToShow;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@STService
public class Alignment extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Alignment.class);

	@Autowired
	protected ExtensionPointManager exptManager;

	private static final List<IRI> skosMappingRelations = Arrays.asList(SKOS.MAPPING_RELATION, SKOS.EXACT_MATCH,
			SKOS.BROAD_MATCH, SKOS.NARROW_MATCH, SKOS.CLOSE_MATCH, SKOS.RELATED_MATCH);

	private static final List<IRI> owlMappingRelations = Arrays.asList(OWL.SAMEAS, OWL.DIFFERENTFROM,
			OWL.EQUIVALENTCLASS, OWL.DISJOINTWITH, RDFS.SUBCLASSOF);

	private static final List<IRI> propertiesMappingRelations = Arrays.asList(OWL.EQUIVALENTPROPERTY,
			OWL.PROPERTYDISJOINTWITH, RDFS.SUBPROPERTYOF);

	// map that contain <id, context> pairs to handle multiple sessions
	private Map<String, AlignmentModel> modelsMap = new HashMap<>();

	public static final long DEFAULT_ALINGNMENT_PAGE_SIZE = 50;

	//@formatter:off
	//SERVICES FOR ALIGMENT FOR THE SEARCH
	/**
	 * Perform a search (a SPARQL query) on another resoure identifies by an IRI 
	 * @param searchTerm
	 * @param datasetIRI
	 * @param rolesArray
	 * @param searchModeList
	 * @param searchScope
	 * @param langs
	 * @param useIndexes
	 * @return
	 * @throws MetadataRegistryStateException 
	 * @throws NoSuchDatasetMetadataException 
	 */
	/*@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResources(String searchTerm, IRI datasetIRI, 
			String[] rolesArray, List<SearchMode> searchModeList, SearchScope searchScope, List<String> langs) {
		
		//get the datasetMetadata associated to the desired dataset
		DatasetMetadata datasetMetadata = metadataRegistryBackend.getDatasetMetadata(datasetIRI);
		if(datasetMetadata==null) {
			throw new IllegalArgumentException("dataset "+datasetIRI.stringValue()+" has no datasetMetadata "
					+ "associated");
		}
		
		//consult metadataRegistryBackend to get the LexicalModel to see in what to search (to set inWhatToSearch)
		AdvancedSearch advancedSearch = new AdvancedSearch();
		InWhatToSearch inWhatToSearch = advancedSearch.new InWhatToSearch();
		//TODO
		
		//get the connection from metadataRegistryBackend
		java.util.Optional<IRI> sparqlEndPoint = datasetMetadata.getSparqlEndpoint();
		if(!sparqlEndPoint.isPresent()) {
			throw new IllegalArgumentException("dataset "+datasetIRI.stringValue()+" has no SPARQL endpoint "
					+ "associated");
		}
		
		SPARQLRepository sparqlRepository = new SPARQLRepository(sparqlEndPoint.get().stringValue());
		sparqlRepository.initialize();
		
		RepositoryConnection conn = sparqlRepository.getConnection();
		
		//what to show is dependend on the Lexicalization Model
		WhatToShow whatToShow = advancedSearch.new WhatToShow();
		//TODO
		
		
		return advancedSearch.searchResources(searchTerm, rolesArray, searchModeList, searchScope, langs, conn, 
				inWhatToSearch, whatToShow);
	}
	*/
	//@formatter:on

	// new service

	// SERVICES FOR ALIGNMENT IN RESOURCE VIEW
	/**
	 * Returns a list of Resoruces which are "similar" to the one in input. This service should be used to
	 * obtain a list of possible candidate for an alignment between a local resource and resources in a remote
	 * dataset
	 * 
	 * @param inputRes
	 *            the input resources from which to obtain the lexicalizations used in the search in the
	 *            remote dataset
	 * @param resourcePosition
	 *            the remote dataset or a local project
	 * @param rolesArray
	 *            the roles to which the returned resources should belong to
	 * @param searchModeList
	 *            the optional list of searchMode that will be used in the search (is no value is passed, then
	 *            'contains' and 'fuzzy' are used)
	 * @param langToLexModel
	 *
	 * @return the list of remote resources obtained from the search
	 * @throws MetadataRegistryStateException
	 * @throws NoSuchDatasetMetadataException
	 * @throws STPropertyAccessException
	 * @throws IllegalStateException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResources(IRI inputRes,
			ResourcePosition resourcePosition, String[] rolesArray, @Optional List<SearchMode> searchModeList,
			Map<String, IRI> langToLexModel) throws IllegalStateException, STPropertyAccessException {

		SearchStrategy regexSearchStrategy = instantiateSearchStrategy(SearchStrategies.REGEX);


		logger.debug("starting Alignment.serachResources");
		//@formatter:off
		//if not searchModeList is passed, then assume they are contains and fuzzy (since those two contains 
		// all the others)
		if(searchModeList == null || searchModeList.size()==0) {
			searchModeList = new ArrayList<>();
			searchModeList.add(SearchMode.contains);
			searchModeList.add(SearchMode.fuzzy);
		}
		
		//check that all passed targetLexModel hve one of the right value
		for(IRI targetLexModel : langToLexModel.values()) {
			if(!targetLexModel.equals(Project.RDFS_LEXICALIZATION_MODEL) &&
					!targetLexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) &&
					!targetLexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL) &&
					!targetLexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
				throw new IllegalArgumentException("targetLexModel "+targetLexModel.stringValue()+" is not a "
						+ "valid Lexicalization Model  ");
			}
		}
		
		
		//consult metadataRegistryBackend to get the LexicalModel to see in what to search (to set inWhatToSearch)
		AdvancedSearch advancedSearch = new AdvancedSearch();
		///now get all the lexicalization in the desired languages, using the current Lexical Model
		IRI currLexModel =  getProject().getLexicalizationModel();
		List<String> langs = new ArrayList<>();
		langs.addAll(langToLexModel.keySet());
		List<Literal> labelsList = advancedSearch.getLabelsFromLangs(stServiceContext, inputRes, 
				currLexModel, langs, getManagedConnection());
		
		Collection<AnnotatedValue<Resource>> annValueList;
		
		//according to the resourcePosition, behave in a specific way
		if(resourcePosition instanceof LocalResourcePosition) {
			//it is another local project, so get the project and construct an instance of a redux 
			// STServiceContext
			Project otherProject = ((LocalResourcePosition)resourcePosition).getProject();
			//TODO test if it is enough to pass the otherProject or if other infos are needed by 
			// instantiateSearchStrategy and QueryBuilder
			SimpleSTServiceContext simpleSTServiceContext = new SimpleSTServiceContext(otherProject);
			
			IRI lexModel = otherProject.getLexicalizationModel();
			
			SearchStrategies searchStrategy = STRepositoryInfoUtils.getSearchStrategy(
					otherProject.getRepositoryManager().getSTRepositoryInfo("core"));

			
			//the structures containing the results, which will be later ordered and returned
			//Map<String, Integer> resToCountMap = new HashMap<>();
			Map<String, AnnotatedValue<Resource>> resToAnnValueMap = new HashMap<>();
			//int maxCount = 0;
			
			//iterate over the labelsList, get the associated language and then get the lexModel from langToLexModel for 
			// such language and execute one query per language-LexModel
			for(Literal label : labelsList) {

				//for every element of searchModeList do a search
				for(SearchMode searchMode : searchModeList) {
				
					String lang = label.getLanguage().get();
					String query = ServiceForSearches.getPrefixes() + "\n"
							+ SearchStrategyUtils.instantiateSearchStrategy(exptManager, searchStrategy)
								.searchResource(simpleSTServiceContext, label.getLabel(), rolesArray, true,
									false, false, false, searchMode, null, "or",
										langs, false, lexModel,
									false, false, false, false, null);
		
					logger.debug("query = " + query);
		
					QueryBuilder qb;
					qb = new QueryBuilder(simpleSTServiceContext, query);
					qb.processRendering();
					//execute the SPARQL query and obtain the results
					Collection<AnnotatedValue<Resource>> currentAnnValuList = qb.runQuery();
					for(AnnotatedValue<Resource> annValue : currentAnnValuList) {
						String stringValue = annValue.getStringValue();
						if(resToAnnValueMap.containsKey(stringValue)) {
							//there is already an AnnotatedValue for the retrieve resource, so add the new matching 
							// language
							String matchedLang = resToAnnValueMap.get(stringValue).getAttributes()
									.get("matchedLang").stringValue();
							boolean addLang = true;
							String[] langsMatched = matchedLang.split(",");
							for (String prevMatchedLang : langsMatched) {
								if (prevMatchedLang.equals(lang)) {
									addLang = false;
									break;
								}
							}
							if(addLang) {
								matchedLang+=","+lang;
								resToAnnValueMap.get(stringValue).setAttribute("matchedLang", matchedLang);
							}
							//check if this returned AnnotatedValue has a different matchMode, if so
							// add it to the list ones already present in the 
							String matchedMode = resToAnnValueMap.get(stringValue).getAttributes()
									.get("matchMode").stringValue();
							boolean addMatchMode = true;
							String[] matchedModeArray = matchedMode.split(",");
							for (String singleMatchedMode : matchedModeArray) {
								if (singleMatchedMode.equals(annValue.getAttributes().get("matchMode").stringValue())) {
									addMatchMode = false;
								}
							}
							if(addMatchMode) {
								//add the matchMode from the annValue to the Annotated Valued stored in the map
								matchedMode+=","+annValue.getAttributes().get("matchMode").stringValue();
								resToAnnValueMap.get(stringValue).setAttribute("matchMode", matchedMode);
							}
						} else {
							//it is the first AnnotatedValue returned for the retrieve Resource
							annValue.setAttribute("matchedLang", lang);
							resToAnnValueMap.put(stringValue, annValue);
						}
					}
				}
			}
			return orderResultsFromSearch(resToAnnValueMap.values());
		} else if(resourcePosition instanceof RemoteResourcePosition) {
		
			//get the datasetMetadata associated to the desired dataset
			
			DatasetMetadata datasetMetadata = ((RemoteResourcePosition) resourcePosition).getDatasetMetadata();
			if(datasetMetadata==null) {
				throw new IllegalArgumentException("dataset "+resourcePosition.getPosition()+" has no datasetMetadata "
						+ "associated");
			}
			
			//get the SPARQL endpoint from metadataRegistryBackend
			java.util.Optional<IRI> sparqlEndPoint = datasetMetadata.getSparqlEndpoint();
			if(!sparqlEndPoint.isPresent()) {
				throw new IllegalArgumentException("dataset "+datasetMetadata.getIdentity()+" has no SPARQL endpoint "
						+ "associated");
			}
			
			logger.debug("SPARQL endpoint = "+sparqlEndPoint.get().stringValue());
			SPARQLRepository sparqlRepository = new SPARQLRepository(sparqlEndPoint.get().stringValue());
			sparqlRepository.init();
			
			RepositoryConnection conn = sparqlRepository.getConnection();
			
			//call the function to obtain the desired list of annotated Resources
			annValueList = searchResourcesForRemote(labelsList, conn, rolesArray, searchModeList, langToLexModel,
					regexSearchStrategy);
		} else {
			throw new IllegalArgumentException("Unsupported resource position");
		}
		
		return orderResultsFromSearch(annValueList);
		//@formatter:on
	}

	public Collection<AnnotatedValue<Resource>> searchResourcesForRemote(List<Literal> labelsList,
			RepositoryConnection remoteConn, String[] rolesArray, List<SearchMode> searchModeList,
			Map<String, IRI> langToLexModel, SearchStrategy regexSearchStrategy) {

		// check that the optional targetLexModel has one of the right value
		if (langToLexModel == null || langToLexModel.size() == 0) {
			throw new IllegalArgumentException(
					"targetLexModel is mandatory is not a " + "valid Lexicalization Model  ");
		}

		// check that all passed targetLexModel hve one of the right value
		for (IRI targetLexModel : langToLexModel.values()) {
			if (!targetLexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)
					&& !targetLexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)
					&& !targetLexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)
					&& !targetLexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
				throw new IllegalArgumentException("targetLexModel " + targetLexModel.stringValue()
						+ " is not a " + "valid Lexicalization Model  ");
			}
		}

		// consult metadataRegistryBackend to get the LexicalModel to see in what to search (to set
		// inWhatToSearch)
		AdvancedSearch advancedSearch = new AdvancedSearch();

		// if not searchModeList is passed, then assume they are contains and fuzzy (since those two contains
		// all the others)
		if (searchModeList == null || searchModeList.size() == 0) {
			throw new IllegalArgumentException("At least one searchMode should be passed");
		}

		// the structures containing the results, which will be later ordered and returned
		// Map<String, Integer> resToCountMap = new HashMap<>();
		Map<String, AnnotatedValue<Resource>> resToAnnValueMap = new HashMap<>();
		// int maxCount = 0;

		// iterate over the labelsList, get the associated language and then get the lexModel from
		// langToLexModel for
		// such language and execute one query per language-LexModel
		for (Literal label : labelsList) {
			if (!label.getLanguage().isPresent()) {
				// the is no language in this Literal
				continue;
			}
			String lang = label.getLanguage().get();
			IRI targetLexModel = langToLexModel.get(lang);

			InWhatToSearch inWhatToSearch = advancedSearch.new InWhatToSearch();
			WhatToShow whatToShow = advancedSearch.new WhatToShow();
			// according to the Lexicalization model of the target dataset, set the corresponding values in
			// inWhatToSearch and whatToShow
			if (targetLexModel.equals(Project.RDFS_LEXICALIZATION_MODEL)) {
				inWhatToSearch.setSearchInRDFLabel(true);
				whatToShow.setShowRDFLabel(true);
			} else if (targetLexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)) {
				inWhatToSearch.setSearchInSkosLabel(true);
				whatToShow.setShowSKOSLabel(true);
			} else if (targetLexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
				inWhatToSearch.setSearchInSkosxlLabel(true);
				whatToShow.setShowSKOSXLLabel(true);
			} else if (targetLexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
				inWhatToSearch.setSearchInDCTitle(true);
				inWhatToSearch.setSearchInWrittenRep(true);
				whatToShow.setShowDCTitle(true);
				whatToShow.setShowWrittenRep(true);
			}
			// add the language regarding the show
			for (String langToShow : langToLexModel.keySet()) {
				whatToShow.addLang(langToShow);
			}

			// do one search per label
			List<AnnotatedValue<Resource>> currentAnnValuList = advancedSearch.searchResources(label,
					rolesArray, searchModeList, remoteConn, targetLexModel, inWhatToSearch, whatToShow, regexSearchStrategy);

			// analyze the return list and find a way to rank the results
			// maybe order them according to how may times a resource is returned
			for (AnnotatedValue<Resource> annValue : currentAnnValuList) {
				String stringValue = annValue.getStringValue();
				if (resToAnnValueMap.containsKey(stringValue)) {
					// there is already an AnnotatedValue for the retrieve resource, so add the new matching
					// language
					String matchedLang = resToAnnValueMap.get(stringValue).getAttributes().get("matchedLang")
							.stringValue() + "," + lang;
					resToAnnValueMap.get(stringValue).setAttribute("matchedLang", matchedLang);
				} else {
					// it is the first AnnotatedValue returned for the retrieve Resource
					annValue.setAttribute("matchedLang", lang);
					resToAnnValueMap.put(stringValue, annValue);
				}
			}
		}
		// all labels have been analyze, so return the Annotated Value
		return resToAnnValueMap.values();
	}

	public Collection<AnnotatedValue<Resource>> orderResultsFromSearch(
			Collection<AnnotatedValue<Resource>> annList) {
		List<AnnotatedValue<Resource>> orderedAnnValueList = new ArrayList<>();

		List<AnnotatedValue<Resource>> restAnnValueList = new ArrayList<>();

		// order the result by placing first the one obtained from an exact match
		for (AnnotatedValue<Resource> annValue : annList) {
			String matchType = annValue.getAttributes().get("matchMode").stringValue();
			if (matchType.contains("exact")) {
				orderedAnnValueList.add(annValue);
			} else {
				restAnnValueList.add(annValue);
			}
		}

		// add to the ordered list the other elements
		orderedAnnValueList.addAll(restAnnValueList);

		return orderedAnnValueList;
	}

	/**
	 * Returns the number of available mappings
	 * 
	 * @param targetUriPrefix
	 * @param mappingProperties
	 * @param expressInPages
	 *            whether the count should be an absolute number or expressed as the number of pages
	 * @param pageSize
	 *            if less or equal to zero, then everything goes into one page
	 * 
	 * @return
	 * @throws ProjectInconsistentException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Long getMappingCount(String targetUriPrefix,
			@Optional(defaultValue = "") List<IRI> mappingProperties,
			@Optional(defaultValue = "false") boolean expressInPages,
			@Optional(defaultValue = "" + DEFAULT_ALINGNMENT_PAGE_SIZE) long pageSize) {
		if (mappingProperties.isEmpty()) {
			mappingProperties = new ArrayList<>();
			mappingProperties.addAll(skosMappingRelations);
			mappingProperties.addAll(owlMappingRelations);
			mappingProperties.addAll(propertiesMappingRelations);
		}

		TupleQuery query = getManagedConnection().prepareTupleQuery(
		//@formatter:off
			"SELECT (COUNT(*) as ?count) {\n" + 
			"  ?s ?p ?o .\n" + 
			"  FILTER(STRSTARTS(STR(?o), ?targetUriPrefix))\n" + 
			"  FILTER(STRSTARTS(STR(?s), ?sourceUriPrefix))\n" + 
			"}\n" + 
			"values(?p){" + mappingProperties.stream().map(p -> "(" + RenderUtils.toSPARQL(p) + ")").collect(Collectors.joining())+"}\n" 
			//@formatter:on
		);

		query.setIncludeInferred(false);
		query.setBinding("sourceUriPrefix",
				SimpleValueFactory.getInstance().createLiteral(getProject().getDefaultNamespace()));
		query.setBinding("targetUriPrefix", SimpleValueFactory.getInstance().createLiteral(targetUriPrefix));

		long count = Literals
				.getLongValue(QueryResults.asList(query.evaluate()).iterator().next().getValue("count"), 0);
		if (expressInPages) {
			if (pageSize < 0) {
				return count != 0 ? 1l : 0l;
			} else {
				// ceil division
				return (count + pageSize - 1) / pageSize;
			}
		} else {
			return count;
		}
	}

	/**
	 * Returns the available mappings
	 * 
	 * @param targetUriPrefix
	 * @param page
	 * @param pageSize
	 * @param mappingProperties
	 * 
	 * @return
	 * @throws ProjectInconsistentException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<Triple<IRI, IRI, IRI>> getMappings(String targetUriPrefix,
			@Optional(defaultValue = "0") int page,
			@Optional(defaultValue = "" + DEFAULT_ALINGNMENT_PAGE_SIZE) int pageSize,
			@Optional(defaultValue = "") List<IRI> mappingProperties) {

		if (mappingProperties.isEmpty()) {
			mappingProperties = new ArrayList<>();
			mappingProperties.addAll(skosMappingRelations);
			mappingProperties.addAll(owlMappingRelations);
			mappingProperties.addAll(propertiesMappingRelations);
		}

		TupleQuery query = getManagedConnection().prepareTupleQuery(
		//@formatter:off
			"SELECT ?s ?p ?o {\n" + 
			"  ?s ?p ?o .\n" + 
			"  FILTER(STRSTARTS(STR(?o), ?targetUriPrefix))\n" + 
			"  FILTER(STRSTARTS(STR(?s), ?sourceUriPrefix))\n" + 
			"}\n" + 
			"offset " + (page * pageSize) + "\n" + 
			(pageSize <= 0 ? "" : "limit " + pageSize + "\n") +
			"values(?p){" + mappingProperties.stream().map(p -> "(" + RenderUtils.toSPARQL(p) + ")").collect(Collectors.joining())+"}\n" 
			//@formatter:on
		);

		query.setIncludeInferred(false);
		query.setBinding("sourceUriPrefix",
				SimpleValueFactory.getInstance().createLiteral(getProject().getDefaultNamespace()));
		query.setBinding("targetUriPrefix", SimpleValueFactory.getInstance().createLiteral(targetUriPrefix));

		return QueryResults.stream(query.evaluate()).map(bs -> ImmutableTriple.of((IRI) bs.getValue("s"),
				(IRI) bs.getValue("p"), (IRI) bs.getValue("o"))).collect(Collectors.toList());
	}

	/**
	 * Adds the given alignment triple only if predicate is a valid alignment property
	 * 
	 * @param sourceResource
	 * @param predicate
	 * @param targetResource
	 * @return
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#sourceResource)+ ', alignment)', 'C')")
	public void addAlignment(Resource sourceResource, IRI predicate, IRI targetResource) {
		RepositoryConnection repoConn = getManagedConnection();
		repoConn.add(sourceResource, predicate, targetResource, getWorkingGraph());
	}

	/**
	 * Returns the available alignment properties depending on the resource role to align (property, or
	 * concept, or class,...).
	 * 
	 * @param role
	 *            role of resource to align
	 * @param allMappingProps
	 *            if false returns just the mapping properties available for the current model type; if true
	 *            returns all the mapping properties independently from the model type
	 * @return
	 * @throws ProjectInconsistentException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<IRI>> getMappingProperties(RDFResourceRole role,
			@Optional(defaultValue = "false") boolean allMappingProps) {
		List<IRI> props = getAvailableMappingProperties(role, allMappingProps);
		Collection<AnnotatedValue<IRI>> propColl = new ArrayList<>();
		for (IRI p : props) {
			AnnotatedValue<IRI> annValue = new AnnotatedValue<>(p);
			annValue.setAttribute("show", getPropertyQName(p));
			propColl.add(annValue);
		}
		return propColl;
	}

	// SERVICES FOR ALIGNMENT VALIDATION

	/**
	 * Loads an alignment file (that is compliant with AlignmentAPI format) and if one of the two aligned
	 * ontologies has the same baseURI of the current model, then return a response with its content.
	 * 
	 * @param inputFile
	 * @return
	 * @throws AlignmentInitializationException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public JsonNode loadAlignment(MultipartFile inputFile, @Optional Project leftProject, @Optional Project rightProject)
			throws AlignmentInitializationException, IOException {
		
		// create a temp file (in karaf data/temp folder) to copy the received file
		File inputServerFile = File.createTempFile("alignment", inputFile.getOriginalFilename());
		inputFile.transferTo(inputServerFile);

		// creating model for loading alignment
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		Repository repository = new SailRepository(memStore);
		repository.init();
		AlignmentModel alignModel = new AlignmentModel();
		alignModel.add(inputServerFile);

		return loadAlignmentHelper(alignModel, leftProject, rightProject);
	}
	
	/**
	 * This method is responsible for the finalization of alignment load operations, irrespectively of the
	 * source of the alignment (e.g. file, GENOMA task, etc...)
	 * 
	 * @param alignModel
	 * @return
	 * @throws AlignmentInitializationException
	 */
	public JsonNode loadAlignmentHelper(AlignmentModel alignModel, Project leftProject, Project rightProject) throws AlignmentInitializationException {
		String token = stServiceContext.getSessionToken();
		modelsMap.put(token, alignModel);

		performProjectsCheck(alignModel, leftProject, rightProject);
		
		alignModel.preProcess();

		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode alignmentNode = jsonFactory.objectNode();
		alignmentNode.set("onto1", jsonFactory.textNode(alignModel.getOnto1()));
		alignmentNode.set("onto2", jsonFactory.textNode(alignModel.getOnto2()));

		ArrayNode unknownRelationsArrayNode = jsonFactory.arrayNode();
		List<String> unknownRel = alignModel.getUnknownRelations();
		for (String rel : unknownRel) {
			unknownRelationsArrayNode.add(jsonFactory.textNode(rel));
		}
		alignmentNode.set("unknownRelations", unknownRelationsArrayNode);
		return alignmentNode;
	}
	
	private void performProjectsCheck(AlignmentModel alignModel, Project leftProject, Project rightProject) throws AlignmentInitializationException {
		String onto1BaseURI = alignModel.getOnto1();
		String onto2BaseURI = alignModel.getOnto2();
		
		if (leftProject == null) { //left project not provided => not an EDOAL project => get the current project
			leftProject = getProject();
		}
		
		// removes final / or # from baseURIs in order to avoid failing of the check on the baseURIs match
		String leftProjectBaseURI = leftProject.getNewOntologyManager().getBaseURI();
		if (leftProjectBaseURI.endsWith("/") || leftProjectBaseURI.endsWith("#")) {
			leftProjectBaseURI = leftProjectBaseURI.substring(0, leftProjectBaseURI.length() - 1);
		}
		if (onto1BaseURI.endsWith("/") || onto1BaseURI.endsWith("#")) {
			onto1BaseURI = onto1BaseURI.substring(0, onto1BaseURI.length() - 1);
		}
		if (onto2BaseURI.endsWith("/") || onto2BaseURI.endsWith("#")) {
			onto2BaseURI = onto2BaseURI.substring(0, onto2BaseURI.length() - 1);
		}
		
		if (rightProject != null) {
			//EDOAL => both the aligned ontologies (1 and 2) must refer to the two datasets of the EDOAL project
			String rightProjectBaseURI = rightProject.getNewOntologyManager().getBaseURI();
			if (rightProjectBaseURI.endsWith("/") || rightProjectBaseURI.endsWith("#")) {
				rightProjectBaseURI = rightProjectBaseURI.substring(0, rightProjectBaseURI.length() - 1);
			}
			
			if (!leftProjectBaseURI.equals(onto1BaseURI) || !rightProjectBaseURI.equals(onto2BaseURI)) {
				throw new AlignmentNotMatchingProjectsException();
			}
		} else { //not EDOAL => one of the two ontologies must refer to the current project
			if (!leftProjectBaseURI.equals(onto1BaseURI)) {
				if (leftProjectBaseURI.equals(onto2BaseURI)) {
					if (alignModel.hasCustomRelation()) {
						throw new ReversedAlignmentWithCustomRelationsException();
					}
					alignModel.reverse();
				} else {
					throw new AlignmentNotMatchingCurrentProject();
				}
			}
		}
	}
	
	
	/**
	 * Returns the cells of the alignment file. Handles the scalability returning a portion of cells if
	 * <code>pageIdx</code> and <code>range</code> are provided as parameters
	 * 
	 * @param pageIdx
	 *            index of the page in case
	 * @param range
	 *            alignment per page to show. If 0, returns all the alignments.
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public JsonNode listCells(@Optional(defaultValue = "0") int pageIdx,
			@Optional(defaultValue = "0") int range) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode mapNode = jsonFactory.objectNode();
		ArrayNode cellArrayNode = jsonFactory.arrayNode();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cells = alignModel.listCells();
		// if range = 0 => return all cells
		if (range == 0) {
			mapNode.set("page", jsonFactory.numberNode(1));
			mapNode.set("totPage", jsonFactory.numberNode(1));
			for (Cell c : cells) {
				cellArrayNode.add(createCellJsonNode(c));
			}
		} else {
			int size = cells.size();
			int begin = pageIdx * range;
			int end = begin + range;

			// if index of first cell > size of cell list (index out of bound) => return empty list of cells
			if (begin > size) {
				mapNode.set("page", jsonFactory.numberNode(1));
				mapNode.set("totPage", jsonFactory.numberNode(1));
			} else {
				int maxPage = size / range;
				if (size % range > 0) {
					maxPage++;
				}
				mapNode.set("page", jsonFactory.numberNode(pageIdx));
				mapNode.set("totPage", jsonFactory.numberNode(maxPage));
				if (end > size) {
					end = size;
				}
				for (int i = begin; i < end; i++) {
					cellArrayNode.add(createCellJsonNode(cells.get(i)));
				}
			}
		}
		mapNode.set("cells", cellArrayNode);
		return mapNode;
	}

	/**
	 * Accepts the alignment updating the alignment model
	 * 
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#entity1)+ ', alignment)', 'C')")
	public JsonNode acceptAlignment(IRI entity1, IRI entity2, String relation, @Optional IRI forcedProperty,
			@Optional(defaultValue = "false") boolean setAsDefault) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAlignment(entity1, entity2, relation, forcedProperty, setAsDefault,
				getManagedConnection());
		Cell c = alignModel.getCell(entity1, entity2, relation);
		return createCellJsonNode(c);
	}

	/**
	 * Accepts all the alignment updating the alignment model
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'C')")
	public JsonNode acceptAllAlignment() {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.acceptAllAlignment(getManagedConnection());
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode cellsArrayNode = jsonFactory.arrayNode();
		List<Cell> cells = alignModel.listCells();
		for (Cell c : cells) {
			cellsArrayNode.add(createCellJsonNode(c));
		}
		return cellsArrayNode;
	}

	/**
	 * Accepts all the alignment with measure above the given threshold updating the alignment model. The
	 * response contains the description of all the cells affected by the accept
	 * 
	 * @param threshold
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'C')")
	public JsonNode acceptAllAbove(float threshold) {
		ArrayNode cellsArrayNode = JsonNodeFactory.instance.arrayNode();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure >= threshold) {
				IRI entity1 = cell.getEntity1();
				IRI entity2 = cell.getEntity2();
				String relation = cell.getRelation();
				alignModel.acceptAlignment(entity1, entity2, relation, null, false, getManagedConnection());
				Cell updatedCell = alignModel.getCell(entity1, entity2, relation);
				cellsArrayNode.add(createCellJsonNode(updatedCell));
			}
		}
		return cellsArrayNode;
	}

	/**
	 * Rejects the alignment
	 * 
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', '')")
	public JsonNode rejectAlignment(IRI entity1, IRI entity2, String relation) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.rejectAlignment(entity1, entity2, relation);
		Cell c = alignModel.getCell(entity1, entity2, relation);
		return createCellJsonNode(c);
	}

	/**
	 * Rejects all the alignments
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', '')")
	public JsonNode rejectAllAlignment() {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.rejectAllAlignment();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode cellsArrayNode = jsonFactory.arrayNode();
		List<Cell> cells = alignModel.listCells();
		for (Cell c : cells) {
			cellsArrayNode.add(createCellJsonNode(c));
		}
		return cellsArrayNode;
	}

	/**
	 * Rejects all the alignments under the given threshold
	 * 
	 * @param threshold
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', '')")
	public JsonNode rejectAllUnder(float threshold) {
		ArrayNode cellsArrayNode = JsonNodeFactory.instance.arrayNode();
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		List<Cell> cellList = alignModel.listCells();
		for (Cell cell : cellList) {
			float measure = cell.getMeasure();
			if (measure < threshold) {
				IRI entity1 = cell.getEntity1();
				IRI entity2 = cell.getEntity2();
				String relation = cell.getRelation();
				alignModel.rejectAlignment(entity1, entity2, relation);
				Cell updatedCell = alignModel.getCell(entity1, entity2, relation);
				cellsArrayNode.add(createCellJsonNode(updatedCell));
			}
		}
		return cellsArrayNode;
	}

	/**
	 * Change the relation of an alignment
	 * 
	 * @param entity1
	 * @param entity2
	 * @param oldRelation
	 * @return
	 */
	@STServiceOperation
	@Read
	public JsonNode changeRelation(IRI entity1, IRI entity2, String oldRelation, String newRelation) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.updateRelation(entity1, entity2, oldRelation, newRelation, 1.0f); //
		Cell updatedCell = alignModel.getCell(entity1, entity2, newRelation);
		return createCellJsonNode(updatedCell);
	}

	/**
	 * Change the mapping property of an alignment
	 * 
	 * @param entity1
	 * @param entity2
	 * @param relation
	 * @param mappingProperty
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'U')")
	public JsonNode changeMappingProperty(IRI entity1, IRI entity2, String relation, IRI mappingProperty) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		alignModel.changeMappingProperty(entity1, entity2, relation, mappingProperty);
		Cell updatedCell = alignModel.getCell(entity1, entity2, relation);
		return createCellJsonNode(updatedCell);
	}

	/**
	 * Adds the accepted alignment cell to the ontology model and delete the rejected ones (if previously
	 * added to the ontology)
	 * 
	 * @param deleteRejected
	 *            tells if remove the triples related to rejected alignments
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'CUD')")
	public JsonNode applyValidation(@Optional(defaultValue = "false") boolean deleteRejected) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode reportArrayNode = jsonFactory.arrayNode();

		RepositoryConnection repoConn = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());

		List<Cell> acceptedCells = alignModel.listCellsByStatus(Status.accepted);
		for (Cell cell : acceptedCells) {
			modelAdditions.add(cell.getEntity1(), cell.getMappingProperty(), cell.getEntity2());
			ObjectNode cellNode = jsonFactory.objectNode();
			cellNode.set("entity1", jsonFactory.textNode(cell.getEntity1().stringValue()));
			cellNode.set("entity2", jsonFactory.textNode(cell.getEntity2().stringValue()));
			cellNode.set("property", jsonFactory.textNode(cell.getMappingProperty().stringValue()));
			cellNode.set("action", jsonFactory.textNode("Added"));
			reportArrayNode.add(cellNode);
		}

		if (deleteRejected) {
			List<Cell> rejectedCells = alignModel.listCellsByStatus(Status.rejected);
			for (Cell cell : rejectedCells) {
				try {
					IRI entity1 = cell.getEntity1();
					IRI entity2 = cell.getEntity2();
					List<IRI> props = alignModel.suggestPropertiesForRelation(entity1, cell.getRelation(),
							false, repoConn);
					for (IRI p : props) {
						if (repoConn.hasStatement(entity1, p, entity2, true, getWorkingGraph())) {
							modelRemovals.add(entity1, p, entity2);
							ObjectNode cellNode = jsonFactory.objectNode();
							cellNode.set("entity1", jsonFactory.textNode(cell.getEntity1().stringValue()));
							cellNode.set("entity2", jsonFactory.textNode(cell.getEntity2().stringValue()));
							cellNode.set("property", jsonFactory.textNode(p.stringValue()));
							cellNode.set("action", jsonFactory.textNode("Deleted"));
							reportArrayNode.add(cellNode);
						}
					}
				} catch (InvalidAlignmentRelationException e) {
				} // in case of invalid relation, simply do nothing
			}
		}

		repoConn.add(modelAdditions, getWorkingGraph());
		repoConn.remove(modelRemovals, getWorkingGraph());

		return reportArrayNode;
	}

	/**
	 * Adds the accepted alignment cell to the EDOAL model and delete the rejected ones (if previously added).
	 *
	 * Note: this service needs to be here and cannot be moved to Edoal class since I need to retrieve the
	 * alignment model from modelMap that is stored here
	 *
	 * @param deleteRejected
	 *            tells if remove the linkset related to rejected alignments
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
//	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'CUD')")
	public void applyValidationToEdoal(@Optional(defaultValue = "false") boolean deleteRejected) {
		AlignmentModel alignModel = modelsMap.get(stServiceContext.getSessionToken());
		RepositoryConnection repoConn = getManagedConnection();

		List<Cell> acceptedCells = alignModel.listCellsByStatus(Status.accepted);
		if (!acceptedCells.isEmpty()) {
			//TODO check for duplicate
			String insertQuery = "PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" +
					"INSERT {\n" +
					"  GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
					"    ?alignment align:map [\n" +
					"      a align:Cell ;\n" +
					"        align:entity1 ?leftEntity ;\n" +
					"        align:entity2 ?rightEntity ;\n" +
					"        align:relation ?relation ;\n" +
					"        align:measure ?measure;\n" +
					"        align:mappingProperty ?mappingProperty\n" +
					"    ] .\n" +
					"  }\n" +
					"} WHERE {\n" +
					"  ?alignment a align:Alignment . \n" +
					"  values (?leftEntity ?rightEntity ?relation ?measure ?mappingProperty) {\n";
			for (Cell cell : acceptedCells) {
				insertQuery += "( " + NTriplesUtil.toNTriplesString(cell.getEntity1()) + " "
						+ NTriplesUtil.toNTriplesString(cell.getEntity2()) + " "
						+ "'" + cell.getRelation() + "' "
						+ cell.getMeasure() + " "
						+ NTriplesUtil.toNTriplesString(cell.getMappingProperty()) + " )\n";
			}
			insertQuery += "}\n}"; //close values and where
			Update update = repoConn.prepareUpdate(insertQuery);
			update.execute();
		}

		if (deleteRejected) {
			List<Cell> rejectedCells = alignModel.listCellsByStatus(Status.rejected);
			if (!rejectedCells.isEmpty()) {
				String deleteQuery = "PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" +
						"DELETE {\n" +
						"  GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
						"    ?alignment align:map ?cell .\n" +
						"    ?cell ?p ?o .\n" +
						"  }\n" +
						"} WHERE {\n" +
						"  ?alignment a align:Alignment . \n" +
						"  ?cell a align:Cell . \n" +
						"  ?cell align:entity1 ?leftEntity .\n" +
						"  ?cell align:entity2 ?rightEntity .\n" +
						"  ?cell align:relation ?relation .\n" +
						"  ?cell align:measure ?measure .\n" +
						"  values (?leftEntity ?rightEntity ?relation ?measure) {\n";
				for (Cell cell : rejectedCells) {
					deleteQuery += "  ( " + NTriplesUtil.toNTriplesString(cell.getEntity1()) + " "
							+ NTriplesUtil.toNTriplesString(cell.getEntity2()) + " "
							+ "'" + cell.getRelation() + "' "
							+ cell.getMeasure() + " )\n";
				}
				deleteQuery += "}\n}"; //close values and where
				Update update = repoConn.prepareUpdate(deleteQuery);
				update.execute();
			}
		}
	}

	/**
	 * Save the alignment with the performed changes and export as rdf file
	 * 
	 * @param oRes
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public void exportAlignment(HttpServletResponse oRes) throws IOException {
		AlignmentModel alignmentModel = modelsMap.get(stServiceContext.getSessionToken());
		File tempServerFile = File.createTempFile("alignment", ".rdf");
		try {
			alignmentModel.serialize(tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=alignment.rdf");
			oRes.setContentType(RDFFormat.RDFXML.getDefaultMIMEType());
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Return a list of mapping properties suggested for the given entity and the alignment relation
	 * 
	 * @param role
	 * @param relation
	 * @return
	 * @throws InvalidAlignmentRelationException
	 * @throws ProjectInconsistentException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource, alignment)', 'R')")
	public Collection<AnnotatedValue<IRI>> getSuggestedProperties(RDFResourceRole role, String relation) {
		List<IRI> props;
		try {
			props = AlignmentUtils.suggestPropertiesForRelation(role, relation);
		} catch (InvalidAlignmentRelationException e) {
			/* If it is not possible to find properties to suggest, (probably because the relation is not
			known) returns all the mapping properties. */
			props = getAvailableMappingProperties(role, false);
		}

		Collection<AnnotatedValue<IRI>> propColl = new ArrayList<>();
		for (IRI p : props) {
			AnnotatedValue<IRI> annValue = new AnnotatedValue<>(p);
			annValue.setAttribute("show", getPropertyQName(p));
			propColl.add(annValue);
		}
		return propColl;
	}

	/**
	 * Returns a list of mapping properties compliant to the given role
	 * 
	 * @param role
	 * @param allMappingProps
	 *            if true, returns all the known mapping properties, if false filters out the properties not
	 *            compatible with the resource and the project type
	 * @return
	 * @throws ProjectInconsistentException
	 */
	private List<IRI> getAvailableMappingProperties(RDFResourceRole role, boolean allMappingProps) {
		List<IRI> mappingProps = new ArrayList<>();
		if (allMappingProps) {
			mappingProps.addAll(propertiesMappingRelations);
			mappingProps.addAll(skosMappingRelations);
			mappingProps.addAll(owlMappingRelations);
		} else {
			boolean isProperty = RDFResourceRole.isProperty(role);
			if (isProperty) { // is Property?
				mappingProps.addAll(propertiesMappingRelations);
			} else {
				if (getProject().getModel().equals(Project.SKOS_MODEL)) { // SKOS or SKOSXL
					mappingProps.addAll(skosMappingRelations);
				} else { // OWL
					mappingProps.addAll(owlMappingRelations);
				}
			}
		}
		return mappingProps;
	}

	/**
	 * Returns the qname of a property if a known namespace is found in its URI, the URI of the same property
	 * otherwise.
	 * 
	 * @param property
	 * @return
	 */
	private String getPropertyQName(IRI property) {
		RepositoryResult<Namespace> namespaces = getManagedConnection().getNamespaces();
		while (namespaces.hasNext()) {
			Namespace ns = namespaces.next();
			if (ns.getName().equals(property.getNamespace())) {
				return ns.getPrefix() + ":" + property.getLocalName();
			}
		}
		return property.stringValue();
	}

	private JsonNode createCellJsonNode(Cell c) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode cellNode = jsonFactory.objectNode();
		cellNode.set("entity1", jsonFactory.textNode(c.getEntity1().stringValue()));
		cellNode.set("entity2", jsonFactory.textNode(c.getEntity2().stringValue()));
		cellNode.set("measure", jsonFactory.numberNode(c.getMeasure()));
		cellNode.set("relation", jsonFactory.textNode(c.getRelation()));
		if (c.getMappingProperty() != null) {
			ObjectNode mapPropNode = jsonFactory.objectNode();
			mapPropNode.set("@id", jsonFactory.textNode(c.getMappingProperty().stringValue()));
			mapPropNode.set("show", jsonFactory.textNode(getPropertyQName(c.getMappingProperty())));
			cellNode.set("mappingProperty", mapPropNode);
		}
		if (c.getStatus() != null) {
			cellNode.set("status", jsonFactory.textNode(c.getStatus().name()));
		}
		if (c.getComment() != null) {
			cellNode.set("comment", jsonFactory.textNode(c.getComment()));
		}
		return cellNode;
	}

	/**
	 * Remove the saved alignment from the session
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	public void closeSession() {
		if (stServiceContext.hasContextParameter("token")) {
			String token = stServiceContext.getSessionToken();
			AlignmentModel align = modelsMap.get(token);
			if (align != null) {
				align.close();
			}
			modelsMap.remove(token);
		}
	}
}
