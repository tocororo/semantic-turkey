package it.uniroma2.art.semanticturkey.services.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
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
//	@PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', '{key1: ''value1'', key2: true}', 'R')")
	public Collection<AnnotatedValue<Resource>> getTopConcepts(@Optional @LocallyDefined Resource scheme) {
		QueryBuilder qb;

		if (scheme != null) {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                                \n" +
					" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                               \n" +
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                          \n" +
                    "                                                                                    \n" +
					" SELECT ?resource ?attr_more WHERE {                                                \n" +
					"     ?conceptSubClass rdfs:subClassOf* skos:Concept .                               \n" +
					"     ?resource rdf:type ?conceptSubClass .                                          \n" +
					"     ?resource skos:topConceptOf|^skos:hasTopConcept ?scheme .                      \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {?aNarrowerConcept skos:broader ?resource .                   \n" +
					"                       ?aNarrowerConcept skos:inScheme ?scheme . } as ?attr_more )  \n" +
					"     }                                                                              \n" +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
					// @formatter:on
			);
			qb.setBinding("scheme", scheme);
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
	public Collection<AnnotatedValue<Resource>> getNarrowerConcepts(@LocallyDefined Resource concept,
			@Optional @LocallyDefined Resource scheme) {
		QueryBuilder qb;

		if (scheme != null) {
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
					"     ?resource skos:inScheme ?scheme.                                               \n" +
					"     OPTIONAL {                                                                     \n" +
					"         BIND( EXISTS {?aNarrowerConcept skos:broader ?resource .                   \n" +
					"                       ?aNarrowerConcept skos:inScheme ?scheme . } as ?attr_more )  \n" +
					"     }                                                                              \n" +
					" }                                                                                  \n" +
					" GROUP BY ?resource ?attr_more                                                      \n"
					// @formatter:on
			);
			qb.setBinding("scheme", scheme);
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
		modelAdditions.add(newConceptIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT); //?conc a skos:Concept
		
		if (label != null) { //?conc skos:prefLabel ?label
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, label);
		}
		modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);//?conc skos:inScheme ?sc
		if (broaderConcept != null) {//?conc skos:broader ?broad
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else { //?conc skos:topConceptOf ?sc
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
		}

		//CustomForm further info
		RepositoryConnection repoConnection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(repoConnection);
		if (customFormId != null && userPromptMap != null) {
			try {
				CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
				if (cForm.isTypeGraph()){
					CustomFormGraph cfGraph = cForm.asCustomFormGraph();
					StandardForm stdForm = new StandardForm(newConceptIRI, label, label.getLanguage().orElse(null));
					UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm);
					shutDownCodaCore(codaCore);
					
					for (ARTTriple t : updates.getInsertTriples()){
						modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
					}
					for (ARTTriple t : updates.getDeleteTriples()){
						modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
					}
				} else {
					throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
						+ "' as constructor since it is not of type 'graph'");
				}
			} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e){
				throw new CODAException(e);
			} finally {
				shutDownCodaCore(codaCore);
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedConcept = new AnnotatedValue<IRI>(newConceptIRI);
		annotatedConcept.setAttribute("role", RDFResourceRolesEnum.concept.name());
		//TODO compute show
		return annotatedConcept; 
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
		
		if (label != null) {
			modelAdditions.add(newSchemeIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.PREF_LABEL, label);
		}

		//CustomForm further info
		RepositoryConnection repoConnection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(repoConnection);
		if (customFormId != null && userPromptMap != null) {
			try {
				
				CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
				if (cForm.isTypeGraph()){
					CustomFormGraph cfGraph = cForm.asCustomFormGraph();
					StandardForm stdForm = new StandardForm(newSchemeIRI, label, label.getLanguage().orElse(null));
					UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm);
					shutDownCodaCore(codaCore);
					
					for (ARTTriple t : updates.getInsertTriples()){
						modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
					}
					for (ARTTriple t : updates.getDeleteTriples()){
						modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
					}
				} else {
					throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
						+ "' as constructor since it is not of type 'graph'");
				}
			} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e){
				throw new CODAException(e);
			} finally {
				shutDownCodaCore(codaCore);
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedConcept = new AnnotatedValue<IRI>(newSchemeIRI);
		annotatedConcept.setAttribute("role", RDFResourceRolesEnum.conceptScheme.name());
		//TODO compute show
		return annotatedConcept; 
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