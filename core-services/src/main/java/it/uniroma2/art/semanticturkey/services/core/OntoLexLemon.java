package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toSet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.lime.model.vocabulary.VARTRANS;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.constraints.SubPropertyOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.NonWorkingGraphUpdateException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.aspects.ResourceLevelChangeMetadataSupport;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexiconRenderer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

/**
 * This class provides services for manipulating data based on the W3C OntoLex-Lemon Model.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class OntoLexLemon extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(OntoLexLemon.class);
	public static final IRI LEXINFO = SimpleValueFactory.getInstance()
			.createIRI("http://www.lexinfo.net/ontology/3.0/lexinfo");
	public static final IRI WN = SimpleValueFactory.getInstance()
			.createIRI("https://globalwordnet.github.io/schemas/wn");
	public static final IRI MORPHOSYNTACTIC_PROPERTY = SimpleValueFactory.getInstance()
			.createIRI("http://www.lexinfo.net/ontology/3.0/lexinfo#morphosyntacticProperty");
	public static final IRI SKOS_DEFINITION_PROPERTY = SKOS.DEFINITION;
	public static final IRI WN_DEFINITION = SimpleValueFactory.getInstance()
			.createIRI("https://globalwordnet.github.io/schemas/wn#definition");
	static final IRI WN_PART_OF_SPEECH = SimpleValueFactory.getInstance()
			.createIRI("https://globalwordnet.github.io/schemas/wn#partOfSpeech");

	/* --- Lexicons --- */

	/**
	 * Creates a new lime:Lexicon for the provided language.
	 * 
	 * @param newLexicon
	 * @param language
	 * @param title
	 * @param customFormValue
	 * @return
	 * @throws CustomFormException
	 * @throws CODAException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'C')")
	public AnnotatedValue<IRI> createLexicon(@Optional @NotLocallyDefined IRI newLexicon, String language,
			@Optional @LanguageTaggedString Literal title, @Optional CustomFormValue customFormValue)
			throws CODAException, CustomFormException, URIGenerationException {

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newLexiconIRI;

		if (newLexicon == null) {
			newLexiconIRI = generateLexiconURI(title);
		} else {
			newLexiconIRI = newLexicon;
		}

		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(newLexiconIRI,
				RDFResourceRole.limeLexicon); // set created for versioning

		modelAdditions.add(newLexiconIRI, RDF.TYPE, LIME.LEXICON);
		modelAdditions.add(newLexiconIRI, DCTERMS.TITLE, title);
		modelAdditions.add(newLexiconIRI, LIME.LANGUAGE,
				SimpleValueFactory.getInstance().createLiteral(language, XMLSchema.LANGUAGE));

		RepositoryConnection repoConnection = getManagedConnection();

		// enrich with custom form
		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newLexiconIRI.stringValue());
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
			enrichWithCustomForm(getManagedConnection(), modelAdditions, modelRemovals, cForm,
					customFormValue.getUserPromptMap(), stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());

		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<>(newLexiconIRI);
		annotatedValue.setAttribute("role", RDFResourceRole.limeLexicon.name());
		annotatedValue.setAttribute("explicit", true);
		return annotatedValue;
	}

	/**
	 * Returns lexicons.
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'R')")
	public Collection<AnnotatedValue<Resource>> getLexicons() {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
				" PREFIX lime: <http://www.w3.org/ns/lemon/lime#>						    \n" +
				"                                                                           \n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {			\n" +
				"     ?resource rdf:type lime:Lexicon .                                     \n" +
				generateNatureSPARQLWherePart("?resource") +
				" }                                                                         \n" +
				" GROUP BY ?resource                                                        \n"
				// @formatter:on
		);
		qb.processQName();
		qb.processRendering();
		return qb.runQuery();
	}

	/**
	 * Deletes a lexicon.
	 * 
	 * @param lexicon
	 * @param updateModificationTimestamps
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'D')")
	public void deleteLexicon(@LocallyDefined IRI lexicon,
			@Optional(defaultValue = "false") boolean updateModificationTimestamps) {
		// note: the following update is a copy with minor modification of the one for the deletion of a
		// lexical entry

		RepositoryConnection conn = getManagedConnection();

		String deleteLexiconUnitString =
		// @formatter:off
			"PREFIX owl: <http://www.w3.org/2002/07/owl#>                                                \n" +
			"PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>                                     \n" +
			"PREFIX synsem: <http://www.w3.org/ns/lemon/synsem#>                                         \n" +
			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                         \n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                        \n" +
			"PREFIX lexinfo: <http://www.lexinfo.net/ontology/2.0/lexinfo#>                              \n" +
			"PREFIX lemon: <http://lemon-model.net/lemon#>                                               \n" +
			"PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                                       \n" +
			"PREFIX lime: <http://www.w3.org/ns/lemon/lime#>                                             \n" +
			"PREFIX decomp: <http://www.w3.org/ns/lemon/decomp#>                                         \n" +
			(updateModificationTimestamps ?
			"CONSTRUCT {                                                                                 \n"
			:
			"DELETE { GRAPH ?workingGraph { \n"
			) +
			"	?lexicon ?pl1 ?ol1 .                                                                     \n" +
			"	?sl2 ?pl2 ?lexicon .                                                                     \n" +
			"	?lexicalEntry ?p1 ?o1 .                                                                  \n" +
			"	?lexicalRelation ?p14 ?o14 .                                                             \n" +
			"	?s15 ?p15 ?lexicalRelation .                                                             \n" +
			"	?s2 ?p2 ?lexicalEntry .                                                                  \n" +
			"	?lexicalRelation ?p14 ?o14 .                                                             \n" +
			"	?s15 ?p15 ?lexicalRelation .                                                             \n" +
			"	?s1 ?p1 ?lexicalEntry .                                                                  \n" +
			"	?lexicalForm ?p16 ?o16 .                                                                 \n" +
			"	?s17 ?p17 ?lexicalForm .                                                                 \n" +
			"	?lexicalSense ?p3 ?o3 .                                                                  \n" +
			"	?s4 ?p4 ?lexicalSense .                                                                  \n" +
			"	?ontoMap ?p7 ?o7 .                                                                       \n" +
			"	?s8 ?p8 ?ontoMap .                                                                       \n" +
			"	?senseRelation ?p12 ?o12 .                                                               \n" +
			"	?s13 ?p13 ?senseRelation .                                                               \n" +
			"	?synFrame ?p5 ?o5 .                                                                      \n" +
			"	?s6 ?p6 ?synFrame .                                                                      \n" +
			"	?synArg ?p9 ?o9 .                                                                        \n" +
			"	?component ?p10 ?o10 .                                                                   \n" +
			"	?s11 ?p11 ?component .                                                                   \n" +
			(updateModificationTimestamps ?
			"}                                                                                           \n"
			:
			" } }"
			) +
			"WHERE {                                                                                     \n" +
			"   VALUES(?lexicon ?workingGraph) {                                                         \n" +
			"	   (" + RenderUtils.toSPARQL(lexicon) + " " + RenderUtils.toSPARQL(getWorkingGraph()) + ")\n" +
			"   }                                                                                        \n" +
			"	{                                                                                        \n" +
			"	  GRAPH ?workingGraph {                                                                  \n" +
			"		?lexicon ?pl1 ?ol1 .                                                                 \n" +
			"	  }                                                                                      \n" +
			"	} UNION {                                                                                \n" +
			"	  GRAPH ?workingGraph {                                                                  \n" +
			"		?sl2 ?pl2 ?lexicon                                                                   \n" +
			"	  }                                                                                      \n" +
			"	} UNION {                                                                                \n" +
			"	  ?entryProp rdfs:subPropertyOf* lime:entry .                                            \n" +
			"	  ?lexicon ?entryProp ?lexicalEntry .                                                    \n" +
			"	                                                                                         \n" +
			"	  {                                                                                      \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				?lexicalEntry ?p1 ?o1                                                        \n" +
			"			}                                                                                \n" +
			"	  } UNION {                                                                              \n" +
			"		?relatesProp rdfs:subPropertyOf* vartrans:relates .                                  \n" +
			"		?lexicalRelation ?relatesProp ?lexicalEntry .                                        \n" +
			"		GRAPH ?workingGraph	{                                                                \n" +
			"			{                                                                                \n" +
			"				?lexicalRelation ?p14 ?o14                                                   \n" +
			"			} UNION {                                                                        \n" +
			"				?s15 ?p15 ?lexicalRelation                                                   \n" +
			"			}                                                                                \n" +
			"		}                                                                                    \n" +
			"	  } UNION {                                                                              \n" +
			"		GRAPH ?workingGraph {                                                                \n" +
			"			?s2 ?p2 ?lexicalEntry                                                            \n" +
			"		}                                                                                    \n" +
			"	  } UNION {                                                                              \n" +
			"		?lexicalFormProp rdfs:subPropertyOf* ontolex:lexicalForm .                           \n" +
			"		?lexicalEntry ?lexicalFormProp ?lexicalForm .                                        \n" +
			"		GRAPH ?workingGraph {                                                                \n" +
			"			{                                                                                \n" +
			"				?lexicalForm ?p16 ?o16                                                       \n" +
			"			} UNION {                                                                        \n" +
			"				?s17 ?p17 ?lexicalForm                                                       \n" +
			"			}                                                                                \n" +
			"		}                                                                                    \n" +
			"	  } UNION {                                                                              \n" +
			"		{                                                                                    \n" +
			"		  ?senseProp rdfs:subPropertyOf* ontolex:sense .                                     \n" +
			"		  ?lexicalEntry ?senseProp ?lexicalSense .                                           \n" +
			"		} UNION {                                                                            \n" +
			"		  ?isSenseOfProp rdfs:subPropertyOf* ontolex:isSenseOf .                             \n" +
			"		  ?lexicalSense ?isSenseOfProp ?lexicalEntry                                         \n" +
			"		}                                                                                    \n" +
			"		{                                                                                    \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				?lexicalSense ?p3 ?o3                                                        \n" +
			"			}                                                                                \n" +
			"		} UNION {                                                                            \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				?s4 ?p4 ?lexicalSense .                                                      \n" +
			"			}                                                                                \n" +
			"		} UNION {                                                                            \n" +
			"			?lexicalSense ^synsem:ontoMapping/synsem:submap* ?ontoMap .                      \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				{                                                                            \n" +
			"					?ontoMap ?p7 ?o7                                                         \n" +
			"				} UNION {                                                                    \n" +
			"					?s8 ?p8 ?ontoMap                                                         \n" +
			"				}                                                                            \n" +
			"			}                                                                                \n" +
			"		} UNION {                                                                            \n" +
			"			?relatesProp rdfs:subPropertyOf* vartrans:relates .                              \n" +
			"			?senseRelation ?relatesProp ?lexicalSense .                                      \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				{                                                                            \n" +
			"					?senseRelation ?p12 ?o12                                                 \n" +
			"				} UNION {                                                                    \n" +
			"					?s13 ?p13 ?senseRelation                                                 \n" +
			"				}                                                                            \n" +
			"			}                                                                                \n" +
			"		}                                                                                    \n" +
			"	  } UNION {                                                                              \n" +
			"		?synFrameProp rdfs:subPropertyOf* synsem:synBehavior .                               \n" +
			"		?lexicalEntry ?synFrameProp ?synFrame .                                              \n" +
			"		{                                                                                    \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				?synFrame ?p5 ?o5                                                            \n" +
			"			}                                                                                \n" +
			"		} UNION {                                                                            \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				?s6 ?p6 ?synFrame                                                            \n" +
			"			}                                                                                \n" +
			"		} UNION {                                                                            \n" +
			"			{                                                                                \n" +
			"				?synArgProp rdfs:subPropertyOf* synsem:synArg                                \n" +
			"			} UNION {                                                                        \n" +
			"				?synArgProp rdfs:subPropertyOf* lemon:synArg                                 \n" +
			"			}                                                                                \n" +
			"			?synFrame ?synArgProp ?synArg .                                                  \n" +
			"			GRAPH ?workingGraph {                                                            \n" +
			"				?synArg ?p9 ?o9 .                                                            \n" +
			"			}                                                                                \n" +
			"		  }                                                                                  \n" +
			"	  } UNION {                                                                              \n" +
			"		{                                                                                    \n" +
			"			?lexicalEntry decomp:constituent+ ?component .                                   \n" +
			"		} UNION {                                                                            \n" +
			"			?component decomp:correspondsTo ?lexicalEntry .                                  \n" +
			"		}                                                                                    \n" +
			"		FILTER NOT EXISTS {                                                                  \n" +
			"			?decompProp2 rdfs:subPropertyOf* decomp:constituent .                            \n" +
			"			?decompProp3 rdfs:subPropertyOf* decomp:constituent .                            \n" +
			"			?parentComp1 ?decompProp2 ?component .                                           \n" +
			"			?parentComp2 ?decompProp2 ?component .                                           \n" +
			"			FILTER(!sameTerm(?parentComp1, ?parentComp2))                                    \n" +
			"		}                                                                                    \n" +
			"		GRAPH ?workingGraph {                                                                \n" +
			"			{                                                                                \n" +
			"			  ?component ?p10 ?o10                                                           \n" +
			"			} UNION {                                                                        \n" +
			"			  ?s11 ?p11 ?component .                                                         \n" +
			"			}                                                                                \n" +
			"		}                                                                                    \n" +
			"	  }                                                                                      \n" +
			"	}                                                                                        \n" +
			"}                                                                                           \n";
		// @formatter:on

		logger.debug("delete lexicon SPARQL unit:\n{}", deleteLexiconUnitString);

		if (updateModificationTimestamps) { // update of timestamps -> first get the triples and scan them

			GraphQuery deleteLexiconQuery = conn.prepareGraphQuery(deleteLexiconUnitString);

			List<Statement> triples2Delete = QueryResults.asList(deleteLexiconQuery.evaluate());

			// remove the triples
			conn.remove(triples2Delete, getWorkingGraph());

			// scan the triples for the subjects of properties that cause the update of the modification date

			Set<IRI> props = QueryResults.stream(conn.prepareTupleQuery(
			// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                    \n" +
				" PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                   \n" +
				" PREFIX lime: <http://www.w3.org/ns/lemon/lime#>                         \n" +
				" SELECT DISTINCT ?prop {                                                 \n" +
				" 	VALUES(?baseProp) {                                                   \n" +
				" 		(ontolex:isDenotedBy)(ontolex:isEvokedBy)                         \n" +
				" 		(ontolex:isReferenceOf)(ontolex:lexicalizedSense)                 \n" +
				" 	}                                                                     \n" +
				" 	?prop rdfs:subPropertyOf* ?baseProp .                                 \n" +
				"   FILTER(isIRI(?prop))                                                  \n" +
				" }                                                                       \n"    
				// @formatter:on
			).evaluate()).map(bs -> (IRI) bs.getValue("prop")).collect(toSet());

			logger.debug("Properties that should cause the update of the modification date: {}", props);

			triples2Delete.stream().filter(st -> props.contains(st.getPredicate())).map(Statement::getSubject)
					.forEach(res -> {
						if (!res.equals(lexicon)) {
							ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
									.addModifiedResource(res);
						}
					});
		} else { // no update of timestamps -> perform a delete update
			conn.prepareUpdate(deleteLexiconUnitString).execute();
		}

	}

	/**
	 * Returns the language of a lexicon
	 * 
	 * @param lexicon
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'R')")
	public String getLexiconLanguage(@LocallyDefined IRI lexicon) {
		String language = null;
		RepositoryConnection repoConn = getManagedConnection();
		String query = "SELECT ?lexiconLanguage WHERE {\n" + NTriplesUtil.toNTriplesString(lexicon) + " "
				+ NTriplesUtil.toNTriplesString(LIME.LANGUAGE) + " ?lexiconLanguage .\n" + "} LIMIT 1";
		List<BindingSet> result = QueryResults.asList(repoConn.prepareTupleQuery(query).evaluate());

		if (!result.isEmpty()) {
			language = result.get(0).getValue("lexiconLanguage").stringValue();
		}
		return language;
	}

	/* --- Definitions --- */

	/**
	 * Allows the addition of a definition
	 * 
	 * @param resource
	 * @param value
	 * @param lexicon
	 * @throws CODAException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', definitions)','C')")
	public void addDefinition(@LocallyDefined @Modified Resource resource, Literal value,
			@Optional @LocallyDefined Resource lexicon) throws CODAException, URIGenerationException {
		RepositoryConnection con = getManagedConnection();
		IRI pred = getDefinitionPredicate(lexicon, con);
		addDefininitionInternal(resource, value, con, pred);
	}

	protected static IRI getDefinitionPredicate(Resource lexicon, RepositoryConnection con)
			throws QueryEvaluationException, RepositoryException {
		IRI pred;
		Set<IRI> linguisticCatalogs = OntoLexLemon.getLinguisticCatalogs(con, lexicon);
		if (linguisticCatalogs.contains(WN)) {
			pred = WN_DEFINITION;
		} else {
			pred = SKOS.DEFINITION;
		}
		return pred;
	}

	/**
	 * Allows the deletion of a definition
	 * 
	 * @param resource
	 * @param value
	 * @param lexicon
	 * @throws PRParserException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', definitions)','D')")
	public void removeDefinition(@LocallyDefined @Modified Resource resource, Value value,
			@Optional @LocallyDefined Resource lexicon) throws PRParserException, URIGenerationException {
		RepositoryConnection con = getManagedConnection();
		IRI pred = getDefinitionPredicate(lexicon, con);

		if (value instanceof Literal) {
			con.remove(resource, pred, value, getWorkingGraph());
			addDefininitionInternal(resource, value, con, pred);
		} else { // reified definition
			con.remove(resource, pred, value, getWorkingGraph());
			con.remove((Resource) value, null, null, getWorkingGraph());
		}
	}

	protected void addDefininitionInternal(Resource resource, Value value, RepositoryConnection con, IRI pred)
			throws URIGenerationException, RepositoryException, MalformedQueryException,
			UpdateExecutionException {
		IRI definitionIRI = generateIRI("xNote", Collections.emptyMap());

		Update update = con.prepareUpdate(
				"INSERT { ?resource ?pred ?def . ?def <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?value . } WHERE {}");
		update.setBinding("resource", resource);
		update.setBinding("pred", pred);
		update.setBinding("def", definitionIRI);
		update.setBinding("value", value);

		SimpleDataset dataset = new SimpleDataset();
		dataset.setDefaultInsertGraph((IRI) getWorkingGraph());
		update.setDataset(dataset);

		update.execute();
	}

	/**
	 * Allows the update of definitions
	 * 
	 * @param resource
	 * @param value
	 * @param newValue
	 * @param lexicon
	 * @throws PRParserException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resource)+ ', definitions)','U')")
	public void updateDefinition(@LocallyDefined @Modified Resource resource, Value value, Literal newValue,
			@Optional @LocallyDefined Resource lexicon) {
		RepositoryConnection con = getManagedConnection();
		IRI pred = getDefinitionPredicate(lexicon, con);

		if (value instanceof Literal) {
			con.remove(resource, pred, value, getWorkingGraph());
			con.add(resource, pred, newValue, getWorkingGraph());
		} else {
			Resource defRes = (Resource) value;
			con.remove(defRes, RDF.VALUE, null, getWorkingGraph());
			con.add(defRes, RDF.VALUE, newValue, getWorkingGraph());

		}
	}

	/* --- Lexical entries --- */

	/**
	 * Creates a new ontolex:LexicalEntry.
	 * 
	 * @param newLexicalEntry
	 * @param lexicalEntryCls
	 * @param canonicalForm
	 * @param lexicon
	 * @param customFormValue
	 * @return
	 * @throws CODAException
	 * @throws CustomFormException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'C')")
	public AnnotatedValue<IRI> createLexicalEntry(@Optional @NotLocallyDefined IRI newLexicalEntry,
			@Optional(defaultValue = "<http://www.w3.org/ns/lemon/ontolex#LexicalEntry>") @SubClassOf(superClassIRI = "http://www.w3.org/ns/lemon/ontolex#LexicalEntry") IRI lexicalEntryCls,
			@LanguageTaggedString Literal canonicalForm, @LocallyDefined @Modified IRI lexicon,
			@Optional CustomFormValue customFormValue)
			throws CODAException, CustomFormException, URIGenerationException {

		RepositoryConnection repConn = getManagedConnection();

		String lexiconLanguage = getLexiconLanguageInternal(repConn, lexicon).orElseThrow(
				() -> new IllegalArgumentException("The provided lexicon does not declare any language"));

		String formLanguage = canonicalForm.getLanguage()
				.orElseThrow(() -> new IllegalArgumentException("Missing language tag in canonical form"));

		if (!langMatches(formLanguage, lexiconLanguage)) {
			throw new IllegalArgumentException("The canonical form is expressed in a natural language ("
					+ formLanguage + ") not compatible with the lexicon (" + lexiconLanguage + ")");
		}

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newLexicalEntryIRI;
		if (newLexicalEntry == null) {
			newLexicalEntryIRI = generateLexicalEntryURI(canonicalForm, lexicon);
		} else {
			newLexicalEntryIRI = newLexicalEntry;
		}

		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(newLexicalEntryIRI,
				RDFResourceRole.ontolexLexicalEntry); // set created for versioning

		IRI canonicalFormIRI = generateFormIRI(newLexicalEntryIRI, canonicalForm, ONTOLEX.CANONICAL_FORM);

		modelAdditions.add(newLexicalEntryIRI, RDF.TYPE, lexicalEntryCls);
		modelAdditions.add(newLexicalEntryIRI, ONTOLEX.CANONICAL_FORM, canonicalFormIRI);
		modelAdditions.add(newLexicalEntryIRI, LIME.LANGUAGE,
				SimpleValueFactory.getInstance().createLiteral(lexiconLanguage, XMLSchema.LANGUAGE));
		modelAdditions.add(lexicon, LIME.ENTRY, newLexicalEntryIRI);

		modelAdditions.add(canonicalFormIRI, RDF.TYPE, ONTOLEX.FORM);
		modelAdditions.add(canonicalFormIRI, ONTOLEX.WRITTEN_REP, canonicalForm);

		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(canonicalFormIRI,
				RDFResourceRole.ontolexForm); // set created for versioning

		// enrich with custom form
		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newLexicalEntryIRI.stringValue());
			stdForm.addFormEntry(StandardForm.Prompt.lexicon, lexicon.stringValue());

			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
			enrichWithCustomForm(getManagedConnection(), modelAdditions, modelRemovals, cForm,
					customFormValue.getUserPromptMap(), stdForm);
		}

		repConn.add(modelAdditions, getWorkingGraph());
		repConn.remove(modelRemovals, getWorkingGraph());

		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<>(newLexicalEntryIRI);
		annotatedValue.setAttribute("role", RDFResourceRole.ontolexLexicalEntry.name());
		annotatedValue.setAttribute("explicit", true);
		return annotatedValue;
	}

	/**
	 * Returns the language declared by the provided lexical entry, or as fallback the one declared by the
	 * lexicon
	 * 
	 * @param lexicalEntry
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
	public String getLexicalEntryLanguage(@LocallyDefined IRI lexicalEntry) {
		return OntoLexLemon.getLexicalEntryLanguageInternal(getManagedConnection(), lexicalEntry)
				.orElse(null);
	}

	/**
	 * Checks whether the given language is compatible to the one specified in the lexicon
	 * 
	 * @param conn
	 * @param language
	 * @param lexicon
	 */
	public static boolean isLanguageComaptibleWithLexiconMetadata(RepositoryConnection conn, String language,
			IRI lexicon) {
		BooleanQuery query = conn.prepareBooleanQuery(
		// @formatter:off
				" ASK { " +
				"   ?lexicon " + NTriplesUtil.toNTriplesString(LIME.LANGUAGE) + " ?lexiconLanguage . \n" +
				"   FILTER(LANGMATCHES(?language,STR(?lexiconLanguage))) \n" +
				" } "
			// @formatter:on
		);

		query.setBinding("language", conn.getValueFactory().createLiteral(language));
		query.setBinding("lexicon", lexicon);
		query.setIncludeInferred(false);

		return query.evaluate();
	}

	/**
	 * Checks whether the provided language is compatible with the given language range
	 * 
	 * @param language
	 * @param languageRange
	 * @return
	 */
	public static boolean langMatches(String language, String languageRange) {
		if ("*".equals(languageRange)) {
			return true;
		} else if (Objects.equal(language, languageRange)) {
			return true;
		} else if (languageRange.length() >= language.length()) {
			return false;
		} else {
			return language.startsWith(languageRange) && language.charAt(languageRange.length()) == '-';
		}
	}

	/**
	 * Returns the language declared by the provided lexicon
	 * 
	 * @param conn
	 * @param lexicon
	 * @return
	 */
	public static java.util.Optional<String> getLexiconLanguageInternal(RepositoryConnection conn,
			IRI lexicon) {
		return Models.objectString(QueryResults.asModel(conn.getStatements(lexicon, LIME.LANGUAGE, null)));
	}

	/**
	 * Returns the language declared by the provided lexical entry, or as fallback the one declared by the
	 * lexicon
	 * 
	 * @param conn
	 * @param lexicalEntry
	 * @return
	 */
	public static java.util.Optional<String> getLexicalEntryLanguageInternal(RepositoryConnection conn,
			IRI lexicalEntry) {
		TupleQuery query = conn.prepareTupleQuery(
		// @formatter:off
			"PREFIX lime: <http://www.w3.org/ns/lemon/lime#>\n" +
			"SELECT ?lang {\n" +
			"  ?lexicalEntry lime:language|^lime:entry/lime:language ?lang . \n" +
			"}\n" +
			"LIMIT 1\n"
			// @formatter:on
		);
		query.setIncludeInferred(false);
		query.setBinding("lexicalEntry", lexicalEntry);
		return QueryResults.asSet(query.evaluate()).stream().map(bs -> bs.getValue("lang").stringValue())
				.findFirst();
	}

	/**
	 * Returns the language declared by the provided lexical entry, or as fallback the one declared by the
	 * lexicon
	 * 
	 * @param conn
	 * @param form
	 * @return
	 */
	public static java.util.Optional<String> getFormLanguageInternal(RepositoryConnection conn,
			Resource form) {
		TupleQuery query = conn.prepareTupleQuery(
		// @formatter:off
			"PREFIX lime: <http://www.w3.org/ns/lemon/lime#>\n" +
			"PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"SELECT ?lang {\n" +
			"  ?lexicalEntry ?p ?form . \n" +
			"  FILTER EXISTS { ?p rdfs:subPropertyOf* ontolex:lexicalForm } \n" +
			"  ?lexicalEntry lime:language|^lime:entry/lime:language ?lang . \n" +
			"}\n" +
			"LIMIT 1\n"
			// @formatter:on
		);
		query.setIncludeInferred(false);
		query.setBinding("form", form);
		return QueryResults.asSet(query.evaluate()).stream().map(bs -> bs.getValue("lang").stringValue())
				.findFirst();
	}

	/**
	 * Returns the entries in a given lexicon that starts with the supplied index consisting of two
	 * characters.
	 * 
	 * @param index
	 * @param lexicon
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
	public Collection<AnnotatedValue<Resource>> getLexicalEntriesByAlphabeticIndex(
			@Length(min = 1) String index, @Optional IRI lexicon) {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					        \n" +
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						        \n" +
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>								        \n" +                                      
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						        \n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						        \n" +
			" prefix ontolex: <http://www.w3.org/ns/lemon/ontolex#>                             \n" +
			" prefix lime: <http://www.w3.org/ns/lemon/lime#>                                   \n" +
            "                                                                                   \n" +
			" SELECT ?resource " + generateNatureSPARQLSelectPart() +" WHERE {                  \n" +
			(lexicon != null ? "   ?lexicon lime:entry ?resource . \n" : "") +
			"   ?resource ontolex:canonicalForm [                                               \n" +
			"     ontolex:writtenRep ?cf                                                        \n" +
			"   ]                                                                               \n" +
			"   .                                                                               \n" +
			instantiateSearchStrategy().searchSpecificModePrepareQuery("?cf", index+"", SearchMode.startsWith, 
					null, null, false, false) +
			generateNatureSPARQLWherePart("?resource") +
			" }                                                                                 \n" +
			" GROUP BY ?resource                                                                \n"
			// @formatter:on
		);
		if (lexicon != null) {
			qb.setBinding("lexicon", lexicon);
		}
		qb.processQName();
		qb.process(LexicalEntryRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
	public Integer countLexicalEntriesByAlphabeticIndex(@Length(min = 1) String index,
			@Optional IRI lexicon) {
		String query =
		// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					        \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						        \n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								        \n" +
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						        \n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						        \n" +
				" prefix ontolex: <http://www.w3.org/ns/lemon/ontolex#>                             \n" +
				" prefix lime: <http://www.w3.org/ns/lemon/lime#>                                   \n" +
				"                                                                                   \n" +
				" SELECT (count(distinct ?resource) as ?count) WHERE {			                    \n" +
				(lexicon != null ? "   ?lexicon lime:entry ?resource . \n" : "") +
				"   ?resource ontolex:canonicalForm [                                               \n" +
				"     ontolex:writtenRep ?cf                                                        \n" +
				"   ]                                                                               \n" +
				"   .                                                                               \n" +
				instantiateSearchStrategy().searchSpecificModePrepareQuery("?cf", index+"", SearchMode.startsWith,
						null, null, false, false) +
				" }";
				// @formatter:on
		if (lexicon != null) {
			query = query.replace("?lexicon", NTriplesUtil.toNTriplesString(lexicon));
		}
		TupleQueryResult result = getManagedConnection().prepareTupleQuery(query).evaluate();
		return ((Literal) result.next().getValue("count")).intValue();
	}

	/**
	 * Deletes a lexical entry.
	 * 
	 * @param lexicalEntry
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'D')")
	public void deleteLexicalEntry(@LocallyDefined IRI lexicalEntry) {
		RepositoryConnection conn = getManagedConnection();

		String triples2deleteQueryString =
		// @formatter:off
			"PREFIX owl: <http://www.w3.org/2002/07/owl#>                                    \n" + 
			"PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>                         \n" + 
			"PREFIX synsem: <http://www.w3.org/ns/lemon/synsem#>                             \n" + 
			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                             \n" + 
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                            \n" + 
			"PREFIX lexinfo: <http://www.lexinfo.net/ontology/2.0/lexinfo#>                  \n" + 
			"PREFIX lemon: <http://lemon-model.net/lemon#>                                   \n" + 
			"PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                           \n" + 
			"PREFIX lime: <http://www.w3.org/ns/lemon/lime#>                                 \n" + 
			"PREFIX decomp: <http://www.w3.org/ns/lemon/decomp#>                             \n" + 
            "                                                                                \n" + 
			"CONSTRUCT {                                                                     \n" + 
			"	?lexicalEntry ?p1 ?o1 .                                                      \n" + 
			"	?lexicalRelation ?p14 ?o14 .                                                 \n" + 
			"	?s15 ?p15 ?lexicalRelation .                                                 \n" + 
			"	?s2 ?p2 ?lexicalEntry .                                                      \n" + 
			"	?lexicalRelation ?p14 ?o14 .                                                 \n" + 
			"	?s15 ?p15 ?lexicalRelation .                                                 \n" + 
			"	?s1 ?p1 ?lexicalEntry .                                                      \n" + 
			"	?lexicalForm ?p16 ?o16 .                                                     \n" + 
			"	?s17 ?p17 ?lexicalForm .                                                     \n" + 
			"	?lexicalSense ?p3 ?o3 .                                                      \n" + 
			"	?s4 ?p4 ?lexicalSense .                                                      \n" + 
			"	?ontoMap ?p7 ?o7 .                                                           \n" + 
			"	?s8 ?p8 ?ontoMap .                                                           \n" + 
			"	?senseRelation ?p12 ?o12 .                                                   \n" + 
			"	?s13 ?p13 ?senseRelation .                                                   \n" + 
			"	?synFrame ?p5 ?o5 .                                                          \n" + 
			"	?s6 ?p6 ?synFrame .                                                          \n" + 
			"	?synArg ?p9 ?o9 .                                                            \n" + 
			"	?component ?p10 ?o10 .                                                       \n" + 
			"	?s11 ?p11 ?component .                                                       \n" + 
			"}                                                                               \n" + 
			"WHERE {                                                                         \n" + 
			"	{                                                                            \n" + 
			"		GRAPH ?workingGraph {                                                    \n" + 
			"			?lexicalEntry ?p1 ?o1                                                \n" + 
			"		}                                                                        \n" + 
			"  } UNION {                                                                     \n" + 
			"    ?relatesProp rdfs:subPropertyOf* vartrans:relates .                         \n" + 
			"    ?lexicalRelation ?relatesProp ?lexicalEntry .                               \n" + 
			"	GRAPH ?workingGraph	{                                                        \n" + 
			"		{                                                                        \n" + 
			"			?lexicalRelation ?p14 ?o14                                           \n" + 
			"		} UNION {                                                                \n" + 
			"			?s15 ?p15 ?lexicalRelation                                           \n" + 
			"		}                                                                        \n" + 
			"	}                                                                            \n" + 
			"  } UNION {                                                                     \n" + 
			"	GRAPH ?workingGraph {                                                        \n" + 
			"		?s2 ?p2 ?lexicalEntry                                                    \n" + 
			"	}                                                                            \n" + 
			"  } UNION {                                                                     \n" + 
			"    ?lexicalFormProp rdfs:subPropertyOf* ontolex:lexicalForm .                  \n" + 
			"    ?lexicalEntry ?lexicalFormProp ?lexicalForm .                               \n" + 
			"	GRAPH ?workingGraph {                                                        \n" + 
			"		{                                                                        \n" + 
			"			?lexicalForm ?p16 ?o16                                               \n" + 
			"		} UNION {                                                                \n" + 
			"			?s17 ?p17 ?lexicalForm                                               \n" + 
			"		}                                                                        \n" + 
			"	}                                                                            \n" +
			"  } UNION {                                                                     \n" + 
			"    {                                                                           \n" + 
			"      ?senseProp rdfs:subPropertyOf* ontolex:sense .                            \n" + 
			"      ?lexicalEntry ?senseProp ?lexicalSense .                                  \n" + 
			"    } UNION {                                                                   \n" + 
			"      ?isSenseOfProp rdfs:subPropertyOf* ontolex:isSenseOf .                    \n" + 
			"      ?lexicalSense ?isSenseOfProp ?lexicalEntry                                \n" + 
			"    }                                                                           \n" + 
			"    {                                                                           \n" + 
			"		GRAPH ?workingGraph {                                                    \n" + 
			"			?lexicalSense ?p3 ?o3                                                \n" + 
			"		}                                                                        \n" + 
			"	} UNION {                                                                    \n" + 
			"		GRAPH ?workingGraph {                                                    \n" + 
			"			?s4 ?p4 ?lexicalSense .                                              \n" + 
			"		}                                                                        \n" + 
			"	} UNION {                                                                    \n" + 
			"		?lexicalSense ^synsem:ontoMapping/synsem:submap* ?ontoMap .              \n" + 
			"        GRAPH ?workingGraph {                                                   \n" + 
			"			{                                                                    \n" + 
			"				?ontoMap ?p7 ?o7                                                 \n" + 
			"			} UNION {                                                            \n" + 
			"				?s8 ?p8 ?ontoMap                                                 \n" + 
			"			}                                                                    \n" + 
			"		}                                                                        \n" + 
			"    } UNION {                                                                   \n" + 
			"		?relatesProp rdfs:subPropertyOf* vartrans:relates .                      \n" + 
			"		?senseRelation ?relatesProp ?lexicalSense .                              \n" + 
			"		GRAPH ?workingGraph {                                                    \n" + 
			"			{                                                                    \n" + 
			"				?senseRelation ?p12 ?o12                                         \n" + 
			"			} UNION {                                                            \n" + 
			"				?s13 ?p13 ?senseRelation                                         \n" + 
			"			}                                                                    \n" + 
			"		}                                                                        \n" + 
			"    }                                                                           \n" + 
			"  } UNION {                                                                     \n" + 
			"    ?synFrameProp rdfs:subPropertyOf* synsem:synBehavior .                      \n" + 
			"    ?lexicalEntry ?synFrameProp ?synFrame .                                     \n" + 
			"    {                                                                           \n" + 
			"		GRAPH ?workingGraph {                                                    \n" + 
			"			?synFrame ?p5 ?o5                                                    \n" + 
			"		}                                                                        \n" + 
			"	} UNION {                                                                    \n" + 
			"		GRAPH ?workingGraph {                                                    \n" + 
			"			?s6 ?p6 ?synFrame                                                    \n" + 
			"		}                                                                        \n" + 
			"	} UNION {                                                                    \n" + 
			"        {                                                                       \n" + 
			"			?synArgProp rdfs:subPropertyOf* synsem:synArg                        \n" + 
			"		} UNION {                                                                \n" + 
			"			?synArgProp rdfs:subPropertyOf* lemon:synArg                         \n" + 
			"		}                                                                        \n" + 
			"        ?synFrame ?synArgProp ?synArg .                                         \n" + 
			"        GRAPH ?workingGraph {                                                   \n" + 
			"			?synArg ?p9 ?o9 .                                                    \n" + 
			"		}                                                                        \n" + 
			"      }                                                                         \n" + 
			"  } UNION {                                                                     \n" + 
			"	{                                                                            \n" + 
			"		?lexicalEntry decomp:constituent+ ?component .                           \n" + 
			"    } UNION {                                                                   \n" + 
			"		?component decomp:correspondsTo ?lexicalEntry .                          \n" + 
			"    }                                                                           \n" + 
			"    FILTER NOT EXISTS {                                                         \n" + 
			"		?decompProp2 rdfs:subPropertyOf* decomp:constituent .                    \n" + 
			"		?decompProp3 rdfs:subPropertyOf* decomp:constituent .                    \n" + 
			"		?parentComp1 ?decompProp2 ?component .                                   \n" + 
			"		?parentComp2 ?decompProp2 ?component .                                   \n" + 
			"		FILTER(!sameTerm(?parentComp1, ?parentComp2))                            \n" + 
			"    }                                                                           \n" + 
			"	GRAPH ?workingGraph {                                                        \n" + 
			"		{                                                                        \n" + 
			"		  ?component ?p10 ?o10                                                   \n" + 
			"		} UNION {                                                                \n" + 
			"		  ?s11 ?p11 ?component .                                                 \n" + 
			"		}                                                                        \n" + 
			"	}                                                                            \n" + 
			"  }                                                                             \n" + 
			"}	                                                                             \n" +
			"VALUES(?lexicalEntry ?workingGraph) {                                           \n" +
			"	(" + RenderUtils.toSPARQL(lexicalEntry) + " " + RenderUtils.toSPARQL(getWorkingGraph()) + ")\n" +
			"}                                                                               \n" ;
			// @formatter:on

		logger.debug("delete lexical entry query:\n{}", triples2deleteQueryString);

		GraphQuery triples2deleteQuery = conn.prepareGraphQuery(triples2deleteQueryString);

		List<Statement> triples2Delete = QueryResults.asList(triples2deleteQuery.evaluate());

		// remove the triples
		conn.remove(triples2Delete, getWorkingGraph());

		// scan the triples for the subjects of properties that cause the update of the modification date

		Set<IRI> props = QueryResults.stream(conn.prepareTupleQuery(
		// @formatter:off
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                    \n" +
			" PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                   \n" +
			" PREFIX lime: <http://www.w3.org/ns/lemon/lime#>                         \n" +
			" SELECT DISTINCT ?prop {                                                 \n" +
			" 	VALUES(?baseProp) {                                                   \n" +
			" 		(lime:entry)                                                      \n" +
			" 		(ontolex:isDenotedBy)(ontolex:isEvokedBy)                         \n" +
			" 		(ontolex:isReferenceOf)(ontolex:lexicalizedSense)                 \n" +
			" 	}                                                                     \n" +
			" 	?prop rdfs:subPropertyOf* ?baseProp .                                 \n" +
			"   FILTER(isIRI(?prop))                                                  \n" +
			" }                                                                       \n"    
			// @formatter:on
		).evaluate()).map(bs -> (IRI) bs.getValue("prop")).collect(toSet());

		logger.debug("Properties that should cause the update of the modification date: {}", props);

		triples2Delete.stream().filter(st -> props.contains(st.getPredicate())).map(Statement::getSubject)
				.forEach(res -> ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(res));
	}

	/**
	 * Returns the 2-digits index of the given lexicalEntry
	 * 
	 * @param lexicalEntry
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
	public String getLexicalEntryIndex(@LocallyDefined IRI lexicalEntry) {
		//@formatter:off
		String index="";
		
		String indexVar = "?index";
		String varType = "?type";
		String resInNT = NTriplesUtil.toNTriplesString(lexicalEntry);
		
		String query = "SELECT "+indexVar +
				"\nWHERE {" +
				"\n"+resInNT+" a "+varType+" . " +
						//consider the classes that are subclasses of ONTOLEX.LEXICAL_ENTRY
						"\n"+varType+" "+NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF)+"* "
								+NTriplesUtil.toNTriplesString(ONTOLEX.LEXICAL_ENTRY)+" ." +
						//"\nFILTER("+varType+" = <"+ONTOLEX.LEXICAL_ENTRY.stringValue()+">)";
				//add the index to which this lexical entry belong to
				"\n"+resInNT+" <"+ONTOLEX.CANONICAL_FORM.stringValue()+"> ?canonicalForm ."+
						"\n?canonicalForm <"+ONTOLEX.WRITTEN_REP+"> ?writtenRep ." +
						ServiceForSearches.getFirstLetterForLiteral("?writtenRep", indexVar) +
				"\n}";
		logger.debug("query = " + query);
		
		TupleQuery tupleQuery;
		tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		//set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		for(Resource namedGraph : stServiceContext.getRGraphs()){
			if(namedGraph instanceof IRI){
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);
		
		TupleQueryResult tupleBindingsIterator = tupleQuery.evaluate();
		if(tupleBindingsIterator.hasNext()) {
			index = tupleBindingsIterator.next().getValue(indexVar.substring(1)).stringValue();
		}
		
		return index;
		//@formatter:on
	}

	/**
	 * Returns the lexicons which the lexicalEntry belongs to
	 * 
	 * @param lexicalEntry
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
	public Collection<AnnotatedValue<Resource>> getLexicalEntryLexicons(@LocallyDefined IRI lexicalEntry) {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					        \n" +
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						        \n" +
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>								        \n" +                                      
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						        \n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						        \n" +
			" prefix ontolex: <http://www.w3.org/ns/lemon/ontolex#>                             \n" +
			" prefix lime: <http://www.w3.org/ns/lemon/lime#>                                   \n" +
            "                                                                                   \n" +
			" SELECT ?resource " + generateNatureSPARQLSelectPart() +" WHERE {                  \n" +
			"   ?resource lime:entry ?entry .                             	                    \n" +
			generateNatureSPARQLWherePart("?resource") +
			" }                                                                                 \n" +
			" GROUP BY ?resource                                                                \n"
			// @formatter:on
		);
		qb.setBinding("entry", lexicalEntry);
		qb.processQName();
		qb.process(LexiconRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}

	/**
	 * Returns the senses of a lexicalEntry
	 * 
	 * @param lexicalEntry
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
	public Collection<AnnotatedValue<Resource>> getLexicalEntrySenses(@LocallyDefined IRI lexicalEntry) {
		QueryBuilder qb = createQueryBuilder(
		// @formatter:off
			" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					        \n" +
			" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						        \n" +
			" PREFIX owl: <http://www.w3.org/2002/07/owl#>								        \n" +                                      
			" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						        \n" +
			" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						        \n" +
			" prefix ontolex: <http://www.w3.org/ns/lemon/ontolex#>                             \n" +
			" prefix lime: <http://www.w3.org/ns/lemon/lime#>                                   \n" +
            "                                                                                   \n" +
			" SELECT ?resource " + generateNatureSPARQLSelectPart() +" WHERE {                  \n" +
			"   ?entry ontolex:sense ?resource .                             	                \n" +
			generateNatureSPARQLWherePart("?resource") +
			" }                                                                                 \n" +
			" GROUP BY ?resource                                                                \n"
			// @formatter:on
		);
		qb.setBinding("entry", lexicalEntry);
		qb.processQName();
		qb.process(LexicalEntryRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}

	/**
	 * Adds a subterm to an {@code ontolex:LexicalEntry}.
	 * 
	 * @param lexicalEntry
	 * @param sublexicalEntry
	 * @param property
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, subterms)', 'C')")
	public void addSubterm(@LocallyDefined @Modified IRI lexicalEntry, IRI sublexicalEntry,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/ns/lemon/decomp#subterm") @Optional(defaultValue = "<http://www.w3.org/ns/lemon/decomp#subterm>") IRI property) {
		RepositoryConnection repConn = getManagedConnection();
		repConn.add(lexicalEntry, property, sublexicalEntry, getWorkingGraph());
	}

	/**
	 * Removes a subterm from an {@code ontolex:LexicalEntry}.
	 * 
	 * @param lexicalEntry
	 * @param sublexicalEntry
	 * @param property
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, subterms)', 'D')")
	public void removeSubterm(@LocallyDefined @Modified IRI lexicalEntry, IRI sublexicalEntry,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/ns/lemon/decomp#subterm") @Optional(defaultValue = "<http://www.w3.org/ns/lemon/decomp#subterm>") IRI property) {
		RepositoryConnection repConn = getManagedConnection();
		repConn.remove(lexicalEntry, property, sublexicalEntry, getWorkingGraph());
	}

	/**
	 * Sets the constituents of a lexical entry
	 * 
	 * @param lexicalEntry
	 * @param constituentLexicalEntries
	 * @param ordered
	 * @throws DeniedOperationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, constituents)', 'C')")
	public void setLexicalEntryConstituents(@LocallyDefined @Modified IRI lexicalEntry,
			@LocallyDefinedResources List<IRI> constituentLexicalEntries, boolean ordered)
			throws URIGenerationException, DeniedOperationException {
		Model triples2remove = prepareClearLexicalEntryConstituents(lexicalEntry);

		RepositoryConnection conn = getManagedConnection();
		ValueFactory vf = conn.getValueFactory();

		if (constituentLexicalEntries.stream().anyMatch(lexicalEntry::equals)) {
			throw new IllegalArgumentException(
					"A lexical entry should not be used as a constituent of itself");
		}

		TupleQuery subComponentsQuery = conn.prepareTupleQuery(
		// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                                            \n" +
			"PREFIX decomp: <http://www.w3.org/ns/lemon/decomp#>                                                             \n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>                                                                 \n" +
			"SELECT DISTINCT ?lexicalEntry ?component ?componentLexicalEntry ?index WHERE {                                  \n" +
			"    VALUES(?lexicalEntry) {                                                                                     \n" +
			constituentLexicalEntries.stream().map(RenderUtils::toSPARQL).collect(Collectors.joining(")\n    (", "    (", ")\n")) +
			"    }                                                                                                           \n" +
			"    ?constProp rdfs:subPropertyOf* decomp:constituent .                                                         \n" +
			"    ?lexicalEntry ?constProp ?component .                                                                       \n" +
			(ordered ?
			"    optional {                                                                                                  \n" +
			"        ?lexicalEntry ?prop ?component .                                                                        \n" +
			"        FILTER(REGEX(str(?prop),                                                                                \n" +
			"                \"^http://www\\\\.w3\\\\.org/1999/02/22-rdf-syntax-ns#_(\\\\d+)$\"))                            \n" +
			"   		bind(strdt(                                                                                          \n" +
			"                replace(str(?prop), \"^http://www\\\\.w3\\\\.org/1999/02/22-rdf-syntax-ns#_(\\\\d+)$\", \"$1\"),\n" +
			"                xsd:integer)                                                                                    \n" +
			"            as ?index)                                                                                          \n" +
			"    }                                                                                                           \n"
			:
			""
			) +
			"	optional {                                                                                                    \n" +
			"       ?corrProp rdfs:subPropertyOf* decomp:correspondsTo .                                                      \n" +
			"       ?component ?corrProp ?componentLexicalEntry .                                                             \n" +
			"   }                                                                                                             \n" +
			"}                                                                                                                \n" +
			"ORDER BY ?index                                                                                                  \n"
			// @formatter:on
		);

		logger.debug("subcomponents query:\n{}", subComponentsQuery);
		subComponentsQuery.setIncludeInferred(false);
		List<BindingSet> subComponentsBindings = QueryResults.asList(subComponentsQuery.evaluate());

		logger.debug("retrieved subcomponents: {}", subComponentsBindings);

		Map<IRI, List<BindingSet>> lexicalEntry2subComponents = subComponentsBindings.stream()
				.collect(Collectors.groupingBy(bs -> (IRI) bs.getValue("lexicalEntry")));

		Model triples2add = new LinkedHashModel();

		int index = 1;

		for (IRI constituentLexicalEntry : constituentLexicalEntries) {
			@Nullable
			List<BindingSet> subComponentBindingSets = lexicalEntry2subComponents
					.get(constituentLexicalEntry);

			if (subComponentBindingSets == null) {
				IRI component = generateIRI("ontolexComponent", Collections.emptyMap());
				triples2add.add(lexicalEntry, DECOMP.CONSTITUENT, component);
				triples2add.add(component, RDF.TYPE, DECOMP.COMPONENT);
				triples2add.add(component, DECOMP.CORRESPONDS_TO, constituentLexicalEntry);

				if (ordered) {
					triples2add.add(lexicalEntry,
							vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#_" + (index++)),
							component);
				}
			} else {

				triples2add.add(lexicalEntry, DECOMP.SUBTERM, constituentLexicalEntry);

				BigInteger previousIndex = BigInteger.valueOf(-1);

				for (BindingSet bs : subComponentBindingSets) {
					Resource subComponent = (Resource) bs.getValue("component");
					Resource subComponentLexicalEntry = (Resource) bs.getValue("componentLexicalEntry");

					if (subComponentLexicalEntry == null) {
						throw new IllegalArgumentException(
								"A component of the lexical entry \"" + constituentLexicalEntry
										+ "\" used as a constituent is not bound to any lexical entry");
					}

					if (subComponentLexicalEntry.equals(lexicalEntry)) {
						throw new IllegalArgumentException(
								"A lexical entry should not be used as a constituent of another lexical entry used as a constituent of the former");
					}

					triples2add.add(lexicalEntry, DECOMP.CONSTITUENT, subComponent);

					if (ordered) {
						@Nullable
						BigInteger currentIndex = java.util.Optional.ofNullable(bs.getValue("index"))
								.map(Literal.class::cast)
								.map(l -> Literals.getIntegerValue(l, BigInteger.ZERO)).orElse(null);

						if (currentIndex == null) {
							throw new IllegalArgumentException("A component of the lexical entry \""
									+ constituentLexicalEntry + "\" is not ordered");
						} else if (currentIndex.equals(previousIndex)) {
							throw new IllegalArgumentException(
									"The ordering of the components of the lexical " + "entry \""
											+ constituentLexicalEntries + "\" is ambiguous");
						}

						triples2add.add(lexicalEntry,
								vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#_" + (index++)),
								subComponent);
					}
				}
			}
		}

		conn.remove(Sets.difference(triples2remove, triples2add), getWorkingGraph());
		conn.add(Sets.difference(triples2add, triples2remove), getWorkingGraph());
	}

	/**
	 * Sets the constituents of a lexical entry
	 * 
	 * @param lexicalEntry
	 * @throws DeniedOperationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, constituents)', 'D')")
	public void clearLexicalEntryConstituents(@LocallyDefined @Modified IRI lexicalEntry)
			throws DeniedOperationException {
		Model triple2remove = prepareClearLexicalEntryConstituents(lexicalEntry);
		getManagedConnection().remove(triple2remove, getWorkingGraph());
	}

	public Model prepareClearLexicalEntryConstituents(IRI lexicalEntry)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException,
			DeniedOperationException, UpdateExecutionException {
		RepositoryConnection conn = getManagedConnection();

		// Checks that the operation can be done (e.g. it does not invole any statement pending for acceptance
		// or removal)

		String checkQueryString =
		// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                                  \n" +
			"PREFIX decomp: <http://www.w3.org/ns/lemon/decomp#>                                                   \n" +
			"ASK {                                                                                                 \n" +
			"   VALUES(?lexicalEntry ?workingGraph) {                                                              \n" +
			"      (" + RenderUtils.toSPARQL(lexicalEntry) + " " + RenderUtils.toSPARQL(getWorkingGraph()) + ")    \n" +
			"   }                                                                                                  \n" +
			"   ?decompProp rdfs:subPropertyOf* decomp:constituent .                                               \n" +
			"   graph ?g { ?lexicalEntry ?decompProp ?component . }                                                \n" +
			"	OPTIONAL {                                                                                         \n" +
			"		graph ?g2 {                                                                                    \n" +
			"			?lexicalEntry ?prop ?component                                                             \n" +
			"		}                                                                                              \n" +
			"		FILTER(REGEX(str(?prop), \"^http://www\\\\.w3\\\\.org/1999/02/22-rdf-syntax-ns#_(\\\\d+)$\"))  \n" +
			"		FILTER(!sameTerm(?g2, ?workingGraph))                                                          \n" +
			"	}                                                                                                  \n" +
			"	FILTER(!sameTerm(?g, ?workingGraph))                                                               \n" +
			"}                                                                                                     \n";
			// @formatter:on

		BooleanQuery checkQuery = conn.prepareBooleanQuery(checkQueryString);
		logger.debug("check query:\n{}", checkQueryString);

		checkQuery.setIncludeInferred(false);
		if (checkQuery.evaluate()) {
			throw new NonWorkingGraphUpdateException();
		}

		// Deletes the triples

		GraphQuery deleteQuery = conn.prepareGraphQuery(
		// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                              \n" +
			"PREFIX decomp: <http://www.w3.org/ns/lemon/decomp#>                                               \n" +
			"CONSTRUCT {                                                                                       \n" +
			"	?lexicalEntry ?decompProp ?component .                                                         \n" +
			"	?lexicalEntry ?nProp ?component .                                                              \n" +
			"	?comp ?compP ?compO .                                                                          \n" +
			"}                                                                                                 \n" +
			"WHERE {                                                                                           \n" +
			"   ?decompProp rdfs:subPropertyOf* decomp:constituent .                                           \n" +
			"   GRAPH ?workingGraph { ?lexicalEntry ?decompProp ?component . }                                 \n" +
			"	OPTIONAL {                                                                                     \n" +
			"	   GRAPH ?workingGraph {                                                                       \n" +
			"			?lexicalEntry ?nProp ?component .                                                            \n" +
			"			FILTER(REGEX(str(?nProp), \"^http://www\\\\.w3\\\\.org/1999/02/22-rdf-syntax-ns#_(\\\\d+)$\"))\n" +
			"		}                                                                                          \n" +
			"	}                                                                                              \n" +
			"	optional {                                                                                     \n" +
			"		?component decomp:constituent* ?comp .                                                     \n" +
			"		FILTER NOT EXISTS {                                                                        \n" +
			"		  ?decompProp2 rdfs:subPropertyOf* decomp:constituent .                                    \n" +
			"		  ?decompProp3 rdfs:subPropertyOf* decomp:constituent .                                    \n" +
			"		  ?parentComp1 ?decompProp2 ?component .                                                   \n" +
			"		  ?parentComp2 ?decompProp2 ?component .                                                   \n" +
			"		  FILTER(!sameTerm(?parentComp1, ?parentComp2))                                            \n" +
			"		}		                                                                                   \n" +
			"		GRAPH ?workingGraph { ?comp ?compP ?compO . }                                              \n" +
			"	}                                                                                              \n" +
			"}                                                                                                 \n"
			// @formatter:on
		);
		deleteQuery.setBinding("lexicalEntry", lexicalEntry);
		deleteQuery.setBinding("workingGraph", getWorkingGraph());
		return QueryResults.asModel(deleteQuery.evaluate());
	}

	/* --- Forms --- */

	/**
	 * Sets the canonical form of a given lexical entry.
	 * 
	 * @param lexicalEntry
	 * @param newForm
	 * @param writtenRep
	 * @param customFormValue
	 * @throws URIGenerationException
	 * @throws CODAException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, lexicalForms)', 'C')")
	public void setCanonicalForm(@Modified @LocallyDefined IRI lexicalEntry,
			@Optional @NotLocallyDefined IRI newForm, @LanguageTaggedString Literal writtenRep,
			@Optional CustomFormValue customFormValue)
			throws URIGenerationException, CODAException, CustomFormException {
		addFormInternal(newForm, writtenRep, lexicalEntry, customFormValue, ONTOLEX.CANONICAL_FORM, true);
	}

	/**
	 * Adds an other form of a given lexical entry.
	 * 
	 * @param lexicalEntry
	 * @param newForm
	 * @param writtenRep
	 * @param customFormValue
	 * @throws URIGenerationException
	 * @throws CODAException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, lexicalForms)', 'C')")
	public void addOtherForm(@Modified @LocallyDefined IRI lexicalEntry,
			@Optional @NotLocallyDefined IRI newForm, @LanguageTaggedString Literal writtenRep,
			@Optional CustomFormValue customFormValue)
			throws URIGenerationException, CODAException, CustomFormException {
		addFormInternal(newForm, writtenRep, lexicalEntry, customFormValue, ONTOLEX.OTHER_FORM, false);
	}

	// Annotations are not interpreted on non-service methods. Left for documentation purpose.
	protected void addFormInternal(@Optional @NotLocallyDefined IRI newForm,
			@LanguageTaggedString Literal writtenRep, @LocallyDefined IRI lexicalEntry,
			@Optional CustomFormValue customFormValue, IRI property, boolean replaces)
			throws URIGenerationException, CODAException, CustomFormException {

		RepositoryConnection repoConnection = getManagedConnection();

		String lexicalEntryLanguage = getLexicalEntryLanguageInternal(repoConnection, lexicalEntry)
				.orElseThrow(() -> new RuntimeException("The lexical entry does not declare any language"));
		String formLanguage = writtenRep.getLanguage()
				.orElseThrow(() -> new RuntimeException("The form does not declare any language"));

		if (!langMatches(formLanguage, lexicalEntryLanguage)) {
			throw new IllegalArgumentException("The form is expressed in a natural language (" + formLanguage
					+ ") not compatible with the lexical entry (" + lexicalEntryLanguage + ")");
		}

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newFormIRI;

		if (newForm == null) {
			newFormIRI = generateFormIRI(lexicalEntry, writtenRep, property);
		} else {
			newFormIRI = newForm;
		}

		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(newFormIRI,
				RDFResourceRole.ontolexForm); // set created for versioning

		modelAdditions.add(newFormIRI, RDF.TYPE, ONTOLEX.FORM);
		modelAdditions.add(newFormIRI, ONTOLEX.WRITTEN_REP, writtenRep);

		modelAdditions.add(lexicalEntry, property, newFormIRI);

		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(lexicalEntry,
				RDFResourceRole.ontolexLexicalEntry);

		if (replaces) {
			Model previousFormStatements = QueryResults
					.asModel(repoConnection.getStatements(lexicalEntry, property, null, getWorkingGraph()));

			modelRemovals.addAll(previousFormStatements);

			Set<Resource> deletedForms = previousFormStatements.objects().stream()
					.filter(Resource.class::isInstance).map(Resource.class::cast).collect(toSet());

			deletedForms.stream().filter(Resource.class::isInstance).map(form -> {
				Model removedStatements = new LinkedHashModel();
				removedStatements.addAll(QueryResults
						.asModel(repoConnection.getStatements(form, null, null, false, getWorkingGraph())));
				removedStatements.addAll(QueryResults
						.asModel(repoConnection.getStatements(null, null, form, false, getWorkingGraph())));

				return removedStatements;
			}).forEach(modelRemovals::addAll);
		}

		// enrich with custom form
		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newFormIRI.stringValue());
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
			enrichWithCustomForm(getManagedConnection(), modelAdditions, modelRemovals, cForm,
					customFormValue.getUserPromptMap(), stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}

	/**
	 * Removes a form of a lexical entry, and deletes it.
	 * 
	 * @param lexicalEntry
	 * @param property
	 * @param form
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, lexicalForms)', 'D')")
	public void removeForm(@LocallyDefined @Modified Resource lexicalEntry,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/ns/lemon/ontolex#lexicalForm") IRI property,
			@LocallyDefined Resource form) {
		RepositoryConnection repConn = getManagedConnection();

		if (!repConn.hasStatement(lexicalEntry, property, form, false, getWorkingGraph())) {
			throw new IllegalArgumentException("Not a form of the indicated lexical entry");
		}

		// Removes ingoing links
		repConn.remove((Resource) null, null, form, getWorkingGraph());

		// Removes outgoing links
		repConn.remove(form, null, null, getWorkingGraph());
	}

	/**
	 * Adds a representation to an {@code ontolex:Form}.
	 * 
	 * @param form
	 * @param representation
	 * @param property
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexForm, formRepresentations)', 'C')")
	public void addFormRepresentation(@LocallyDefined @Modified Resource form, Literal representation,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/ns/lemon/ontolex#representation") IRI property) {

		RepositoryConnection repConn = getManagedConnection();

		String formLanguage = getFormLanguageInternal(repConn, form)
				.orElseThrow(() -> new RuntimeException("The form does not declare any language"));
		String representationLanguage = representation.getLanguage()
				.orElseThrow(() -> new RuntimeException("The representation does not declare any language"));

		if (!langMatches(representationLanguage, formLanguage)) {
			throw new IllegalArgumentException("The representation is expressed in a natural language ("
					+ representationLanguage + ") not compatible with the form (" + formLanguage + ")");
		}

		repConn.add(form, property, representation, getWorkingGraph());
	}

	/**
	 * Updates a representation of an {@code ontolex:Form}.
	 * 
	 * @param form
	 * @param representation
	 * @param newRepresentation
	 * @param property
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexForm, formRepresentations)', 'U')")
	public void updateFormRepresentation(@LocallyDefined @Modified Resource form, Literal representation,
			Literal newRepresentation,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/ns/lemon/ontolex#representation") IRI property) {

		RepositoryConnection repConn = getManagedConnection();

		String formLanguage = getFormLanguageInternal(repConn, form)
				.orElseThrow(() -> new RuntimeException("The form does not declare any language"));
		String representationLanguage = newRepresentation.getLanguage()
				.orElseThrow(() -> new RuntimeException("The representation does not declare any language"));

		if (!langMatches(representationLanguage, formLanguage)) {
			throw new IllegalArgumentException("The representation is expressed in a natural language ("
					+ representationLanguage + ") not compatible with the form (" + formLanguage + ")");
		}

		repConn.remove(form, property, representation, getWorkingGraph());
		repConn.add(form, property, newRepresentation, getWorkingGraph());
	}

	/**
	 * Removes a representations from an {@code ontolex:Form}.
	 * 
	 * @param form
	 * @param representation
	 * @param property
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexForm, formRepresentations)', 'D')")
	public void removeFormRepresentation(@LocallyDefined @Modified Resource form, Literal representation,
			@SubPropertyOf(superPropertyIRI = "http://www.w3.org/ns/lemon/ontolex#representation") IRI property) {
		RepositoryConnection repConn = getManagedConnection();
		repConn.remove(form, property, representation, getWorkingGraph());
	}

	/**
	 * Returns the language declared by the provided lexical entry, or as fallback the one declared by the
	 * lexicon
	 * 
	 * @param form
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexForm)', 'R')")
	public String getFormLanguage(@LocallyDefined Resource form) {
		return OntoLexLemon.getFormLanguageInternal(getManagedConnection(), form).orElse(null);
	}

	/* --- Lexicalizations --- */

	/**
	 * Adds a lexicalization of the RDF resource {@code reference} using the {@code ontolex:LexicalEntry}
	 * {@code lexicalEntry}. If {@code createPlain} is {@code true}, then a plain lexicalization directly
	 * connecting the lexical entry and the reference is created as follows:
	 * <ul>
	 * <li>If {@code lexicalEntry} is defined in the working graph (see {@link STServiceContext#getWGraph()},
	 * then a triple with the property {@code ontolex:denotes} is asserted;</li>
	 * <li>If {@code reference} is defined in the working graph, then a triple with the property
	 * {@code ontolex:isDenoted is asserted;</li>
	 * 
	<li>If neither is defined in the working graph, then an exception is thrown.</li>
	 * 
	</ul>
	 * If {@code createSense} is {@code true}, then an {@code ontolex:LexicalSense} is created (possibly in
	 * addition to the plain lexicalization) and connected to the lexical entry and the reference following a
	 * policy analogous to the one already described. Differently from the case above, the creation of a sense
	 * does not fail if both the lexical entry and the reference aren't locally defined. Indeed, the service
	 * will just create a sense and connect it to both.
	 * 
	 * @param lexicalEntry
	 * 
	 * @param reference
	 * @param createPlain
	 * @param createSense
	 * @param lexicalSenseCls
	 * @param customFormValue
	 * @throws URIGenerationException
	 * @throws CustomFormException
	 * @throws CODAException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#reference)+ ', lexicalization)', 'C')")
	public void addLexicalization(Resource lexicalEntry, Resource reference, boolean createPlain,
			boolean createSense,
			@SubClassOf(superClassIRI = "http://www.w3.org/ns/lemon/ontolex#LexicalSense") @Optional(defaultValue = "<http://www.w3.org/ns/lemon/ontolex#LexicalSense>") IRI lexicalSenseCls,
			@Optional CustomFormValue customFormValue)
			throws URIGenerationException, CODAException, CustomFormException {
		if (!createPlain && !createSense) {
			throw new IllegalArgumentException("Either <createPlain> or <createSense> shall be enabled");
		}
		RepositoryConnection conn = getManagedConnection();

		Resource workingGraph = getWorkingGraph();

		boolean lexicalEntryLocallyDefined = isLocallyDefined(lexicalEntry);
		boolean referenceLocallyDefined = isLocallyDefined(reference);

		// BooleanQuery checkQuery = conn.prepareBooleanQuery(
//		// @formatter:off
//			" PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                           \n" +
//			" SELECT (COALESCE(?plainT) as ?plain) (COALESCE(?reifiedT) as ?reified) {        \n" +
//			"   {                                                                             \n" +
//			"     ?lexicalEntry ontolex:evokes|^ontolex:isEvokedBy ?reference .               \n" +
//			"   } UNION {                                                                     \n" +
//			"     ?lexicalSense ontolex:reference|^ontolex:isReferenceOf ?reference ;         \n" +
//			"       ^ontolex:sense|ontolex:isSenseOf ?lexicalEntry .                          \n" +
//			"   }                                                                             \n" +
//			" }                                                                               \n"
//			// @formatter:on
		// );

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (createPlain) {
			boolean tripleAdded = false;

			if (lexicalEntryLocallyDefined) {
				modelAdditions.add(lexicalEntry, ONTOLEX.DENOTES, reference);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(lexicalEntry, RDFResourceRole.ontolexLexicalEntry);
				tripleAdded = true;
			}

			if (referenceLocallyDefined) {
				modelAdditions.add(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(reference);
				tripleAdded = true;
			}

			if (!tripleAdded) {
				throw new IllegalArgumentException(
						"Unable to create a plain lexicalization because neither the lexical entry nor the reference are locally defined");
			}
		}

		if (createSense) {
			IRI lexicalSenseIRI = generateLexicalSenseIRI(lexicalEntry, reference);

			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(lexicalSenseIRI,
					RDFResourceRole.ontolexLexicalSense);

			modelAdditions.add(lexicalSenseIRI, RDF.TYPE, lexicalSenseCls);
			modelAdditions.add(lexicalSenseIRI, ONTOLEX.IS_SENSE_OF, lexicalEntry);
			modelAdditions.add(lexicalSenseIRI, ONTOLEX.REFERENCE, reference);

			if (lexicalEntryLocallyDefined) {
				modelAdditions.add(lexicalEntry, ONTOLEX.SENSE, lexicalSenseIRI);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(lexicalEntry, RDFResourceRole.ontolexLexicalEntry);
			}

			if (referenceLocallyDefined) {
				modelAdditions.add(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSenseIRI);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(reference);
			}
			// enrich with custom form
			if (customFormValue != null) {
				StandardForm stdForm = new StandardForm();
				stdForm.addFormEntry(StandardForm.Prompt.resource, lexicalSenseIRI.stringValue());

				CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
				enrichWithCustomForm(getManagedConnection(), modelAdditions, modelRemovals, cForm,
						customFormValue.getUserPromptMap(), stdForm);
			}

		}

		conn.remove(modelRemovals, getWorkingGraph());
		conn.add(modelAdditions, getWorkingGraph());
	}

	protected boolean isLocallyDefined(Resource resource)
			throws RepositoryException, MalformedQueryException {
		Resource workingGraph = getWorkingGraph();
		BooleanQuery definedQuery = getManagedConnection().prepareBooleanQuery(
		// @formatter:off
			" ASK {                                 \n" +
			"   VALUES(?g) {                        \n" +
			"      (" + RenderUtils.toSPARQL(workingGraph) + ")\n" + 
			(
			ValidationUtilities.isValidationEnabled(stServiceContext)
			?
			"      (" + RenderUtils.toSPARQL(VALIDATION.stagingAddGraph(workingGraph)) + ")\n" + 
			"      (" + RenderUtils.toSPARQL(VALIDATION.stagingRemoveGraph(workingGraph)) + ")\n"
			:
			""
			) + 
			"   }                                   \n" +
			"   GRAPH ?g {                          \n" +
			"     ?subject ?p ?o .                  \n" +
			"   }                                   \n" +
			" }                                     \n");
			// @formatter:on
		definedQuery.setIncludeInferred(false);
		definedQuery.setBinding("subject", resource);
		return definedQuery.evaluate();
	}

	/**
	 * Removes a plain lexicalization. This operation removes the triples connecting the lexical entry and the
	 * reference in both directions.
	 * 
	 * @param lexicalEntry
	 * @param reference
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#reference)+ ', lexicalization)', 'D')")
	public void removePlainLexicalization(Resource lexicalEntry, Resource reference) {
		RepositoryConnection conn = getManagedConnection();
		boolean tripleRemoved = false;

		if (conn.hasStatement(lexicalEntry, ONTOLEX.DENOTES, reference, false, getWorkingGraph())) {
			tripleRemoved = true;
			conn.remove(lexicalEntry, ONTOLEX.DENOTES, reference, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(lexicalEntry);
		}

		if (conn.hasStatement(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, false, getWorkingGraph())) {
			tripleRemoved = true;
			conn.remove(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(reference);
		}

		if (!tripleRemoved) {
			throw new IllegalArgumentException(
					"Unable to delete a plain lexicalization because neither the lexical entry nor the reference are locally defined");
		}
	}

	/**
	 * Removes a reified lexicalization expressed as an {@code ontolex:LexicalSense}. Optionally, it is
	 * possible to remove the corresponding plain lexicalization(s).
	 * 
	 * @param lexicalSense
	 * @param removePlain
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, lexicalization)', 'D')")
	public void removeReifiedLexicalization(Resource lexicalSense, boolean removePlain) {
		RepositoryConnection conn = getManagedConnection();

		Set<Resource> lexicalEntries = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.IS_SENSE_OF, null, false, getWorkingGraph())));
		Set<Resource> references = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.REFERENCE, null, false, getWorkingGraph())));

		conn.remove(lexicalSense, null, null, getWorkingGraph());

		for (Resource lexicalEntry : lexicalEntries) {
			if (conn.hasStatement(lexicalEntry, ONTOLEX.SENSE, lexicalSense, false, getWorkingGraph())) {
				conn.remove(lexicalEntry, ONTOLEX.SENSE, lexicalSense, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(lexicalEntry, RDFResourceRole.ontolexLexicalEntry);
			}
		}

		for (Resource reference : references) {
			if (conn.hasStatement(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, false,
					getWorkingGraph())) {
				conn.remove(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(reference);
			}
		}

		conn.remove(lexicalSense, null, null, getWorkingGraph());
		conn.remove((Resource) null, null, lexicalSense, getWorkingGraph());

		if (removePlain) {
			for (Resource lexicalEntry : lexicalEntries) {
				for (Resource reference : references) {
					if (conn.hasStatement(lexicalEntry, ONTOLEX.DENOTES, reference, false,
							getWorkingGraph())) {
						conn.remove(lexicalEntry, ONTOLEX.DENOTES, reference, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(lexicalEntry);
					}

					if (conn.hasStatement(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, false,
							getWorkingGraph())) {
						conn.remove(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(reference);
					}
				}
			}
		}

	}

	/* --- Conceptualizations --- */

	/**
	 * Adds a conceptualization of the RDF resource {@code concept} using the {@code ontolex:LexicalEntry}
	 * {@code lexicalEntry}. If {@code createPlain} is {@code true}, then a plain conceptualization directly
	 * connecting the lexical entry and the reference is created as follows:
	 * <ul>
	 * <li>If {@code lexicalEntry} is defined in the working graph (see {@link STServiceContext#getWGraph()},
	 * then a triple with the property {@code ontolex:evokes} is asserted;</li>
	 * <li>If {@code concept} is defined in the working graph, then a triple with the property
	 * {@code ontolex:isEvokedBy} is asserted;</li>
	 * <li>If neither is defined in the working graph, then an exception is thrown.</li>
	 * </ul>
	 * If {@code createSense} is {@code true}, then an {@code ontolex:LexicalSense} is created (possibly in
	 * addition to the plain conceptualization) and connected to the lexical entry and the reference following
	 * a policy analogous to the one already described. Differently from the case above, the creation of a
	 * sense does not fail if both the lexical entry and the reference aren't locally defined. Indeed, the
	 * service will just create a sense and connect it to both.
	 * 
	 * @param lexicalEntry
	 * @param concept
	 * @param createPlain
	 * @param createSense
	 * @param lexicalSenseCls
	 * @param customFormValue
	 * @throws URIGenerationException
	 * @throws CustomFormException
	 * @throws CODAException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, conceptualization)', 'C')")
	public void addConceptualization(Resource lexicalEntry, Resource concept, boolean createPlain,
			boolean createSense,
			@SubClassOf(superClassIRI = "http://www.w3.org/ns/lemon/ontolex#LexicalSense") @Optional(defaultValue = "<http://www.w3.org/ns/lemon/ontolex#LexicalSense>") IRI lexicalSenseCls,
			@Optional CustomFormValue customFormValue)
			throws URIGenerationException, CODAException, CustomFormException {
		if (!createPlain && !createSense) {
			throw new IllegalArgumentException("Either <createPlain> or <createSense> shall be enabled");
		}
		RepositoryConnection conn = getManagedConnection();

		boolean lexicalEntryLocallyDefined = isLocallyDefined(lexicalEntry);

		boolean conceptLocallyDefined = isLocallyDefined(concept);

		// BooleanQuery checkQuery = conn.prepareBooleanQuery(
//		// @formatter:off
//			" PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>                           \n" +
//			" SELECT (COALESCE(?plainT) as ?plain) (COALESCE(?reifiedT) as ?reified) {        \n" +
//			"   {                                                                             \n" +
//			"     ?lexicalEntry ontolex:evokes|^ontolex:isEvokedBy ?reference .               \n" +
//			"   } UNION {                                                                     \n" +
//			"     ?lexicalSense ontolex:reference|^ontolex:isReferenceOf ?reference ;         \n" +
//			"       ^ontolex:sense|ontolex:isSenseOf ?lexicalEntry .                          \n" +
//			"   }                                                                             \n" +
//			" }                                                                               \n"
//			// @formatter:on
		// );

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (createPlain) {
			boolean tripleAdded = false;

			if (lexicalEntryLocallyDefined) {
				modelAdditions.add(lexicalEntry, ONTOLEX.EVOKES, concept);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(lexicalEntry, RDFResourceRole.ontolexLexicalEntry);
				tripleAdded = true;
			}

			if (conceptLocallyDefined) {
				modelAdditions.add(concept, ONTOLEX.IS_EVOKED_BY, lexicalEntry, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(concept);
				tripleAdded = true;
			}

			if (!tripleAdded) {
				throw new IllegalArgumentException(
						"Unable to create a plain conceptualization because neither the lexical entry nor the concept are locally defined");
			}
		}

		if (createSense) {
			IRI lexicalSenseIRI = generateLexicalSenseIRI(lexicalEntry, concept);

			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(lexicalSenseIRI,
					RDFResourceRole.ontolexLexicalSense);

			modelAdditions.add(lexicalSenseIRI, RDF.TYPE, lexicalSenseCls);
			modelAdditions.add(lexicalSenseIRI, ONTOLEX.IS_SENSE_OF, lexicalEntry);
			modelAdditions.add(lexicalSenseIRI, ONTOLEX.IS_LEXICALIZED_SENSE_OF, concept);

			if (lexicalEntryLocallyDefined) {
				modelAdditions.add(lexicalEntry, ONTOLEX.SENSE, lexicalSenseIRI);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(lexicalEntry, RDFResourceRole.ontolexLexicalEntry);
			}

			if (conceptLocallyDefined) {
				modelAdditions.add(concept, ONTOLEX.LEXICALIZED_SENSE, lexicalSenseIRI);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(concept);
			}
			// enrich with custom form
			if (customFormValue != null) {
				StandardForm stdForm = new StandardForm();
				stdForm.addFormEntry(StandardForm.Prompt.resource, lexicalSenseIRI.stringValue());

				CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
				enrichWithCustomForm(getManagedConnection(), modelAdditions, modelRemovals, cForm,
						customFormValue.getUserPromptMap(), stdForm);
			}

		}

		conn.remove(modelRemovals, getWorkingGraph());
		conn.add(modelAdditions, getWorkingGraph());
	}

	/**
	 * Removes a plain conceptualization. This operation removes the triples connecting the lexical entry and
	 * the concpet in both directions.
	 * 
	 * @param lexicalEntry
	 * @param reference
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, conceptualization)', 'D')")
	public void removePlainConceptualization(Resource lexicalEntry, Resource concept) {
		RepositoryConnection conn = getManagedConnection();
		boolean tripleRemoved = false;

		if (conn.hasStatement(lexicalEntry, ONTOLEX.EVOKES, concept, false, getWorkingGraph())) {
			tripleRemoved = true;
			conn.remove(lexicalEntry, ONTOLEX.EVOKES, concept, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(lexicalEntry);
		}

		if (conn.hasStatement(concept, ONTOLEX.IS_EVOKED_BY, lexicalEntry, false, getWorkingGraph())) {
			tripleRemoved = true;
			conn.remove(concept, ONTOLEX.IS_EVOKED_BY, lexicalEntry, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(concept);
		}

		if (!tripleRemoved) {
			throw new IllegalArgumentException(
					"Unable to delete a plain conceptualization because neither the lexical entry nor the concept are locally defined");
		}
	}

	/* --- Senses --- */

	/**
	 * Removes a lexical sense (see {@code ontolex:LexicalSense}). Optionally, it is possible to remove the
	 * corresponding plain lexicalization(s) and conceptualizations.
	 * 
	 * @param lexicalSense
	 * @param removePlain
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, sense)', 'D')")
	public void removeSense(Resource lexicalSense, boolean removePlain) {
		RepositoryConnection conn = getManagedConnection();

		Set<Resource> lexicalEntries = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.IS_SENSE_OF, null, false, getWorkingGraph())));
		Set<Resource> references = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.REFERENCE, null, false, getWorkingGraph())));
		Set<Resource> concepts = Models.objectResources(QueryResults.asModel(conn.getStatements(lexicalSense,
				ONTOLEX.IS_LEXICALIZED_SENSE_OF, null, false, getWorkingGraph())));

		conn.remove(lexicalSense, null, null, getWorkingGraph());

		for (Resource lexicalEntry : lexicalEntries) {
			if (conn.hasStatement(lexicalEntry, ONTOLEX.SENSE, lexicalSense, false, getWorkingGraph())) {
				conn.remove(lexicalEntry, ONTOLEX.SENSE, lexicalSense, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
						.addModifiedResource(lexicalEntry, RDFResourceRole.ontolexLexicalEntry);
			}
		}

		for (Resource reference : references) {
			if (conn.hasStatement(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, false,
					getWorkingGraph())) {
				conn.remove(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(reference);
			}
		}

		for (Resource concept : concepts) {
			if (conn.hasStatement(concept, ONTOLEX.LEXICALIZED_SENSE, lexicalSense, false,
					getWorkingGraph())) {
				conn.remove(concept, ONTOLEX.LEXICALIZED_SENSE, lexicalSense, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(concept);
			}
		}

		Update reifiedRelationRemoval = conn.prepareUpdate(
		//@formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
			"PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>\n" + 
			"DELETE  {\n" + 
			"	?relation ?p ? o .\n" + 
			"}\n" + 
			"WHERE {\n" + 
			"   ?relatesProp rdfs:subPropertyOf* vartrans:relates.\n" +
			"	?relation ?relatesProp ?resource .\n" + 
			"	?relation ?p ? o .\n" + 
			"}"
			//@formatter:on
		);
		SimpleDataset dataset = new SimpleDataset();
		dataset.addDefaultRemoveGraph((IRI) getWorkingGraph());
		reifiedRelationRemoval.setDataset(dataset);
		reifiedRelationRemoval.setBinding("resource", lexicalSense);
		reifiedRelationRemoval.execute();

		conn.remove(lexicalSense, null, null, getWorkingGraph());
		conn.remove((Resource) null, null, lexicalSense, getWorkingGraph());

		if (removePlain) {
			for (Resource lexicalEntry : lexicalEntries) {
				for (Resource reference : references) {
					if (conn.hasStatement(lexicalEntry, ONTOLEX.DENOTES, reference, false,
							getWorkingGraph())) {
						conn.remove(lexicalEntry, ONTOLEX.DENOTES, reference, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(lexicalEntry);
					}

					if (conn.hasStatement(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, false,
							getWorkingGraph())) {
						conn.remove(reference, ONTOLEX.IS_DENOTED_BY, lexicalEntry, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(reference);
					}
				}

				for (Resource concept : concepts) {
					if (conn.hasStatement(lexicalEntry, ONTOLEX.EVOKES, concept, false, getWorkingGraph())) {
						conn.remove(lexicalEntry, ONTOLEX.EVOKES, concept, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(lexicalEntry);
					}

					if (conn.hasStatement(concept, ONTOLEX.IS_EVOKED_BY, lexicalEntry, false,
							getWorkingGraph())) {
						conn.remove(concept, ONTOLEX.IS_EVOKED_BY, lexicalEntry, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(concept);
					}
				}

			}
		}

	}

	/**
	 * Sets the reference of a lexical sense. Optionally, deletes plain references and, if specified, creates
	 * new ones.
	 * 
	 * @param lexicalSense
	 * @param newReference
	 * @param deletePlain
	 * @param createPlain
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(resource, sense)', 'U')")
	public void setReference(Resource lexicalSense, Resource newReference, boolean deletePlain,
			boolean createPlain) {
		RepositoryConnection conn = getManagedConnection();

		boolean locallyDefinedNewReference = isLocallyDefined(newReference);

		Set<Resource> lexicalEntries = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.IS_SENSE_OF, null, false, getWorkingGraph())));
		Set<Resource> references = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.REFERENCE, null, false, getWorkingGraph())));

		lexicalEntries.forEach(e -> {
			if (isLocallyDefined(e))
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(e);
		});

		for (Resource reference : references) {
			if (conn.hasStatement(lexicalSense, ONTOLEX.REFERENCE, reference, false, getWorkingGraph())) {
				conn.remove(lexicalSense, ONTOLEX.REFERENCE, reference, getWorkingGraph());
			}

			if (conn.hasStatement(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, false,
					getWorkingGraph())) {
				conn.remove(reference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, getWorkingGraph());
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(reference);
			}
		}

		conn.add(lexicalSense, ONTOLEX.REFERENCE, newReference, getWorkingGraph());

		if (locallyDefinedNewReference) {
			conn.add(newReference, ONTOLEX.IS_REFERENCE_OF, lexicalSense, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(newReference);
		}

		if (deletePlain) {
			for (Resource entry : lexicalEntries) {
				for (Resource reference : references) {
					if (conn.hasStatement(entry, ONTOLEX.DENOTES, reference, false, getWorkingGraph())) {
						conn.remove(entry, ONTOLEX.DENOTES, reference, getWorkingGraph());
					}

					if (conn.hasStatement(reference, ONTOLEX.IS_DENOTED_BY, entry, false,
							getWorkingGraph())) {
						conn.remove(reference, ONTOLEX.IS_DENOTED_BY, entry, getWorkingGraph());
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata()
								.addModifiedResource(reference);
					}

				}
			}

			if (createPlain) {
				for (Resource entry : lexicalEntries) {
					boolean locallyDefinedEntry = isLocallyDefined(entry);

					if (locallyDefinedEntry) {
						conn.add(entry, ONTOLEX.DENOTES, newReference, getWorkingGraph());
					}

					if (locallyDefinedNewReference) {
						conn.add(newReference, ONTOLEX.IS_DENOTED_BY, entry, getWorkingGraph());
					}
				}
			}
		}
	}

	/**
	 * Adds a lexical concept to a lexical sense. Optionally, creates plain conceptualizations.
	 * 
	 * @param lexicalSense
	 * @param newConcept
	 * @param createPlain
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, conceptualization)', 'U')")
	public void addConcept(Resource lexicalSense, Resource newConcept, boolean createPlain) {
		RepositoryConnection conn = getManagedConnection();

		boolean locallyDefinedNewConcept = isLocallyDefined(newConcept);

		Set<Resource> lexicalEntries = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.IS_SENSE_OF, null, false, getWorkingGraph())));

		lexicalEntries.forEach(e -> {
			if (isLocallyDefined(e))
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(e);
		});

		conn.add(lexicalSense, ONTOLEX.IS_LEXICALIZED_SENSE_OF, newConcept, getWorkingGraph());

		if (locallyDefinedNewConcept) {
			conn.add(newConcept, ONTOLEX.LEXICALIZED_SENSE, lexicalSense, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(newConcept);
		}

		if (createPlain) {
			for (Resource entry : lexicalEntries) {
				boolean locallyDefinedEntry = isLocallyDefined(entry);

				if (locallyDefinedEntry) {
					conn.add(entry, ONTOLEX.EVOKES, newConcept, getWorkingGraph());
				}

				if (locallyDefinedNewConcept) {
					conn.add(newConcept, ONTOLEX.IS_EVOKED_BY, entry, getWorkingGraph());
				}
			}
		}
	}

	/**
	 * Removes a lexical concept from a lexical sense. Optionally, delete plain conceptualizations.
	 * 
	 * @param lexicalSense
	 * @param newConcept
	 * @param deletePlain
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry, conceptualization)', 'D')")
	public void removeConcept(Resource lexicalSense, Resource concept, boolean deletePlain) {
		RepositoryConnection conn = getManagedConnection();

		boolean locallyDefinedConcept = isLocallyDefined(concept);

		Set<Resource> lexicalEntries = Models.objectResources(QueryResults.asModel(
				conn.getStatements(lexicalSense, ONTOLEX.IS_SENSE_OF, null, false, getWorkingGraph())));

		lexicalEntries.forEach(e -> {
			if (isLocallyDefined(e))
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(e);
		});

		conn.remove(lexicalSense, ONTOLEX.IS_LEXICALIZED_SENSE_OF, concept, getWorkingGraph());

		if (locallyDefinedConcept) {
			conn.remove(concept, ONTOLEX.LEXICALIZED_SENSE, lexicalSense, getWorkingGraph());
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addModifiedResource(concept);
		}

		if (deletePlain) {
			for (Resource entry : lexicalEntries) {
				boolean locallyDefinedEntry = isLocallyDefined(entry);

				if (locallyDefinedEntry) {
					conn.remove(entry, ONTOLEX.EVOKES, concept, getWorkingGraph());
				}

				if (locallyDefinedConcept) {
					conn.add(concept, ONTOLEX.IS_EVOKED_BY, entry, getWorkingGraph());
				}
			}
		}
	}

	/* --- LexicoSemanticRelations --- */

	/**
	 * Returns the categories of lexical relations, possibly filtered based on the linguistic catalogs of the
	 * supplied lexicon.
	 * 
	 * @param lexicalSense
	 * @param newConcept
	 * @param deletePlain
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<IRI>> getLexicalRelationCategories(
			@Optional @LocallyDefined Resource lexicon) {
		RepositoryConnection con = getManagedConnection();
		Set<IRI> linguisticCatalogs = OntoLexLemon.getLinguisticCatalogs(con, lexicon);
		List<AnnotatedValue<IRI>> categories = new ArrayList<>();

		if (linguisticCatalogs.contains(LEXINFO)) {
			QueryBuilder qb = createQueryBuilder(
			//@formatter:off
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>\n" + 
				"SELECT ?resource WHERE {\n" + 
				"	?resource rdfs:subPropertyOf+ vartrans:lexicalRel .\n" + 
				"	FILTER(isIRI(?resource) && STRSTARTS(STR(?resource),\"http://www.lexinfo.net/ontology/3.0/lexinfo#\"))\n" + 
				"}\n" + 
				"GROUP BY ?resource "
				//@formatter:on
			);
			qb.processStandardAttributes();
			qb.runQuery().forEach(r -> categories.add((AnnotatedValue<IRI>) (Object) r));
		}
		return categories;
	}

	/**
	 * Returns the categories of sense relations, possibly filtered based on the linguistic catalogs of the
	 * supplied lexicon.
	 * 
	 * @param lexicalSense
	 * @param newConcept
	 * @param deletePlain
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<IRI>> getSenseRelationCategories(
			@Optional @LocallyDefined Resource lexicon) {
		RepositoryConnection con = getManagedConnection();
		Set<IRI> linguisticCatalogs = OntoLexLemon.getLinguisticCatalogs(con, lexicon);
		List<AnnotatedValue<IRI>> categories = new ArrayList<>();

		if (linguisticCatalogs.contains(WN)) {
			QueryBuilder qb = createQueryBuilder(
			//@formatter:off
				"PREFIX wn: <https://globalwordnet.github.io/schemas/wn#>\n" +
				"SELECT ?resource WHERE {\n" + 
				"	?resource a wn:SenseRelType.\n" + 
				"	FILTER(isIRI(?resource) && STRSTARTS(STR(?resource),\"https://globalwordnet.github.io/schemas/wn#\"))\n" + 
				"}\n" + 
				"GROUP BY ?resource "
				//@formatter:on
			);
			qb.processStandardAttributes();
			qb.runQuery().forEach(r -> categories.add((AnnotatedValue<IRI>) (Object) r));
		}

		if (linguisticCatalogs.contains(LEXINFO)) {
			QueryBuilder qb = createQueryBuilder(
			//@formatter:off
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>\n" + 
				"SELECT ?resource WHERE {\n" + 
				"	?resource rdfs:subPropertyOf+ vartrans:senseRel .\n" + 
				"	FILTER(isIRI(?resource) && STRSTARTS(STR(?resource),\"http://www.lexinfo.net/ontology/3.0/lexinfo#\"))\n" + 
				"}\n" + 
				"GROUP BY ?resource "
				//@formatter:on
			);
			qb.processStandardAttributes();
			qb.runQuery().forEach(r -> categories.add((AnnotatedValue<IRI>) (Object) r));
		}
		return categories;
	}

	/**
	 * Returns the categories of conceptual relations, possibly filtered based on the linguistic catalogs of
	 * the supplied lexicon.
	 * 
	 * @param lexicalSense
	 * @param newConcept
	 * @param deletePlain
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(property)', 'R')")
	public Collection<AnnotatedValue<IRI>> getConceptualRelationCategories(
			@Optional @LocallyDefined Resource lexicon) {
		RepositoryConnection con = getManagedConnection();
		Set<IRI> linguisticCatalogs = OntoLexLemon.getLinguisticCatalogs(con, lexicon);
		List<AnnotatedValue<IRI>> categories = new ArrayList<>();

		if (linguisticCatalogs.contains(WN)) {
			QueryBuilder qb = createQueryBuilder(
			//@formatter:off
				"PREFIX wn: <https://globalwordnet.github.io/schemas/wn#>\n" +
				"SELECT ?resource WHERE {\n" + 
				"	?resource a wn:SynsetRelType .\n" + 
				"	FILTER(isIRI(?resource) && STRSTARTS(STR(?resource),\"https://globalwordnet.github.io/schemas/wn#\"))\n" + 
				"}\n" + 
				"GROUP BY ?resource "
				//@formatter:on
			);
			qb.processStandardAttributes();
			qb.runQuery().forEach(r -> categories.add((AnnotatedValue<IRI>) (Object) r));
		}

		return categories;
	}

	/**
	 * Creates a lexico-semantic relation.
	 * 
	 * @param source
	 * @param target
	 * @param undirectional
	 * @param category
	 * @param relationClass
	 * @throws URIGenerationException
	 */
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf', 'W')") // TODO define access control
	public void createLexicoSemanticRelation(Resource source, Resource target, boolean undirectional,
			@Optional IRI category,
			@SubClassOf(superClassIRI = "http://www.w3.org/ns/lemon/vartrans#LexicoSemanticRelation") IRI relationClass,
			@Optional @LocallyDefined Resource translationSet) throws URIGenerationException {
		RepositoryConnection con = getManagedConnection();
		Update update = con.prepareUpdate(
		//@formatter:off
			"PREFIX vartrans: <http://www.w3.org/ns/lemon/vartrans#>\n" + 
			"INSERT {\n" + 
			"	?rel a ?relationClass ;\n" + 
			"		?sourcePred ?source ;\n" + 
			"		?targetPred ?target ;\n" + 
			"		?category vartrans:category ?category .\n" + 
			"		\r\n" + 
			"	?translationSet vartrans:trans ?rel .\n" + 
			"}\n" + 
			"WHERE {\n" + 
			"}"
			//@formatter:on
		);
		update.setBinding("rel", generateIRI("individual", Collections.emptyMap()));
		update.setBinding("relationClass", relationClass);
		update.setBinding("sourcePred", undirectional ? VARTRANS.RELATES : VARTRANS.SOURCE);
		update.setBinding("targetPred", undirectional ? VARTRANS.RELATES : VARTRANS.TARGET);
		if (category != null) {
			update.setBinding("category", category);
		}
		if (translationSet != null) {
			update.setBinding("translationSet", translationSet);
		}

	}

	/**
	 * Generates a new URI for an ontolex:LexicalEntry, optionally given its accompanying canonicalForm and
	 * the lexicon it was attached to. The actual generation of the URI is delegated to
	 * {@link #generateIRI(String, Map)}, which in turn invokes the current binding for the extension point
	 * {@link URIGenerator}. In the end, the <i>URI generator</i> will be provided with the following:
	 * <ul>
	 * <li><code>lexicon</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>canonicalForm</code> and <code>lexicon</code>
	 * (each, if not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param canonicalForm
	 * @param lexicon
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateLexicalEntryURI(Literal canonicalForm, IRI lexicon) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (canonicalForm != null) {
			args.put(URIGenerator.Parameters.canonicalForm, canonicalForm);
		}

		if (lexicon != null) {
			args.put(URIGenerator.Parameters.lexicon, lexicon);
		}
		return generateIRI(URIGenerator.Roles.ontolexLexicalEntry, args);
	}

	/**
	 * Generates a new URI for a lime:Lexicon, optionally given its accompanying title. The actual generation
	 * of the URI is delegated to {@link #generateIRI(String, Map)}, which in turn invokes the current binding
	 * for the extension point {@link URIGenerator}. In the end, the <i>URI generator</i> will be provided
	 * with the following:
	 * <ul>
	 * <li><code>lexicon</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>title</code> (if not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param title
	 *            the title of the lexicon
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateLexiconURI(Literal title) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (title != null) {
			args.put(URIGenerator.Parameters.title, title);
		}
		return generateIRI(URIGenerator.Roles.limeLexicon, args);
	}

	/**
	 * Generates a new URI for an ontolex:Form, given its written representation, the lexical entry it is
	 * attached to and the property used for the binding. The actual generation of the URI is delegated to
	 * {@link #generateIRI(String, Map)}, which in turn invokes the current binding for the extension point
	 * {@link URIGenerator}. In the end, the <i>URI generator</i> will be provided with the following:
	 * <ul>
	 * <li><code>lexicalEntry</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>lexicalEntry</code>, <code>writtenRep</code> and
	 * <code>formProperty</code> (each, if not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param lexicalEntry
	 * @param writtenRep
	 * @param formProperty
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateFormIRI(IRI lexicalEntry, Literal writtenRep, IRI formProperty)
			throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (lexicalEntry != null) {
			args.put(URIGenerator.Parameters.entry, lexicalEntry);
		}
		if (writtenRep != null) {
			args.put(URIGenerator.Parameters.writtenRep, writtenRep);
		}
		if (formProperty != null) {
			args.put(URIGenerator.Parameters.formProperty, formProperty);
		}
		return generateIRI(URIGenerator.Roles.ontolexForm, args);
	}

	/**
	 * Generates a new URI for an ontolex:LexicalSense, given its lexical entry and reference. The actual
	 * generation of the URI is delegated to {@link #generateIRI(String, Map)}, which in turn invokes the
	 * current binding for the extension point {@link URIGenerator}. In the end, the <i>URI generator</i> will
	 * be provided with the following:
	 * <ul>
	 * <li><code>ontolexLexicalSense</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>lexicalEntry</code> and
	 * <code>reference</code></li>
	 * </ul>
	 * 
	 * @param lexicalEntry
	 * @param reference
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateLexicalSenseIRI(Resource lexicalEntry, Resource reference)
			throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (lexicalEntry != null) {
			args.put(URIGenerator.Parameters.entry, lexicalEntry);
		}

		if (reference != null) {
			args.put(URIGenerator.Parameters.lexicalizedResource, reference);
		}

		return generateIRI(URIGenerator.Roles.ontolexLexicalSense, args);
	}

	static protected Set<IRI> getLinguisticCatalogs(RepositoryConnection con, Resource lexicon)
			throws QueryEvaluationException, RepositoryException {
		Set<IRI> linguisticCatalogs = Collections.emptySet();

		if (lexicon != null) {
			linguisticCatalogs = Models
					.objectIRIs(
							QueryResults.asModel(con.getStatements(lexicon, LIME.LINGUISTIC_CATALOG, null)))
					.stream().map(OntologyManager::computeCanonicalURI).collect(Collectors.toSet());
		}

		if (linguisticCatalogs.isEmpty()) {
			linguisticCatalogs = Collections.singleton(OntoLexLemon.LEXINFO);
		}
		return linguisticCatalogs;
	}

	private static final Pattern rdfN_uriPattern = Pattern
			.compile("^http://www\\.w3\\.org/1999/02/22-rdf-syntax-ns#_\\d+$");

	public static <T> boolean sortConstituents(List<T> values, Function<T, Resource> valueExtractor,
			Model statements, Resource subjectResource) {
		boolean allOrdered = true;

		Map<Value, Long> value2Position = new LinkedHashMap<>();

		for (Value value : values.stream().map(valueExtractor).collect(Collectors.toList())) {

			/*
			 * if a value is bound to different rdf:_n, then reduce those value to just one by considering the
			 * following priority: add graph > remove graph > other graph. If there are multiple positions
			 * with the same priority, just pick one arbitrarily.
			 */
			java.util.Optional<Long> positionHolder = statements.filter(subjectResource, null, value).stream()
					.filter(stmt -> rdfN_uriPattern.matcher(stmt.getPredicate().stringValue()).matches())
					.reduce((s1, s2) -> {
						Resource c1 = s1.getContext();
						Resource c2 = s2.getContext();

						if (VALIDATION.isAddGraph(c2)) {
							return s2;
						} else if (VALIDATION.isRemoveGraph(c2) && !VALIDATION.isAddGraph(c1)) {
							return s2;
						} else {
							return s1;
						}
					}).map(stmt -> (Long) Long.parseLong(stmt.getPredicate().stringValue()
							.substring("http://www.w3.org/1999/02/22-rdf-syntax-ns#_".length())));

			if (positionHolder.isPresent()) {
				value2Position.put(value, positionHolder.get());
			} else {
				allOrdered = false;
			}
		}

		if (allOrdered) {
			values.sort((a1, a2) -> java.util.Objects.compare(value2Position.get(valueExtractor.apply(a1)),
					value2Position.get(valueExtractor.apply(a2)), Comparator.naturalOrder()));
			return true;
		} else {
			return false;
		}
	}

}