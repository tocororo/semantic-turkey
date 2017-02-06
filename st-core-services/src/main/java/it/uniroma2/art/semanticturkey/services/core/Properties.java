package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * This class provides services for reading the Properties.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class Properties extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Properties.class);

	/**
	 * returns all root properties
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopProperties() {
		logger.info("request to get all the top properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	?propertyType	.							\n" +
				"     FILTER (?propertyType = rdf:Property || " +
				"				?propertyType = owl:ObjectProperty ||" +
				"				?propertyType = owl:DatatypeProperty ||" +
				"				?propertyType = owl:AnnotationProperty ||" +
				"				?propertyType = owl:OntologyProperty  )						\n" +
				"     FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root RDF properties
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopRDFProperties() {
		logger.info("request to get the top RDF properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	rdf:Property	.							\n" +
				"     FILTER (NOT EXISTS{ ?resource rdfs:subPropertyOf ?superProp})			\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Object properties
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> geTopObjectProperties() {
		logger.info("request to get the top Object properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:ObjectProperty	.						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Datatype properties
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopDatatypeProperties() {
		logger.info("request to get the top Datatype properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:DatatypeProperty .						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Annotation properties
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopAnnotationProperties() {
		logger.info("request to get the top Annotation properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:AnnotationProperty  .					\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * returns all root Ontology properties
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopOntologyProperties() {
		logger.info("request to get the top Ontology properties");
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>						\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>					\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>								\n" +
                "																			\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdf:type	owl:OntologyProperty .						\n" +
				"     FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp}) 		\n" +
				" }																			\n" +
				" GROUP BY ?resource														\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	/**
	 * takes a list of Properties and return their description as if they were roots for a tree
	 * (so more, role, explicit etc...)
	 * @param propList
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getPropertiesInfo(IRI[] propList) {
		logger.info("request to get the Propery info, given a property List");
		
		QueryBuilder qb;
		StringBuilder sb = new StringBuilder();
		sb.append(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     VALUES(?resource) {");
		for(int i=0; i<propList.length; ++i){
			sb.append("(<"+propList[i].stringValue()+">)");
		}
		sb.append("}													 				\n" +
				"} 																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb = createQueryBuilder(sb.toString());
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSubProperties(@LocallyDefined Resource superProperty) {
		logger.info("request to get the top sub properties for "+superProperty.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>									\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>								\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>											\n" +
                "																						\n" +
				" SELECT ?resource WHERE {													\n" +
				"     ?resource 	rdfs:subPropertyOf	?superProperty .								\n" +
				" }																						\n" +
				" GROUP BY ?resource															\n"
				// @formatter:on
		);
		qb.process(PropertiesMoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("superProperty", superProperty);
		return qb.runQuery();
	}
	
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSuperProperties(@LocallyDefined Resource subProperty) {
		logger.info("request to get the top super properties for "+subProperty.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?subProperty 	rdfs:subPropertyOf	?resource		 .				\n" +
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("subProperty", subProperty);
		return qb.runQuery();
	}
	
	/**
	 * Retrieves all types of res, then all properties having their domain on any of the types for res.
	 * Note that it provides only root properties (e.g. if both rdfs:label and skos:prefLabel,
	 * which is a subProperty of rdfs:label, have domain = one of the types of res, then only rdfs:label is returned)
	 * @param res
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForResource(@LocallyDefined Resource res) {
		logger.info("request to get the top all properties having their domain on any of the types for "+res.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?res 	rdf:type	?type		 .									\n" +
				"     ?resource 	rdfs:domain	?type		 .							\n" +
				"     {?resource 	rdf:type	?typeOfProp  .							\n" + 
				"		FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp})}	\n" + 
				"		UNION															\n" + 
				"     {?resource rdfs:subPropertyOf ?superProp  .						\n" + 
				"		FILTER (NOT EXISTS {?superProp 	rdf:domain	?type})}			\n" + 
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("res", res);
		return qb.runQuery();
	}
	
	/**
	 * Retrieves all properties having their domain on cls.
	 * Note that it has to provide only root properties (e.g. if both rdfs:label and skos:prefLabel,
	 * which is a subProperty of rdfs:label, have domain = cls, then only rdfs:label is returned)
	 * @param classUri
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForClass(@LocallyDefined Resource classUri) {
		logger.info("request to get all properties having their domain on "+classUri.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?resource 	rdf:domain	?classUri		 .						\n" +
				"     {?resource 	rdf:type	?typeOfProp  .							\n" + 
				"		FILTER (NOT EXISTS {?resource rdfs:subPropertyOf ?superProp})}	\n" + 
				"		UNION															\n" + 
				"     {?resource rdfs:subPropertyOf ?superProp  .						\n" + 
				"		FILTER (NOT EXISTS {?superProp 	rdf:domain	?type})}			\n" + 
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("classUri", classUri);
		return qb.runQuery();
	}
	
	/**
	 * it takes any named class which is relevant in the domain of prop.
	 * Relevant means that if the domain of prop is (A or B) or (A and B) in any case the relevant domain classes
	 * are provided by a list with A and B.
	 * @param propeperty
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantDomainClasses(@LocallyDefined Resource property) {
		logger.info("request to get any named class which is relevant in the domain of "+property.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?propeperty 	rdfs:domain		?resource	.						\n" +
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("propeperty", property);
		return qb.runQuery();
	}
	
	/**
	 * it takes any named class which is relevant in the range of prop.
	 * Relevant means that if the range of prop is (A or B) or (A and B) in any case the relevant domain classes
	 * are provided by a list with A and B.
	 * @param propeperty
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantRangeClasses(@LocallyDefined Resource propeperty) {
		logger.info("request to get any named class which is relevant in the range of "+propeperty.stringValue());
		
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?propeperty 	rdfs:range	?resource	.							\n" +
				" }																		\n" +
				" GROUP BY ?resource													\n"
				// @formatter:on
		);
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		qb.setBinding("propeperty", propeperty);
		return qb.runQuery();
	}
	
	
}


class PropertiesMoreProcessor implements QueryBuilderProcessor {

	public static final PropertiesMoreProcessor INSTANCE = new PropertiesMoreProcessor();
	private GraphPattern graphPattern;

	private PropertiesMoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE)
				.prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern("BIND( EXISTS {?aSubProperty rdfs:subPropertyOf ?resource . } AS ?attr_more )  \n").graphPattern();
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
	public GraphPattern getGraphPattern() {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable) {
		return null;
	}
};