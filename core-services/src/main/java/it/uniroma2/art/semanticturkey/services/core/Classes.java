package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ClassWithSubclassesOrInstancesException;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterParserException;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterPrefixNotDefinedException;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterSyntacticException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Deleted;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.aspects.ResourceLevelChangeMetadataSupport;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.ManchesterClassInterface;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * This class provides services for manipulating OWL/RDFS classes.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Classes extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(Classes.class);
	
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
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getSubClasses(@LocallyDefined IRI superClass,
			@Optional(defaultValue = "true") boolean numInst) {
		QueryBuilder qb;

		if (OWL.THING.equals(superClass)) {
			qb = createQueryBuilder(
					// @formatter:off
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
					"PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>					\n" +
					"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n" +
                    "																		\n" +
					//adding the nature in the SELECT, which should be removed when the appropriate processor is used
					"SELECT ?resource "+generateNatureSPARQLSelectPart()+"					\n" +
					"WHERE {																\n" +
					"	?metaClass rdfs:subClassOf* rdfs:Class .							\n" +
					"	?resource a ?metaClass.												\n" +
					"	FILTER(isIRI(?resource))											\n" +
					"	FILTER(?resource != owl:Thing)										\n" +
					"	FILTER(?resource != rdfs:Resource)									\n" +
					"	FILTER(?metaClass != rdfs:Datatype)									\n" +
					"	FILTER NOT EXISTS {													\n" +
					"		?resource rdfs:subClassOf ?superClass2 .						\n" +
					"		FILTER(?resource != ?superClass2)								\n" +
					"		FILTER(isIRI(?superClass2) && ?superClass2 != owl:Thing)		\n" +
					"		?superClass2 a ?metaClass2 .									\n" +
					"		?metaClass2 rdfs:subClassOf* rdfs:Class .						\n" +
					"	}																	\n" +
					//adding the nature in the query (will be replaced by the appropriate processor), 
					//remember to change the SELECT as well
					generateNatureSPARQLWherePart("?resource") +
					"}																		\n" +
					"GROUP BY ?resource														\n"
					// @formatter:on
			);
		} else if (RDFS.RESOURCE.equals(superClass)) {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                       \n" +                          
					" prefix owl: <http://www.w3.org/2002/07/owl#>                                    \n" +                          
					" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>                            \n" +                          
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                             \n" +
					" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                          	  \n" +
                    "                                                                                 \n" +                          
					//adding the nature in the SELECT, which should be removed when the appropriate processor is used
					" SELECT ?resource "+generateNatureSPARQLSelectPart()+" 						  \n" + 
					" WHERE {																          \n" +
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
					"			FILTER(?resource != ?superClass2)									  \n" +	
					" 			FILTER(isIRI(?superClass2) && ?superClass2 != rdfs:Resource)          \n" +
					" 			?superClass2 a ?metaClass2 .                                          \n" +
					" 			?metaClass2 rdfs:subClassOf* rdfs:Class .                             \n" +
					"		}                                                                         \n" +
					" 	}                                                                             \n" +
					
					//adding the nature in the query (will be replaced by the appropriate processor), 
					//remember to change the SELECT as well
					generateNatureSPARQLWherePart("?resource") +
					
					
					" }                                                                               \n" +
					" GROUP BY ?resource 			                                                  \n"
					// @formatter:on
			);
		} else {
			qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                   \n" +                                      
				" prefix owl: <http://www.w3.org/2002/07/owl#>                                \n" +                                      
				" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>                        \n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>                         \n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>                         \n" +
				//adding the nature in the SELECT, which should be removed when the appropriate processor is used
				" SELECT ?resource "+generateNatureSPARQLSelectPart()+" 			 		 \n" + 
				" WHERE {																      \n" +
				"    ?resource rdfs:subClassOf " + RenderUtils.toSPARQL(superClass) + "      .\n " +
				"    FILTER(isIRI(?resource))                                                 \n " +
				
				//adding the nature in the query (will be replaced by the appropriate processor), 
				//remember to change the SELECT as well
				generateNatureSPARQLWherePart("?resource") +
				
				" }                                                                           \n " +
				" GROUP BY ?resource			             		                          \n "
				// @formatter:on
			);
		}
		qb.process(ClassesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.process(FixedRoleProcessor.INSTANCE, "resource", "attr_role");
		qb.processRendering();
		qb.processQName();
		if (numInst) {
			qb.process(ClassesNumInstProcessor.INSTANCE, "resource", "attr_numInst");
		}
		// qb.setBinding("superClass", superClass);
		qb.setBinding("workingGraph", getWorkingGraph());
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'R')")
	public Collection<AnnotatedValue<Resource>> getSuperClasses(@LocallyDefined IRI cls) {
		QueryBuilder qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" prefix owl: <http://www.w3.org/2002/07/owl#>								\n" +
				" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>						\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>						\n" +
				" SELECT ?resource "+generateNatureSPARQLSelectPart()+"						\n" +
				" WHERE {																	\n" +
					 RenderUtils.toSPARQL(cls) + " rdfs:subClassOf ?resource				.\n " +
				"    FILTER(isIRI(?resource))												\n " +
				generateNatureSPARQLWherePart("?resource") +
				" }																			\n " +
				" GROUP BY ?resource"
				// @formatter:on
		);
		qb.process(FixedRoleProcessor.INSTANCE, "resource", "attr_role");
		qb.processRendering();
		qb.processQName();
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
	@PreAuthorize("@auth.isAuthorized('rdf(cls)', 'R')")
	public Collection<AnnotatedValue<Resource>> getClassesInfo(IRI[] classList,
			@Optional(defaultValue = "true") boolean numInst) {

		QueryBuilder qb;
		StringBuilder sb = new StringBuilder();
		sb.append(
				// @formatter:off
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +                                      
				" prefix owl: <http://www.w3.org/2002/07/owl#>							\n" +                                      
				" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>					\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>					\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {		\n" +
				"     VALUES(?resource) {");
		sb.append(Arrays.stream(classList).map(iri -> "(" + RenderUtils.toSPARQL(iri) + ")").collect(joining()));
		sb.append("}													 				\n" +
				generateNatureSPARQLWherePart("?resource") +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.process(ClassesMoreProcessor.INSTANCE, "resource", "attr_more");
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
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> getInstances(@LocallyDefined IRI cls,
															 @Optional(defaultValue = "false") boolean includeNonDirect) {
		// @formatter:off
		String query =
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>						\n" +
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>							\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>									\n" +                                      
				" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>							\n" +
				" PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>							\n" +
				" SELECT ?resource " + generateNatureSPARQLSelectPart() + " WHERE {				\n ";

		if (includeNonDirect) {
			query += " ?resource a/rdfs:subClassOf* ?cls .										\n " ;
		} else {
			query += "?resource a ?cls .				 										\n";
		}
		query +=generateNatureSPARQLWherePart("?resource") +
				" }																				\n " +
				" GROUP BY ?resource 															\n ";
		// @formatter:on
		QueryBuilder qb = createQueryBuilder(query);
		qb.processRendering();
		qb.processQName();
		qb.setBinding("cls", cls);
		return qb.runQuery();
	}
	
	/**
	 * Returns the number of instances of the given class.
	 * Also {@link Classes#getClassesInfo(IRI[], boolean)} returns it, but it computes other info, so in case 
	 * of a lot of instances it could be slow. 
	 * @param cls
	 * @return
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Integer getNumberOfInstances(@LocallyDefined IRI cls) {
		String query = " SELECT (count(distinct ?s) as ?count) WHERE { \n" + 
			"?s a " + NTriplesUtil.toNTriplesString(cls) + " \n" +
			"}";
		TupleQueryResult result = getManagedConnection().prepareTupleQuery(query).evaluate();
		return ((Literal) result.next().getValue("count")).intValue();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls)', 'C')")
	public AnnotatedValue<IRI> createClass(@Optional @NotLocallyDefined IRI newClass,
			@LocallyDefined IRI superClass,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2000/01/rdf-schema#Class") IRI classType,
			@Optional CustomFormValue customFormValue) throws CODAException, CustomFormException {

		RepositoryConnection repoConnection = getManagedConnection();
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.type, classType.stringValue());
			newClass = generateResourceWithCustomConstructor(repoConnection, newClass,
					customFormValue, stdForm, modelAdditions, modelRemovals);
		} else if (newClass == null) {
			//both customFormValue and newClass null
			throw new IllegalStateException("Cannot create a resource without providing its IRI or without using a CustomForm with the delegation");
		}
		
		IRI classCls = (classType != null) ? classType : OWL.CLASS;
		modelAdditions.add(newClass, RDF.TYPE, classCls);
		modelAdditions.add(newClass, RDFS.SUBCLASSOF, superClass);

		//set versioning metadata
		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(newClass, RDFResourceRole.cls);

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<>(newClass);
		annotatedValue.setAttribute("role", RDFResourceRole.cls.name());
		annotatedValue.setAttribute("explicit", true);
		return annotatedValue; 
	}
	
	/**
	 * Deletes a class
	 * @param cls
	 * @throws DeniedOperationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls)', 'D')")
	public void deleteClass(@LocallyDefined @Deleted IRI cls) throws DeniedOperationException {
		RepositoryConnection repoConnection = getManagedConnection();
		
		//first check if the class has any subClasses or instances
		
		String query =
			// @formatter:off
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>		\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  \n" +
			"ASK {														\n" +
			"  [] rdfs:subClassOf|rdf:type ?cls  					    \n" +
			"}															\n";
			// @formatter:on
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setBinding("cls", cls);
		booleanQuery.setIncludeInferred(false);
		if(booleanQuery.evaluate()){
			throw new ClassWithSubclassesOrInstancesException(cls);
		}
		
		query = 
				"DELETE {																\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		?s1 ?p1 ?cls .													\n"
				+ "		?cls ?p2 ?o2 .													\n"
				+ "	}																	\n"
				+ "} WHERE {															\n"
				+ "	BIND(URI('" + cls.stringValue() + "') AS ?cls)						\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		{ ?s1 ?p1 ?cls . }												\n"
				+ "		UNION															\n"
				+ "		{ ?cls ?p2 ?o2 . }												\n"
				+ "	}																	\n"
				+ "}";
		repoConnection.prepareUpdate(query).execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(individual)', 'C')")
	public AnnotatedValue<IRI> createInstance(@Optional @NotLocallyDefined IRI newInstance, @LocallyDefined IRI cls,
			@Optional CustomFormValue customFormValue)
					throws CODAException, CustomFormException {

		RepositoryConnection repoConnection = getManagedConnection();
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();

		if (customFormValue != null) {
			StandardForm stdForm = new StandardForm();
			stdForm.addFormEntry(StandardForm.Prompt.type, cls.stringValue());
			newInstance = generateResourceWithCustomConstructor(repoConnection, newInstance,
					customFormValue, stdForm, modelAdditions, modelRemovals);
		} else if (newInstance == null) {
			//both customFormValue and newInstance null
			throw new IllegalStateException("Cannot create a resource without providing its IRI or without using a CustomForm with the delegation");
		}
		
		modelAdditions.add(newInstance, RDF.TYPE, cls);

		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedValue = new AnnotatedValue<>(newInstance);
		if (cls.equals(SKOS.CONCEPT)) {
			annotatedValue.setAttribute("role", RDFResourceRole.concept.name());
		} else if (cls.equals(SKOS.CONCEPT_SCHEME)) {
			annotatedValue.setAttribute("role", RDFResourceRole.conceptScheme.name());
		} else if (cls.equals(SKOS.COLLECTION)) {
			annotatedValue.setAttribute("role", RDFResourceRole.skosCollection.name());
		} else if (cls.equals(SKOS.ORDERED_COLLECTION)) {
			annotatedValue.setAttribute("role", RDFResourceRole.skosOrderedCollection.name());
		} else if (cls.equals(SKOSXL.LABEL)) {
			annotatedValue.setAttribute("role", RDFResourceRole.xLabel.name());
		} else {
			annotatedValue.setAttribute("role", RDFResourceRole.individual.name());
		}
		annotatedValue.setAttribute("explicit", true);

		//set versioning metadata
		ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(newInstance,
				RDFResourceRole.valueOf(annotatedValue.getAttributes().get("role").stringValue()));
		
		return annotatedValue; 
	}
	
	/**
	 * Deletes an instance
	 * @param instance
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(individual)', 'D')")
	public void deleteInstance(@LocallyDefined @Deleted IRI instance) {
		RepositoryConnection repoConnection = getManagedConnection();
		String query = 
				// @formatter:off
				"DELETE {																\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		?s1 ?p1 ?instance .												\n"
				+ "		?instance ?p2 ?o2 .												\n"
				+ "	}																	\n"
				+ "} WHERE {															\n"
				+ "	BIND(URI('" + instance.stringValue() + "') AS ?instance)			\n"
				+ "	GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {	\n"
				+ "		{ ?s1 ?p1 ?instance . }											\n"
				+ "		UNION															\n"
				+ "		{ ?instance ?p2 ?o2 . }											\n"
				+ "	}																	\n"
				+ "}";
				// @formatter:on
		repoConnection.prepareUpdate(query).execute();
	}
	
	
	/**
	 * adds an rdfs:superClassOf relationship between two resources (already defined in the ontology)
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'C')")
	public void addSuperCls(@LocallyDefined @Modified(role=RDFResourceRole.cls) IRI cls, @LocallyDefined IRI supercls){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		modelAdditions.add(repoConnection.getValueFactory().createStatement(cls, RDFS.SUBCLASSOF, supercls));
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	/**
	 * removes the rdfs:superClassOf relationship between two resources (already defined in the ontology)
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'D')")
	public void removeSuperCls(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			IRI supercls) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		modelRemovals.add(repoConnection.getValueFactory().createStatement(cls, RDFS.SUBCLASSOF, supercls));
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	/**
	 * Adds the OWL.INTERSECTIONOF to the description of the class <code>cls</code> using the supplied array, 
	 * <code>clsDescriptions</code> of Manchester expressions to generate the property values. In  
	 * <code>clsDescriptions</code> each element of the array could be a single classIRI
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'C')")
	public void addIntersectionOf(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			List<String> clsDescriptions) throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		RepositoryConnection repoConnection = getManagedConnection();
		Map<String, String> prefixToNamespacesMap = getProject().getOntologyManager()
				.getNSPrefixMappings(false);
		Model modelAdditions = new LinkedHashModel();
		addCollectionBasedClassAxiom(cls, OWL.INTERSECTIONOF, clsDescriptions, modelAdditions, repoConnection,
				prefixToNamespacesMap);
		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	/**
	 * Removes an axiom identified by the property <code>OWL.INTERSECTIONOF</code> based on the collection 
	 * identified by <code>collectionNode</code> from the description of the class identified by <code>cls</code>.
	 */ 
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'D')")
	public void removeIntersectionOf(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			@LocallyDefined BNode collectionBNode) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		removeCollectionBasedClassAxiom(cls, OWL.INTERSECTIONOF, collectionBNode, modelRemovals,
				repoConnection);
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	
	/**
	 * Adds the OWL.UNIONOF to the description of the class <code>cls</code> using the supplied array, 
	 * <code>clsDescriptions</code> of Manchester expressions to generate the property values. In  
	 * <code>clsDescriptions</code> each element of the array could be a single classIRI
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'C')")
	public void addUnionOf(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			List<String> clsDescriptions) throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		RepositoryConnection repoConnection = getManagedConnection();
		Map<String, String> prefixToNamespacesMap = getProject().getOntologyManager()
				.getNSPrefixMappings(false);
		Model modelAdditions = new LinkedHashModel();
		addCollectionBasedClassAxiom(cls, OWL.UNIONOF, clsDescriptions, modelAdditions, repoConnection,
				prefixToNamespacesMap);
		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	/**
	 * Removes an axiom identified by the property <code>OWL.UNIONOF</code> based on the collection 
	 * identified by <code>collectionNode</code> from the description of the class identified by <code>cls</code>.
	 */ 
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'D')")
	public void removeUnionOf(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			@LocallyDefined BNode collectionBNode) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		removeCollectionBasedClassAxiom(cls, OWL.UNIONOF, collectionBNode, modelRemovals, repoConnection);
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	/**
	 * Enumerates (via <code>owl:oneOf</code>) all and only members of the class <code>cls</code>, which
	 * are provided by the parameter <code>individuals</code>
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'C')")
	public void addOneOf(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			List<IRI> individuals) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		createAndAddList(cls, OWL.ONEOF, individuals, modelAdditions, repoConnection);
		repoConnection.add(modelAdditions, getWorkingGraph());
	}	
	
	/**
	 * Removes the enumeration <code>collectionNode</code> from the description of the class
	 * <code>cls</code>
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'D')")
	public void removeOneOf(@LocallyDefined @Modified(role = RDFResourceRole.cls) IRI cls,
			@LocallyDefined BNode collectionBNode) {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelRemovals = new LinkedHashModel();
		removeCollectionBasedClassAxiom(cls, OWL.ONEOF, collectionBNode, modelRemovals, repoConnection);
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}	
	
	private void addCollectionBasedClassAxiom(IRI cls, IRI axiomProp, @NotEmpty List<String> clsDescriptions, 
			Model modelAdditions, RepositoryConnection repoConnection, Map<String, String> prefixToNamespacesMap)
			throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		List<Resource> resourceList = new ArrayList<>();
		//generate all the triple associated to each clsDescription
		for(String clsDesc : clsDescriptions){
			//check if clsDesc is a single IRI or a manchester expression
			boolean isIRI = false;
			if(clsDesc.startsWith("<") && clsDesc.endsWith(">")){
				String clsDescCleaned = clsDesc.substring(1, clsDesc.length()-1);
				if(!clsDescCleaned.contains("<") && !clsDescCleaned.contains(">")){
					//this means that it is a single IRI (and not something like "<...> and <...>"
					IRI clsDescIRI = repoConnection.getValueFactory().createIRI(clsDescCleaned);
					isIRI = true;
					resourceList.add(clsDescIRI);
				}
			}
			if(!isIRI){
				ManchesterClassInterface mci = ManchesterSyntaxUtils.parseCompleteExpression(clsDesc, 
						repoConnection.getValueFactory(), prefixToNamespacesMap);
				List<Statement> statList = new ArrayList<>();
				//it is possible to cast the Resource to a BNode, because the input mci should have a bnode as 
				// starting element
				BNode newBnode = (BNode) ManchesterSyntaxUtils.parseManchesterExpr(mci, statList, 
						repoConnection.getValueFactory());
				modelAdditions.addAll(statList);
				//add the generated BNode into a list
				resourceList.add(newBnode);
			}
		}
		
		//now add all the generated bnode in a list, which is linked to the input cls with the property axiomProp
		createAndAddList(cls, axiomProp,resourceList, modelAdditions, repoConnection);
	}
	
	private void createAndAddList(IRI cls, IRI prop, List<? extends Resource> elemForList, Model modelAdditions, 
			RepositoryConnection repoConnection ){
		BNode elemInList = repoConnection.getValueFactory().createBNode();
		modelAdditions.add(repoConnection.getValueFactory().createStatement(cls, prop, elemInList));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.FIRST, 
				elemForList.get(0)));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.TYPE, RDF.LIST));
		for(int i=1; i<elemForList.size(); ++i){
			BNode nextBNode = repoConnection.getValueFactory().createBNode();
			modelAdditions.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.REST, nextBNode));
			elemInList = nextBNode;
			modelAdditions.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.FIRST, 
					elemForList.get(i)));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.TYPE, RDF.LIST));
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.REST, RDF.NIL));
	}
	
	private void removeCollectionBasedClassAxiom(IRI cls, IRI prop, BNode collectionBNode, 
		Model modelRemovals, RepositoryConnection repoConnection) {
		//iterate over the list, using its first element, collectionBNode, to get all the elements to 
		// remove them
		Resource elemInList = collectionBNode;
		while(!elemInList.equals(RDF.NIL)){
			modelRemovals.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.TYPE, RDF.LIST));
			try(RepositoryResult<Statement> repositoryResult = 
					repoConnection.getStatements(elemInList, RDF.FIRST, null, getWorkingGraph())){
				Resource first = (Resource) repositoryResult.next().getObject();
				modelRemovals.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.FIRST, first));
				
			}
			try(RepositoryResult<Statement> repositoryResult = 
					repoConnection.getStatements(elemInList, RDF.REST, null, getWorkingGraph())){
				Resource next = (Resource) repositoryResult.next().getObject();
				modelRemovals.add(repoConnection.getValueFactory().createStatement(elemInList, RDF.REST, next));
				elemInList = next;
			}
		}
		//remove the triple linking the cls to the list
		modelRemovals.add(repoConnection.getValueFactory().createStatement(cls, prop, collectionBNode));
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
	public GraphPattern getGraphPattern(STServiceContext context) {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		return null;
	}
}

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
	public GraphPattern getGraphPattern(STServiceContext context) {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		return null;
	}
}

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
	public GraphPattern getGraphPattern(STServiceContext context) {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
		return null;
	}

}