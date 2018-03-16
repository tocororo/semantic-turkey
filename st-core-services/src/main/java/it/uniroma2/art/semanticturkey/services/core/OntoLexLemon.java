package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfoUtils;
import it.uniroma2.art.semanticturkey.rendering.AbstractLabelBasedRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.versioning.VersioningMetadataSupport;

/**
 * This class provides services for manipulating data based on the W3C OntoLex-Lemon Model.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class OntoLexLemon extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(OntoLexLemon.class);

	@Autowired
	private CustomFormManager cfManager;

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
	 * @throws ProjectInconsistentException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(lexicon)', 'C')")
	public AnnotatedValue<IRI> createLexicon(@Optional @NotLocallyDefined IRI newLexicon, String language,
			@Optional @LanguageTaggedString Literal title, @Optional CustomFormValue customFormValue)
			throws ProjectInconsistentException, CODAException, CustomFormException, URIGenerationException {

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newLexiconIRI;

		if (newLexicon == null) {
			newLexiconIRI = generateLexiconURI(title);
		} else {
			newLexiconIRI = newLexicon;
		}

		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newLexiconIRI,
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
	@PreAuthorize("@auth.isAuthorized('rdf(lexicon)', 'R')")
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
		qb.process(LexiconRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}

	/* --- Lexical entries --- */

	/**
	 * Creates a new ontolex:LexicalEntry.
	 * 
	 * @param newLexicalEntry
	 * @param canonicalForm
	 * @param lexicon
	 * @param customFormValue
	 * @return
	 * @throws ProjectInconsistentException
	 * @throws CODAException
	 * @throws CustomFormException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(lexicalEntry)', 'C')")
	public AnnotatedValue<IRI> createLexicalEntry(@Optional @NotLocallyDefined IRI newLexicalEntry,
			@Optional(defaultValue = "http://www.w3.org/ns/lemon/ontolex#LexicalEntry") @SubClassOf(superClassIRI = "http://www.w3.org/ns/lemon/ontolex#LexicalEntry") IRI lexicalEntryCls,
			@LanguageTaggedString Literal canonicalForm, @LocallyDefined @Modified IRI lexicon,
			@Optional CustomFormValue customFormValue)
			throws ProjectInconsistentException, CODAException, CustomFormException, URIGenerationException {

		RepositoryConnection repConn = getManagedConnection();

		if (!isLanguageComaptibleWithLexiconMetadata(repConn,
				canonicalForm.getLanguage().orElseThrow(
						() -> new IllegalArgumentException("Missing language tag in canonical form")),
				lexicon)) {
			throw new IllegalArgumentException(
					"The canonical form is expressed in a natural language not compatible with the lexicon");
		}

		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newLexicalEntryIRI;
		if (newLexicalEntry == null) {
			newLexicalEntryIRI = generateLexicalEntryURI(canonicalForm, lexicon);
		} else {
			newLexicalEntryIRI = newLexicalEntry;
		}

		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newLexicalEntryIRI,
				RDFResourceRole.ontolexLexicalEntry); // set created for versioning

		IRI canonicalFormIRI = generateFormIRI(newLexicalEntryIRI, canonicalForm, ONTOLEX.CANONICAL_FORM);

		modelAdditions.add(newLexicalEntryIRI, RDF.TYPE, lexicalEntryCls);
		modelAdditions.add(newLexicalEntryIRI, ONTOLEX.CANONICAL_FORM, canonicalFormIRI);
		modelAdditions.add(lexicon, LIME.ENTRY, newLexicalEntryIRI);

		modelAdditions.add(canonicalFormIRI, RDF.TYPE, ONTOLEX.FORM);
		modelAdditions.add(canonicalFormIRI, ONTOLEX.WRITTEN_REP, canonicalForm);

		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(canonicalFormIRI,
				RDFResourceRole.ontolexForm); // set created for versioning

		// enrich with custom form
		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newLexicalEntryIRI.stringValue());

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
	 * Returns the entries in a given lexicon that starts with the supplied character.
	 * 
	 * @param index
	 * @param lexicon
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(lexicalEntry)', 'R')")
	public Collection<AnnotatedValue<Resource>> getLexicalEntriesByAlphabeticIndex(Character index,
			IRI lexicon) {
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
			"   ?lexicon lime:entry ?resource .                                                 \n" +
			"   FILTER EXISTS {                                                                 \n" +
			"     ?resource ontolex:canonicalForm [                                             \n" +
			"       ontolex:writtenRep ?cf                                                      \n" +
			"     ]                                                                             \n" +
			"     .                                                                             \n" +
			"     FILTER(REGEX(STR(?cf), \"^" + index + "\", \"i\"))                            \n" +
			"   }                                                                               \n" +
			generateNatureSPARQLWherePart("?resource") +
			" }                                                                                 \n" +
			" GROUP BY ?resource                                                                \n"
			// @formatter:on
		);
		qb.setBinding("lexicon", lexicon);
		qb.processQName();
		qb.process(LexicalEntryRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}

	/**
	 * Generates a new URI for an ontolex:LexicalEntry, optionally given its accompanying canonicalForm and
	 * the lexicon it was attached to. The actual generation of the URI is delegated to
	 * {@link #generateURI(String, Map)}, which in turn invokes the current binding for the extension point
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
	 * of the URI is delegated to {@link #generateURI(String, Map)}, which in turn invokes the current binding
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
	 * {@link #generateURI(String, Map)}, which in turn invokes the current binding for the extension point
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

}

class LexiconRenderer extends BaseRenderingEngine {

	private static AbstractLabelBasedRenderingEngineConfiguration conf;

	static {
		conf = new AbstractLabelBasedRenderingEngineConfiguration() {

			@Override
			public String getShortName() {
				return "foo";
			}

		};
	}

	private LexiconRenderer() {
		super(conf);
	}

	public static final LexiconRenderer INSTANCE = new LexiconRenderer();

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
				"?resource <http://purl.org/dc/terms/title> ?labelInternal .\n");
	}

}
class LexicalEntryRenderer extends BaseRenderingEngine {

	private static AbstractLabelBasedRenderingEngineConfiguration conf;

	static {
		conf = new AbstractLabelBasedRenderingEngineConfiguration() {

			@Override
			public String getShortName() {
				return "foo";
			}

		};
		conf.languages = null;
	}

	private LexicalEntryRenderer() {
		super(conf);
	}

	public static final LexicalEntryRenderer INSTANCE = new LexicalEntryRenderer();

	@Override
	protected void getGraphPatternInternal(StringBuilder gp) {
		gp.append(
				"?resource <http://www.w3.org/ns/lemon/ontolex#canonicalForm> [<http://www.w3.org/ns/lemon/ontolex#writtenRep> ?labelInternal ] .\n");
	}

}