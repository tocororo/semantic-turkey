package it.uniroma2.art.semanticturkey.services.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This class provides services for manipulating SKOSXL constructs.
 * 
 * TODO: here there are: generateConceptIRI, generateConceptSchemeIRI, generateCollectionIRI
 * that are implemented also in SKOS service class.
 * Whould be better move this method in a STSKOSServiceAdapter (that extends STServiceAdapter)? 
 * 
 * @author Tiziano
 *
 */
@STService
public class SKOSXL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SKOSXL.class);
	
	@Autowired
	private CustomFormManager cfManager;
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public AnnotatedValue<IRI> createConcept(
			@Optional @NotLocallyDefined IRI newConcept, @Optional @LanguageTaggedString Literal label,
			@Optional @LocallyDefined @Selection Resource broaderConcept, @LocallyDefined IRI conceptScheme,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		IRI newConceptIRI;
		if (newConcept == null) {
			newConceptIRI = generateConceptIRI(label, conceptScheme);
		} else {
			newConceptIRI = newConcept;
		}
		modelAdditions.add(newConceptIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT);
		
		IRI xLabelIRI = null;
		if (label != null) {
			xLabelIRI = generateXLabelIRI(newConceptIRI, label, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xLabelIRI);
			modelAdditions.add(xLabelIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL);
			modelAdditions.add(xLabelIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, label);
		}
		modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);
		if (broaderConcept != null) {
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else {
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
		}

		RepositoryConnection repoConnection = getManagedConnection();
		
		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newConceptIRI.stringValue());
			if (xLabelIRI != null) {
				stdForm.addFormEntry(StandardForm.Prompt.xLabel, xLabelIRI.stringValue());
				stdForm.addFormEntry(StandardForm.Prompt.lexicalForm, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, customFormId, userPromptMap, stdForm);
		}
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newConceptIRI);
		annotatedValue.setAttribute("role", RDFResourceRolesEnum.concept.name());
		//TODO compute show
		return annotatedValue; 
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public AnnotatedValue<IRI> createConceptScheme(
			@Optional @NotLocallyDefined IRI newScheme, @Optional @LanguageTaggedString Literal label,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		IRI newSchemeIRI;
		if (newScheme == null) {
			newSchemeIRI = generateConceptSchemeURI(label);
		} else {
			newSchemeIRI = newScheme;
		}
		modelAdditions.add(newSchemeIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT_SCHEME);
		
		IRI xLabelIRI = null;
		if (label != null) {
			xLabelIRI = generateXLabelIRI(newSchemeIRI, label, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			modelAdditions.add(newSchemeIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xLabelIRI);
			modelAdditions.add(xLabelIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL);
			modelAdditions.add(xLabelIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, label);
		}

		RepositoryConnection repoConnection = getManagedConnection();

		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newSchemeIRI.stringValue());
			if (xLabelIRI != null) {
				stdForm.addFormEntry(StandardForm.Prompt.xLabel, xLabelIRI.stringValue());
				stdForm.addFormEntry(StandardForm.Prompt.lexicalForm, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, customFormId, userPromptMap, stdForm);
		}
				
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newSchemeIRI);
		annotatedValue.setAttribute("role", RDFResourceRolesEnum.conceptScheme.name());
		//TODO compute show
		return annotatedValue; 
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public AnnotatedValue<Resource> createCollection(
			IRI collectionType, @Optional @NotLocallyDefined IRI newCollection, 
			@Optional @LanguageTaggedString Literal label, @Optional @LocallyDefined IRI containingCollection,
			@Optional(defaultValue = "false") boolean bnodeCreationMode,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException, IllegalAccessException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		Resource newCollectionRes;
		if (newCollection == null) {
			if (bnodeCreationMode) {
				newCollectionRes = SimpleValueFactory.getInstance().createBNode();
			} else { //uri
				newCollectionRes = generateCollectionURI(label);
			}
		} else {
			newCollectionRes = newCollection;
		}
		if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
			modelAdditions.add(newCollectionRes, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION);
		} else if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION)){
			modelAdditions.add(newCollectionRes, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION);
			modelAdditions.add(newCollectionRes, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, RDF.NIL);
		} else {
			throw new IllegalAccessException(collectionType.stringValue() + " is not a valid collection type");
		}
		
		IRI xLabelIRI = null;
		if (label != null) {
			xLabelIRI = generateXLabelIRI(newCollectionRes, label, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			modelAdditions.add(newCollectionRes, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xLabelIRI);
			modelAdditions.add(xLabelIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL);
			modelAdditions.add(xLabelIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, label);
		}

		RepositoryConnection repoConnection = getManagedConnection();
		
		if (containingCollection != null) {
			if (repoConnection.hasStatement(containingCollection, RDF.TYPE, 
					org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION, false, getWorkingGraph())) {
				//TODO add newCollection as last of containingCollection
				
			} else if (repoConnection.hasStatement(containingCollection, RDF.TYPE,
					org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION, false, getWorkingGraph())) {
				modelAdditions.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER, newCollectionRes);
			}
		}
		
		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newCollectionRes.stringValue());
			if (xLabelIRI != null) {
				stdForm.addFormEntry(StandardForm.Prompt.xLabel, xLabelIRI.stringValue());
				stdForm.addFormEntry(StandardForm.Prompt.lexicalForm, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, customFormId, userPromptMap, stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(newCollectionRes);
		if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.skosCollection.name());
		} else { //ORDERED
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.skosOrderedCollection.name());
		}
		//TODO compute show
		return annotatedValue; 
	}
	
	/**
	 * Generates a new URI for a SKOS concept, optionally given its accompanying preferred label and concept
	 * scheme. The actual generation of the URI is delegated to {@link #generateURI(String, Map)}, which in
	 * turn invokes the current binding for the extension point {@link URIGenerator}. In the end, the <i>URI
	 * generator</i> will be provided with the following:
	 * <ul>
	 * <li><code>concept</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>label</code> and <code>scheme</code> (each, if
	 * not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param label
	 *            the preferred label accompanying the concept (can be <code>null</code>)
	 * @param scheme
	 *            the scheme to which the concept is being attached at the moment of its creation (can be
	 *            <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptIRI(Literal label, IRI scheme) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}

		if (scheme != null) {
			args.put(URIGenerator.Parameters.scheme, scheme);
		}

		return generateIRI(URIGenerator.Roles.concept, args);
	}
	
	/**
	 * Generates a new URI for a SKOS concept scheme, optionally given its accompanying preferred label.
	 * 
	 * @param label
	 *            the preferred label accompanying the concept scheme (can be <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptSchemeURI(Literal label) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}
		return generateIRI(URIGenerator.Roles.conceptScheme, args);
	}
	
	/**
	 * Generates a new URI for a SKOS collection, optionally given its accompanying preferred label.
	 * 
	 * @param label
	 *            the preferred label accompanying the collection (can be <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateCollectionURI(Literal label) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}
		return generateIRI(URIGenerator.Roles.skosCollection, args);
	}
	
	/**
	 * Generates a new URI for a SKOSXL Label, based on the provided mandatory parameters. The actual
	 * generation of the URI is delegated to {@link #generateURI(String, Map)}, which in turn invokes the
	 * current binding for the extension point {@link URIGenerator}. In the end, the <i>URI generator</i> will
	 * be provided with the following:
	 * <ul>
	 * <li><code>xLabel</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>lexicalForm</code>,
	 * <code>lexicalizedResource</code> and <code>type</code> (each, if not <code>null</code>)</li>
	 * </ul>
	 * 
	 * All arguments should be not <code>null</code>, but in the end is the specific implementation of the
	 * extension point that would complain about the absence of one of these theoretically mandatory
	 * parameters.
	 * 
	 * @param lexicalForm
	 *            the textual content of the label
	 * @param lexicalizedResource
	 *            the resource to which the label will be attached to
	 * @param type
	 *            the property used for attaching the label
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateXLabelIRI(Resource lexicalizedResource, Literal lexicalForm, IRI type) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		
		if (lexicalizedResource != null) {
			args.put(URIGenerator.Parameters.lexicalizedResource, lexicalizedResource);
		}

		if (lexicalForm != null) {
			args.put(URIGenerator.Parameters.lexicalForm, lexicalForm);
		}
		
		if (type != null) {
			args.put(URIGenerator.Parameters.type, type);
		}
		
		return generateIRI(URIGenerator.Roles.xLabel, args);
	}
	
	/**
	 * TODO: move to STServiceAdapter?
	 * 
	 * Enrich the <code>modelAdditions</code> and <code>modelAdditions</code> with the triples to add and remove
	 * suggested by CODA running the PEARL rule defined in the CustomForm with the given <code>cfId</code>  
	 */
	private void enrichWithCustomForm(RepositoryConnection repoConn, Model modelAdditions, Model modelRemovals,
			String cfId, Map<String, Object> userPromptMap, StandardForm stdForm)
			throws ProjectInconsistentException, CODAException, CustomFormException {
		CODACore codaCore = getInitializedCodaCore(repoConn);
		try {

			CustomForm cForm = cfManager.getCustomForm(getProject(), cfId);
			if (cForm.isTypeGraph()) {
				CustomFormGraph cfGraph = cForm.asCustomFormGraph();
				UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm);
				shutDownCodaCore(codaCore);

				for (ARTTriple t : updates.getInsertTriples()) {
					modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
				for (ARTTriple t : updates.getDeleteTriples()) {
					modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
			} else {
				throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
						+ "' as constructor since it is not of type 'graph'");
			}
		} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e) {
			throw new CODAException(e);
		} finally {
			shutDownCodaCore(codaCore);
		}
	}
	
}