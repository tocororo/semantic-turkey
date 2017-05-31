package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.history.HistoryMetadataSupport;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Subject;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.utilities.TurtleHelp;
import it.uniroma2.art.semanticturkey.versioning.VersioningMetadataSupport;

/**
 * This class provides services for manipulating SKOSXL constructs.
 * 
 * TODO: here there are: generateConceptIRI, generateConceptSchemeIRI, generateCollectionIRI
 * that are implemented also in SKOS service class.
 * Whould be better move this method in a STSKOSServiceAdapter (that extends STServiceAdapter)? 
 * 
 * @author Tiziano
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
@STService
public class SKOSXL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SKOSXL.class);
	
	public static enum XLabelCreationMode {
		bnode, uri
	};
	
	@Autowired
	private CustomFormManager cfManager;
	
	/**
	 * @param newConcept
	 * 		IRI of the new created concept. If not provided a random IRI is generated.
	 * @param label
	 * 		preferred label of the concept
	 * @param broaderConcept
	 * 		broader of the new created concept. If not provided the new concept will be a top concept
	 * @param conceptScheme
	 * 		concept scheme where the concept belongs
	 * @param conceptCls
	 * 		class type of the new created concept. It must be a subClassOf skos:Concept. If not provided the new concept
	 * 		will be simply a skos:Concept
	 * @param customFormId
	 * 		id of the custom form to use to add additional info to the concept
	 * @param userPromptMap
	 * 		(json) map of userPrompt field to use with the custom form
	 * @return
	 * @throws URIGenerationException
	 * @throws ProjectInconsistentException
	 * @throws CustomFormException
	 * @throws CODAException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(concept)', 'C')")
	public AnnotatedValue<IRI> createConcept(
			@Optional @NotLocallyDefined IRI newConcept, @Optional @LanguageTaggedString Literal label,
			@Optional @LocallyDefined @Selection Resource broaderConcept, @LocallyDefinedResources List<IRI> conceptSchemes,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2004/02/skos/core#Concept") IRI conceptCls,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException {
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		IRI newConceptIRI;
		if (newConcept == null) {
			newConceptIRI = generateConceptIRI(label, conceptSchemes);
		} else {
			newConceptIRI = newConcept;
		}
		
		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newConceptIRI); // set created for versioning
		HistoryMetadataSupport.currentOperationMetadata().setSubject(newConceptIRI); //set subject for history
		
		IRI conceptClass = org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT;
		if (conceptCls != null) {
			conceptClass = conceptCls;
		}
		
		modelAdditions.add(newConceptIRI, RDF.TYPE, conceptClass);
		
		IRI xLabelIRI = null;
		if (label != null) {
			xLabelIRI = generateXLabelIRI(newConceptIRI, label, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xLabelIRI);
			modelAdditions.add(xLabelIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL);
			modelAdditions.add(xLabelIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, label);
		}
		for(IRI conceptScheme : conceptSchemes){
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);
		}
		if (broaderConcept != null) {
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else {
			for(IRI conceptScheme : conceptSchemes){
				modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
			}
		}

		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newConceptIRI.stringValue());
			if (xLabelIRI != null) {
				stdForm.addFormEntry(StandardForm.Prompt.xLabel, xLabelIRI.stringValue());
				stdForm.addFormEntry(StandardForm.Prompt.lexicalForm, label.getLabel());
				stdForm.addFormEntry(StandardForm.Prompt.labelLang, label.getLanguage().orElse(null));
			}
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm, userPromptMap, stdForm);
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
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'C')")
	public AnnotatedValue<IRI> createConceptScheme(
			@Optional @NotLocallyDefined IRI newScheme, @Optional @LanguageTaggedString Literal label,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2004/02/skos/core#ConceptScheme") IRI schemeCls,
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
		
		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newSchemeIRI); // set created for versioning
		HistoryMetadataSupport.currentOperationMetadata().setSubject(newSchemeIRI); //set subject for history
		
		IRI schemeClass = org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT_SCHEME;
		if (schemeCls != null) {
			schemeClass = schemeCls;
		}
		
		modelAdditions.add(newSchemeIRI, RDF.TYPE, schemeClass);
		
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
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm, userPromptMap, stdForm);
		}
				
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newSchemeIRI);
		annotatedValue.setAttribute("role", RDFResourceRolesEnum.conceptScheme.name());
		//TODO compute show
		return annotatedValue; 
	}
	
	/**
	 * Deletes a conceptScheme and the related xLabels
	 * @param scheme
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'D')")
	public void deleteConceptScheme(@LocallyDefined @Subject IRI scheme) {
		String query = "DELETE {														\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		?s1 ?p1 ?scheme .												\n"
				+ "		?scheme ?p2 ?o2 .												\n"
				+ "		?o2 ?p3 ?o3 .													\n"
				+ "		?s4 ?p4 ?o2 .													\n"
				+ "	}																	\n"
				+ "} WHERE {															\n"
				+ "	BIND(URI('" + scheme.stringValue() + "') AS ?scheme)				\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		{ 																\n"
				+ "			?s1 ?p1 ?scheme . 											\n"
				+ "		} UNION {														\n"
				+ "			?scheme ?p2 ?o2 . 											\n"
				+ "			OPTIONAL {													\n"
				+ "				FILTER(?p2 = 											\n"
					+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL) + " || " 
					+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL) + " || "
					+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL) + ")"
				+ "				?o2 ?p3 ?o3 .											\n"
				+ "				?s4 ?p4 ?o2 .											\n"
				+ "			}															\n"
				+ "		}																\n"
				+ "	}																	\n"
				+ "}";
		System.out.println(query);
		RepositoryConnection repoConnection = getManagedConnection();
		Update uq = repoConnection.prepareUpdate(query);
		uq.execute();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'C')")
	public AnnotatedValue<Resource> createCollection(
			IRI collectionType, @Optional @NotLocallyDefined IRI newCollection, 
			@Optional @LanguageTaggedString Literal label, @Optional @LocallyDefined IRI containingCollection,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2004/02/skos/core#Collection") IRI collectionCls,
			@Optional(defaultValue = "false") boolean bnodeCreationMode,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException, IllegalAccessException {
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		
		Resource newCollectionRes;
		if (newCollection == null) {
			if (bnodeCreationMode) {
				newCollectionRes = vf.createBNode();
			} else { //uri
				newCollectionRes = generateCollectionURI(label);
			}
		} else {
			newCollectionRes = newCollection;
		}
		
		VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(newCollectionRes); // set created for versioning
		HistoryMetadataSupport.currentOperationMetadata().setSubject(newCollectionRes); //set subject for history
		
		IRI collectionClass = collectionType;
		if (collectionCls != null) {
			/* check consistency between collection type an class: @SubClassOf just check that collectionCls is 
			 * subClassOf Collection, but if collectionCls is subClassOf OrderedCollection and collectionType
			 * is collection throw exception */
			if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
				String query = "ASK { "
						+ NTriplesUtil.toNTriplesString(collectionCls) + " " 
						+ NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* "
						+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION) + " }";
				boolean inconsistent = repoConnection.prepareBooleanQuery(query).evaluate();
				if (inconsistent) {
					throw new IllegalArgumentException("Inconsistent collection type: cannot create a collection (not-ordered)"
							+ " of type " + collectionCls.stringValue() + " that is an ordered collection instead");
				}
			}
			collectionClass = collectionCls;
		}
		
		if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.COLLECTION)) {
			modelAdditions.add(newCollectionRes, RDF.TYPE, collectionClass);
		} else if (collectionType.equals(org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION)){
			modelAdditions.add(newCollectionRes, RDF.TYPE, collectionClass);
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

		if (containingCollection != null) {
			if (repoConnection.hasStatement(containingCollection, RDF.TYPE, 
					org.eclipse.rdf4j.model.vocabulary.SKOS.ORDERED_COLLECTION, false, getWorkingGraph())) {
				
				//add newCollection as last of containingCollection (inspired from SKOSModelImpl.addLastToSKOSOrderedCollection())
				Resource memberList = null;
				RepositoryResult<Statement> res = repoConnection.getStatements(
						containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, null, false, getWorkingGraph());
				if (res.hasNext()) {
					memberList = (Resource) res.next().getObject();//it's a resource for sure since the predicate is skos:memberList
				}
				if (memberList == null) {
					BNode newNode = vf.createBNode();
					modelAdditions.add(newNode, RDF.TYPE, RDF.LIST);
					modelAdditions.add(newNode, RDF.FIRST, newCollectionRes);
					modelAdditions.add(newNode, RDF.REST, RDF.NIL);
					modelAdditions.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, newNode);
				} else {
					BNode newNode = vf.createBNode();
					modelAdditions.add(newNode, RDF.TYPE, RDF.LIST);
					modelAdditions.add(newNode, RDF.FIRST, newCollectionRes);
					modelAdditions.add(newNode, RDF.REST, RDF.NIL);
					
					if (memberList.equals(RDF.NIL)) {
						modelRemovals.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, memberList);
						modelAdditions.add(containingCollection, org.eclipse.rdf4j.model.vocabulary.SKOS.MEMBER_LIST, newNode);
					} else {
						//get last node of the list
						Resource lastNode = walkMemberList(repoConnection, memberList);
						modelRemovals.add(lastNode, RDF.REST, RDF.NIL);
						modelAdditions.add(lastNode, RDF.REST, newNode);
					}
				}
				
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
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, cForm, userPromptMap, stdForm);
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
	 * @param schemes
	 *            the schemes to which the concept is being attached at the moment of its creation (can be
	 *            <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptIRI(Literal label, List<IRI> schemes) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}

		if (schemes != null) {
			args.put(URIGenerator.Parameters.schemes,
					SimpleValueFactory.getInstance().createLiteral(TurtleHelp.serializeCollection(schemes)));
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
	 * @param lexicalizationProperty
	 *            the property used for attaching the label
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateXLabelIRI(Resource lexicalizedResource, Literal lexicalForm,
			IRI lexicalizationProperty) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (lexicalizedResource != null) {
			args.put(URIGenerator.Parameters.lexicalizedResource, lexicalizedResource);
		}

		if (lexicalForm != null) {
			args.put(URIGenerator.Parameters.lexicalForm, lexicalForm);
		}

		if (lexicalizationProperty != null) {
			args.put(URIGenerator.Parameters.lexicalizationProperty, lexicalizationProperty);
		}

		return generateIRI(URIGenerator.Roles.xLabel, args);
	}
	
	
	@STServiceOperation
	@Read
	// TODO @PreAuthorize
	public Collection<AnnotatedValue<Resource>> getPrefLabel(@LocallyDefined IRI concept, @Optional(defaultValue="*") String lang){
		Collection<AnnotatedValue<Resource>> resultsList = new ArrayList<>();
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?prefLabel ?label" +
				"\nWHERE{" + 
				"?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?prefLabel ." +
				"?prefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			BindingSet bindingSet = queryResult.next();
			Resource prefLabel = (Resource) bindingSet.getBinding("prefLabel").getValue();
			Literal label = (Literal) bindingSet.getBinding("label").getValue();
			if(lang.equals("*") || (label.getLanguage().isPresent() && label.getLanguage().get().equals(lang))){
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(prefLabel);
				annotatedValue.setAttribute("role", RDFResourceRolesEnum.xLabel.name());
				if(label.getLanguage().isPresent()){
					annotatedValue.setAttribute("lang", label.getLanguage().get());
				}
				annotatedValue.setAttribute("show", label.getLabel());
				resultsList.add(annotatedValue);
			}
		}
		return resultsList;
	}
	
	@STServiceOperation
	@Read
	// TODO @PreAuthorize
	public Collection<AnnotatedValue<Resource>> getAltLabels(@LocallyDefined IRI concept, @Optional(defaultValue="*") String lang){
		Collection<AnnotatedValue<Resource>> resultsList = new ArrayList<>();
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?prefLabel ?label" +
				"\nWHERE{" + 
				"?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" ?prefLabel ." +
				"?prefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			BindingSet bindingSet = queryResult.next();
			Resource prefLabel = (Resource) bindingSet.getBinding("prefLabel").getValue();
			Literal label = (Literal) bindingSet.getBinding("label").getValue();
			if(lang.equals("*") || (label.getLanguage().isPresent() && label.getLanguage().get().equals(lang))){
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(prefLabel);
				annotatedValue.setAttribute("role", RDFResourceRolesEnum.xLabel.name());
				if(label.getLanguage().isPresent()){
					annotatedValue.setAttribute("lang", label.getLanguage().get());
				}
				annotatedValue.setAttribute("show", label.getLabel());
				resultsList.add(annotatedValue);
			}
		}
		return resultsList;
	}
	
	@STServiceOperation
	@Read
	// TODO @PreAuthorize
	public Collection<AnnotatedValue<Resource>> getHiddenLabels(@LocallyDefined IRI concept, @Optional(defaultValue="*") String lang){
		Collection<AnnotatedValue<Resource>> resultsList = new ArrayList<>();
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?prefLabel ?label" +
				"\nWHERE{" + 
				"?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL)+" ?prefLabel ." +
				"?prefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			BindingSet bindingSet = queryResult.next();
			Resource prefLabel = (Resource) bindingSet.getBinding("prefLabel").getValue();
			Literal label = (Literal) bindingSet.getBinding("label").getValue();
			if(lang.equals("*") || (label.getLanguage().isPresent() && label.getLanguage().get().equals(lang))){
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(prefLabel);
				annotatedValue.setAttribute("role", RDFResourceRolesEnum.xLabel.name());
				if(label.getLanguage().isPresent()){
					annotatedValue.setAttribute("lang", label.getLanguage().get());
				}
				annotatedValue.setAttribute("show", label.getLabel());
				resultsList.add(annotatedValue);
			}
		}
		return resultsList;
	}
	
	/**
	 * this service adds an alternative label to a concept 
	 * 
	 * 
	 * @param concept
	 * @param literal
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @return
	 * @throws URIGenerationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void addAltLabel(@LocallyDefined @Subject IRI concept, Literal literal, XLabelCreationMode mode) 
			throws URIGenerationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		Resource xlabel;
		if (mode == XLabelCreationMode.bnode) {
			xlabel = repoConnection.getValueFactory().createBNode();
		} else{
			xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL);
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, literal));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	/**
	 * this service adds an hidden label to a concept 
	 * 
	 * 
	 * @param concept
	 * @param literal
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @return
	 * @throws URIGenerationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void addHiddenLabel(@LocallyDefined @Subject IRI concept, @LanguageTaggedString Literal literal, 
			XLabelCreationMode mode) throws URIGenerationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		Resource xlabel;
		if (mode == XLabelCreationMode.bnode) {
			xlabel = repoConnection.getValueFactory().createBNode();
		} else{
			xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL);
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, literal));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void prefToAtlLabel(@LocallyDefined @Subject IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, xlabel));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void setPrefLabel(@LocallyDefined @Subject IRI concept, @LanguageTaggedString Literal literal,
			XLabelCreationMode mode) throws URIGenerationException{
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		String lang = literal.getLanguage().get();
		
		//check if there is already a prefLabel for the given language (take it form the input literal)
		// @formatter:off
		String query = "select ?oldPrefLabel " +
					"\nWHERE{" +
					"\n?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?oldPrefLabel ."+
					"\n?oldPrefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLabel ."+
					"\nFILTER(lang(?oldLabel) = \""+lang+"\")"+
					"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
			if(tupleQueryResult.hasNext()){
				//if there is already a prefLabel, change it to altLabel
				Resource oldPrefLabel = (Resource) tupleQueryResult.next().getValue("oldPrefLabel");
				modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, oldPrefLabel));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, oldPrefLabel));
			}
			
			Resource xlabel;
			if (mode == XLabelCreationMode.bnode) {
				xlabel = repoConnection.getValueFactory().createBNode();
			} else{
				xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xlabel));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, literal));
		}
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void removePrefLabel(@LocallyDefined @Subject IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "DELETE {" +
				"\nGRAPH ?g {?xlabel ?p1 ?o1 . }"+
				"\nGRAPH ?g {?s2 ?p2 ?xlabel . }"+
				"\n}" + 
				"\nWHERE{" +
				"GRAPH ?g {?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?xlabel .}" +
				
				"{GRAPH ?g {?xlabel ?p1 ?o1 .}}" +
				"\nUNION" +
				"{GRAPH ?g {?s2 ?p2 ?xlabel  .}}" +
				
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("concept", concept);
		update.setBinding("xlabel", xlabel);
		update.setBinding("g", getWorkingGraph());
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void removeAltLabel(@LocallyDefined @Subject IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "DELETE {" +
				"\nGRAPH ?g {?xlabel ?p1 ?o1 . }"+
				"\nGRAPH ?g {?s2 ?p2 ?xlabel . }"+
				"\n}" + 
				"\nWHERE{" +
				"GRAPH ?g {?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" ?xlabel .}" +
				
				"{GRAPH ?g {?xlabel ?p1 ?o1 .}}" +
				"\nUNION" +
				"{GRAPH ?g {?s2 ?p2 ?xlabel  .}}" +
				
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("concept", concept);
		update.setBinding("xlabel", xlabel);
		update.setBinding("g", getWorkingGraph());
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void removeHiddenLabel(@LocallyDefined @Subject IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "DELETE {" +
				"\nGRAPH ?g {?xlabel ?p1 ?o1 . }"+
				"\nGRAPH ?g {?s2 ?p2 ?xlabel . }"+
				"\n}" + 
				"\nWHERE{" +
				"GRAPH ?g {?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL)+" ?xlabel .}" +
				
				"{GRAPH ?g {?xlabel ?p1 ?o1 .}}" +
				"\nUNION" +
				"{GRAPH ?g {?s2 ?p2 ?xlabel  .}}" +
				
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("concept", concept);
		update.setBinding("xlabel", xlabel);
		update.setBinding("?g", getWorkingGraph());
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void changeLabelInfo(@LocallyDefined @Subject Resource xlabel, @LanguageTaggedString Literal literal){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = 
				"DELETE {"+
				"\nGRAPH ?g {?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLiteral .}" +
				"\n}"+
						
				"INSERT {"+
				"\nGRAPH ?g {?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+NTriplesUtil.toNTriplesString(literal)+" .}" +
				"\n}"+
				
				"\nWHERE{"+
				"?s ?p ?xlabel ." +
				"\nOPTIONAL{" +
				"\nGRAPH ?g {?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLiteral .}" +
				"\n}" +
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("?g", getWorkingGraph());
		update.setBinding("xlabel", xlabel);
		update.execute();
		
	}
	
	/**
	 * Returns the last member of a member list (the one that has no rest or has rdf:nil as rest)
	 */
	private Resource walkMemberList(RepositoryConnection repoConnection, Resource list) {
		RepositoryResult<Statement> stmts = repoConnection.getStatements(list, RDF.REST, null, getWorkingGraph());
		if (stmts.hasNext()) {
			Resource obj = (Resource) stmts.next().getObject();
			if (obj.equals(RDF.NIL)) {
				return list;
			} else {
				return walkMemberList(repoConnection, obj);
			}
		} else {
			//if list has no object for rdf:rest property, assume that rest was rdf:nil and list was the last element
			return list;
		}
	}
	
}