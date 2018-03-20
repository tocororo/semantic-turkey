package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryResults;
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
import it.uniroma2.art.semanticturkey.constraints.SubPropertyOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexiconRenderer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
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
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'C')")
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
		qb.process(LexiconRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}

	/**
	 * Deletes a lexicon.
	 * 
	 * @param lexicon
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'D')")
	public void deleteLexicon(@LocallyDefined IRI lexicon) {
		throw new RuntimeException("To be implemented");
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
	 * @throws ProjectInconsistentException
	 * @throws CODAException
	 * @throws CustomFormException
	 * @throws URIGenerationException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'C')")
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
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'R')")
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
	 * Sets the canonical form of a given lexical entry.
	 * 
	 * @param lexicalEntry
	 * @param newForm
	 * @param writtenRep
	 * @param customFormValue
	 * @throws URIGenerationException
	 * @throws ProjectInconsistentException
	 * @throws CODAException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'U')")
	public void setCanonicalForm(@Modified @LocallyDefined IRI lexicalEntry,
			@Optional @NotLocallyDefined IRI newForm, @LanguageTaggedString Literal writtenRep,
			@Optional CustomFormValue customFormValue)
			throws URIGenerationException, ProjectInconsistentException, CODAException, CustomFormException {
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
	 * @throws ProjectInconsistentException
	 * @throws CODAException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'U')")
	public void addOtherForm(@Modified @LocallyDefined IRI lexicalEntry,
			@Optional @NotLocallyDefined IRI newForm, @LanguageTaggedString Literal writtenRep,
			@Optional CustomFormValue customFormValue)
			throws URIGenerationException, ProjectInconsistentException, CODAException, CustomFormException {
		addFormInternal(newForm, writtenRep, lexicalEntry, customFormValue, ONTOLEX.OTHER_FORM, false);
	}

	// Annotations are not interpreted on non-service methods. Left for documentation purpose.
	protected void addFormInternal(@Optional @NotLocallyDefined IRI newForm,
			@LanguageTaggedString Literal writtenRep, @LocallyDefined IRI lexicalEntry,
			@Optional CustomFormValue customFormValue, IRI property, boolean replaces)
			throws URIGenerationException, ProjectInconsistentException, CODAException, CustomFormException {
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		IRI newFormIRI;

		if (newForm == null) {
			newFormIRI = generateFormIRI(lexicalEntry, writtenRep, property);
		} else {
			newFormIRI = newForm;
		}

		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newFormIRI,
				RDFResourceRole.ontolexForm); // set created for versioning

		modelAdditions.add(newFormIRI, RDF.TYPE, ONTOLEX.FORM);
		modelAdditions.add(newFormIRI, ONTOLEX.WRITTEN_REP, writtenRep);

		modelAdditions.add(lexicalEntry, property, newFormIRI);

		VersioningMetadataSupport.currentVersioningMetadata().addModifiedResource(lexicalEntry,
				RDFResourceRole.ontolexLexicalEntry);

		RepositoryConnection repoConnection = getManagedConnection();

		if (replaces) {
			Model previousFormStatements = QueryResults
					.asModel(repoConnection.getStatements(lexicalEntry, property, null, getWorkingGraph()));

			modelRemovals.addAll(previousFormStatements);

			Set<Resource> deletedForms = previousFormStatements.objects().stream()
					.filter(Resource.class::isInstance).map(Resource.class::cast).collect(toSet());

			deletedForms.stream().filter(Resource.class::isInstance).map(form -> {
				Model removedStatements = new LinkedHashModel();
				removedStatements.addAll(QueryResults.asModel(
						repoConnection.getStatements((Resource) form, null, null, false, getWorkingGraph())));
				removedStatements.addAll(QueryResults.asModel(
						repoConnection.getStatements(null, null, (Resource) form, false, getWorkingGraph())));

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
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'U')")
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
	 * Deletes a lexical entry.
	 * 
	 * @param lexicalEntry
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(ontolexLexicalEntry)', 'D')")
	public void deleteLexicalEntry(@LocallyDefined IRI lexicalEntry) {
		throw new RuntimeException("To be implemented");
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