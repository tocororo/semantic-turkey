package it.uniroma2.art.semanticturkey.services.core;

import java.lang.reflect.Method;
import java.util.Arrays;
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
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.utilities.TurtleHelp;

/**
 * This class provides services for manipulating SKOS constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class SKOS extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SKOS.class);
	
	@Autowired
	private CustomFormManager cfManager;

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopConcepts(@Optional @LocallyDefinedResources List<IRI> schemes) {
		QueryBuilder qb;

		if (schemes != null && !schemes.isEmpty()) {
			String query = 
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more WHERE {                                                \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?resource skos:topConceptOf|^skos:hasTopConcept ?scheme .                      \n" +
					"FILTER(";
			boolean first=true;
			for(IRI scheme : schemes){
				if(!first){
					query+= " || ";
				}
				first=false;
				query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
			}		
			query += ") 																				 \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {?aNarrowerConcept skos:broader ?resource .                   \n" +
					"                       ?aNarrowerConcept skos:inScheme ?scheme . } as ?attr_more )  \n" +
					"     }                                                                              \n" +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n";
					// @formatter:on
			qb = createQueryBuilder(query);
		} else {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more WHERE {                                                \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     FILTER NOT EXISTS {?resource skos:broader|^skos:narrower []}                   \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {?aNarrowerConcept skos:broader|^skos:narrower ?resource . } as ?attr_more ) \n" +
					"     }                                                                              \n" +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
					// @formatter:on
			);
		}
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getNarrowerConcepts(@LocallyDefined Resource concept,
			@Optional @LocallyDefinedResources List<IRI> schemes) {
		QueryBuilder qb;

		if (schemes != null && !schemes.isEmpty()) {
			String query = 
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more WHERE {                                                \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?resource skos:broader|^skos:narrower ?concept .                               \n" +
					"     ?resource skos:inScheme ?scheme.                                               \n" +
					"FILTER (";
			boolean first=true;
			for(IRI scheme : schemes){
				if(!first){
					query+= " || ";
				}
				first=false;
				query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
			}	
			query += ") 																				 \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {?aNarrowerConcept skos:broader ?resource .                   \n" +
					"                       ?aNarrowerConcept skos:inScheme ?scheme . } as ?attr_more )  \n" +
					"     }                                                                              \n" +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n";
					// @formatter:on
			qb = createQueryBuilder(query);
			//qb.setBinding("scheme", scheme);
		} else {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more WHERE {                                                \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?resource skos:broader|^skos:narrower ?concept .                               \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {?aNarrowerConcept skos:broader ?resource . } as ?attr_more ) \n" +
					"     }                                                                              \n" +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
					// @formatter:on
			);
		}
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("concept", concept);
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(conceptScheme)', 'R')")
	public Collection<AnnotatedValue<Resource>> getAllSchemes() {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                       \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                      \n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                 \n" +
                "                                                                           \n" +
				" SELECT ?resource WHERE {                                                  \n" +
				"     ?conceptSchemeSubClass rdfs:subClassOf* skos:ConceptScheme .          \n" +
				"     ?resource rdf:type ?conceptSchemeSubClass .                           \n" +
				" }                                                                         \n" +
				" GROUP BY ?resource                                                        \n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'R')")
	public Collection<AnnotatedValue<Resource>> getSchemesMatrixPerConcept(@LocallyDefined Resource concept) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                    \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                                   \n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                              \n" +
                "                                                                                        \n" +
				" SELECT ?resource ?attr_inScheme WHERE {                                                \n" +
				"    ?conceptSchemeSubClass rdfs:subClassOf* skos:ConceptScheme .                        \n" +
				"    ?resource rdf:type ?conceptSchemeSubClass .                                         \n" +
				"    BIND(EXISTS{                                                                        \n" +
				"             ?resource skos:hasTopConcept|^skos:isTopConceptOf|^skos:inScheme ?concept .\n" +
				"         } AS ?attr_inScheme )                                                          \n" +
				" }                                                                                      \n" +
				" GROUP BY ?resource ?attr_inScheme                                                      \n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("concept", concept);
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection)', 'R')")
	public Collection<AnnotatedValue<Resource>> getRootCollections() {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                        \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                       \n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                  \n" +
                "                                                                            \n" +
				" SELECT DISTINCT ?resource WHERE {                                          \n" +
                "   ?collectionSubClass rdfs:subClassOf* skos:Collection .                   \n" +
				"   ?resource rdf:type ?collectionSubClass.                                  \n" +
				"   FILTER NOT EXISTS {                                                      \n" +
				" 	  [] skos:member ?resource .                                             \n" +
				" 	}                                                                        \n" +
				" 	FILTER NOT EXISTS {                                                      \n" +
				" 	  [] skos:memberList/rdf:rest*/rdf:first ?resource .                     \n" +
				" 	}                                                                        \n" +
				" }                                                                          \n" +
				" GROUP BY ?resource                                                         \n"
				// @formatter:on
		);
		qb.process(CollectionsMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(skosCollection, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getNestedCollections(@LocallyDefined Resource container) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                        \n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                       \n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                  \n" +
                "                                                                            \n" +
                " SELECT ?resource (COUNT(DISTINCT ?mid) AS ?index) WHERE {                  \n" +
				"   {                                                                        \n" +
                //for the skos:Collection
				" 	  FILTER NOT EXISTS {?container skos:memberList [] }                     \n" +
				" 	  ?container skos:member ?resource .                                     \n" +
				"   } UNION {                                                                \n" +
				//for the skos:OrderedCollection 
				" 	  ?container skos:memberList ?memberList .                               \n" +
				" 	  ?memberList rdf:rest* ?mid .                                           \n" +
				" 	  ?mid rdf:rest* ?node .                                                 \n" +
				" 	  ?node rdf:first ?resource .                                            \n" +
				"   }                                                                        \n" +
				" 	FILTER EXISTS { ?resource rdf:type/rdfs:subClassOf* skos:Collection . }  \n" +
				" }                                                                          \n" +
				" GROUP BY ?resource                                                         \n" +
				" ORDER BY ?index                                                            \n"
				// @formatter:on
		);
		qb.process(CollectionsMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("container", container);
		return qb.runQuery();
	}

	
	@STServiceOperation
	@Read
	public void failingReadServiceContainingUpdate() {
		try (RepositoryConnection conn = getManagedConnection()) {
			Update update = conn
					.prepareUpdate("insert data {<http://test.it/> <http://test.it/> <http://test.it/> . }");
			update.execute();
		}
	}

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
		
		IRI conceptClass = org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT;
		if (conceptCls != null) {
			conceptClass = conceptCls;
		}
		
		modelAdditions.add(newConceptIRI, RDF.TYPE, conceptClass);
		
		if (label != null) { //?conc skos:prefLabel ?label
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, label);
		}
		for(IRI conceptScheme : conceptSchemes){
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);//?conc skos:inScheme ?sc
		}
		if (broaderConcept != null) {//?conc skos:broader ?broad
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else { //?conc skos:topConceptOf ?sc
			for(IRI conceptScheme : conceptSchemes){
				modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
			}
		}
		
		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newConceptIRI.stringValue());
			if (label != null) {
				stdForm.addFormEntry(StandardForm.Prompt.label, label.getLabel());
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
		
		IRI schemeClass = org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT_SCHEME;
		if (schemeCls != null) {
			schemeClass = schemeCls;
		}
		
		modelAdditions.add(newSchemeIRI, RDF.TYPE, schemeClass);
		
		if (label != null) {
			modelAdditions.add(newSchemeIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, label);
		}

		RepositoryConnection repoConnection = getManagedConnection();
		
		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newSchemeIRI.stringValue());
			if (label != null) {
				stdForm.addFormEntry(StandardForm.Prompt.label, label.getLabel());
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
			if (newCollection.equals(RDF.NIL)) { throw new IllegalArgumentException("Cannot create collection rdf:nil"); }
			newCollectionRes = newCollection;
		}
		
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
		
		if (label != null) {
			modelAdditions.add(newCollectionRes, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, label);
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
			if (label != null) {
				stdForm.addFormEntry(StandardForm.Prompt.label, label.getLabel());
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

	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
		Method m = SKOS.class.getMethod("getNarrowerConcepts", Resource.class, Resource.class,
				String[].class);
		System.out.println(m.getGenericReturnType());
	}
}

class CollectionsMoreProcessor implements QueryBuilderProcessor {

	public static final CollectionsMoreProcessor INSTANCE = new CollectionsMoreProcessor();
	private GraphPattern graphPattern;

	private CollectionsMoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE)
				.prefix("rdfs", RDFS.NAMESPACE)
				.prefix("skos", org.eclipse.rdf4j.model.vocabulary.SKOS.NAMESPACE)
				.projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern(
					// @formatter:off
					" BIND(EXISTS { {                                                                    \n" +
					"                 ?resource a skos:Collection .                                      \n" +
					"                 ?resource skos:member [rdf:type/rdfs:subClassOf* skos:Collection] .\n" +
					"               } union {                                                            \n" +
					"                 ?resource rdf:type/rdfs:subClassOf* skos:OrderedCollection .       \n" +
					"                 ?resource skos:memberList/rdf:rest*/rdf:first [rdf:type/rdfs:subClassOf* skos:Collection] . \n" +
					"             } } AS ?attr_more)                                                     \n"
					// @formatter:on
		).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return false;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

	@Override
	public GraphPattern getGraphPattern(Project<?> currentProject) {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable) {
		return null;
	}
};