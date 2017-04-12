package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
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
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * This class provides services for manipulating OWL/RDFS classes.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Classes extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Classes.class);
	
	@Autowired
	private CustomFormManager cfManager;

	/**
	 * Returns the (explicit) subclasses of the class <code>superClass</code>. If <code>numInst</code> is set to
	 * <code>true</code>, then the description of each class will contain the number of (explicit) instances.
	 * 
	 * @param superClass
	 * @param numInst
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSubClasses(@LocallyDefined IRI superClass,
			@Optional(defaultValue = "true") boolean numInst) {
		QueryBuilder qb;

		if (OWL.THING.equals(superClass)) {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                   \n" +                                      
					" prefix owl: <http://www.w3.org/2002/07/owl#>                                \n" +                                      
					" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>                        \n" +                                      
					"                                                                             \n" +                                      
					" SELECT ?resource ?attr_color WHERE {                                        \n" +                                                    
					" 	  ?metaClass rdfs:subClassOf* owl:Class .                                 \n" +
					"     ?resource a ?metaClass.                                                 \n" +
					"     FILTER(isIRI(?resource))                                                \n" +
					"     FILTER(?resource != owl:Thing)                                          \n" +
					"     FILTER NOT EXISTS {                                                     \n" +
					"     	?resource rdfs:subClassOf ?superClass2 .                              \n" +
					"       FILTER(isIRI(?superClass2) && ?superClass2 != owl:Thing)              \n" +
//					"         ?superClass2 a ?metaClass2 .                                        \n" +
//					"         ?metaClass2 rdfs:subClassOf* rdfs:Class .                           \n" +
					"     }                                                                       \n" +
					" }                                                                           \n" +
					" GROUP BY ?resource ?attr_color                                              \n"                             
					// @formatter:on
			);
		} else if (RDFS.RESOURCE.equals(superClass)) {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                       \n" +                          
					" prefix owl: <http://www.w3.org/2002/07/owl#>                                    \n" +                          
					" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>                            \n" +                          
					"                                                                                 \n" +                          
					" SELECT ?resource ?attr_color WHERE {                                            \n" +
					" 	{                                                                             \n" +
					" 		BIND(owl:Thing as ?resource)                                              \n" +
					" 	} UNION {                                                                     \n" +
					" 		{                                                                         \n" +
					" 			?metaClass rdfs:subClassOf* rdfs:Class .                              \n" +
					" 		} MINUS {                                                                 \n" +
					" 			?metaClass rdfs:subClassOf* owl:Class .                               \n" +
					" 		}                                                                         \n" +
					" 		?resource a ?metaClass .                                                  \n" +
					" 		FILTER(isIRI(?resource))                                                  \n" +
					" 		FILTER(?resource != rdfs:Resource)                                        \n" +
					" 		FILTER NOT EXISTS {                                                       \n" +
					" 			?resource rdfs:subClassOf ?superClass2 .                              \n" +
					" 			FILTER(isIRI(?superClass2) && ?superClass2 != rdfs:Resource)          \n" +
					" 			?superClass2 a ?metaClass2 .                                          \n" +
					" 			?metaClass2 rdfs:subClassOf* rdfs:Class .                             \n" +
					"		}                                                                         \n" +
					" 	}                                                                             \n" +
					" }                                                                               \n" +
					" GROUP BY ?resource ?attr_color                                                  \n"
					// @formatter:on
			);
		} else {
			qb = createQueryBuilder(
				// @formatter:off
				" SELECT ?resource ?attr_color WHERE {                                         \n " +                                                                              
				"    ?resource rdfs:subClassOf " + RenderUtils.toSPARQL(superClass) + "      . \n " +
				"    FILTER(isIRI(?resource))                                                  \n " +
				" }                                                                            \n " +
				" GROUP BY ?resource ?attr_color             		                           \n "
				// @formatter:on
			);
		}
		qb.process(ClassesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.process(FixedRoleProcessor.INSTANCE, "resource", "attr_role");
//		qb.processRole();
		qb.processRendering();
		qb.processQName();
		if (numInst) {
			qb.process(ClassesNumInstProcessor.INSTANCE, "resource", "attr_numInst");
		}
		// qb.setBinding("superClass", superClass);
		qb.setBinding("workingGraph", getWorkingGraph());
		return qb.runQuery();
	}

	/**
	 * Returns the description of the classes in the given <code>classList</code>. If <code>numInst</code> is
	 * set to <code>true</code>, then the description of each class will contain the number of (explicit)
	 * instances.
	 * 
	 * @param classList
	 * @param numInst
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getClassesInfo(IRI[] classList,
			@Optional(defaultValue = "true") boolean numInst) {

		QueryBuilder qb;
		StringBuilder sb = new StringBuilder();
		sb.append(
				// @formatter:off
				" SELECT ?resource WHERE {												\n" +
				"     VALUES(?resource) {");
		sb.append(Arrays.stream(classList).map(iri -> "(" + RenderUtils.toSPARQL(iri) + ")").collect(joining()));
		sb.append("}													 				\n" +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.process(ClassesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		if (numInst) {
			qb.process(ClassesNumInstProcessor.INSTANCE, "resource", "attr_numInst");
		}
		return qb.runQuery();

	}

	/**
	 * Returns the (explicit) instances of the class <code>cls</code>.
	 * 
	 * @param cls
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getInstances(@LocallyDefined IRI cls) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" SELECT ?resource ?attr_color WHERE {                                         \n " +                                                                              
				"    ?resource a ?cls .                                                        \n " +
				" }                                                                            \n " +
				" GROUP BY ?resource ?attr_color             		                           \n "
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("cls", cls);
		return qb.runQuery();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public AnnotatedValue<IRI> createClass(@NotLocallyDefined IRI newClass, @LocallyDefined IRI superClass,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws ProjectInconsistentException, CODAException, CustomFormException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		modelAdditions.add(newClass, RDF.TYPE, OWL.CLASS);
		modelAdditions.add(newClass, RDFS.SUBCLASSOF, superClass);
		
		RepositoryConnection repoConnection = getManagedConnection();

		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newClass.stringValue());
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, customFormId, userPromptMap, stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newClass);
		annotatedValue.setAttribute("role", RDFResourceRolesEnum.cls.name());
		//TODO compute show
		return annotatedValue; 
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public AnnotatedValue<IRI> createInstance(@NotLocallyDefined IRI newInstance, @LocallyDefined IRI cls,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws ProjectInconsistentException, CODAException, CustomFormException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		modelAdditions.add(newInstance, RDF.TYPE, cls);
		
		RepositoryConnection repoConnection = getManagedConnection();

		//CustomForm further info
		if (customFormId != null && userPromptMap != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.resource, newInstance.stringValue());
			enrichWithCustomForm(repoConnection, modelAdditions, modelRemovals, customFormId, userPromptMap, stdForm);
		}

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<IRI>(newInstance);
		if (cls.equals(SKOS.CONCEPT)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.concept.name());
		} else if (cls.equals(SKOS.CONCEPT_SCHEME)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.conceptScheme.name());
		} else if (cls.equals(SKOS.COLLECTION)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.skosCollection.name());
		} else if (cls.equals(SKOS.ORDERED_COLLECTION)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.skosOrderedCollection.name());
		} else if (cls.equals(SKOSXL.LABEL)) {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.xLabel.name());
		} else {
			annotatedValue.setAttribute("role", RDFResourceRolesEnum.individual.name());
		}
		//TODO compute show
		return annotatedValue; 
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

class ClassesMoreProcessor implements QueryBuilderProcessor {

	public static final ClassesMoreProcessor INSTANCE = new ClassesMoreProcessor();
	private GraphPattern graphPattern;

	private ClassesMoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE)
				.prefix("owl", OWL.NAMESPACE).projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern(
						"BIND(?resource = rdfs:Resource || ?resource = owl:Thing || EXISTS{?aSubClass rdfs:subClassOf ?resource . FILTER(?aSubClass != ?resource)} AS ?attr_more)")
				.graphPattern();
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

class ClassesNumInstProcessor implements QueryBuilderProcessor {

	public static final ClassesNumInstProcessor INSTANCE = new ClassesNumInstProcessor();
	private GraphPattern graphPattern;

	private ClassesNumInstProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.count("anInstance", "attr_more"))
				.pattern("?anInstance a ?resource .").graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
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

class FixedRoleProcessor implements QueryBuilderProcessor {

	public static final FixedRoleProcessor INSTANCE = new FixedRoleProcessor();
	private GraphPattern graphPattern;

	private FixedRoleProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.variable("roleT"))
				.pattern("bind(\"cls\" as ?roleT)").graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
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