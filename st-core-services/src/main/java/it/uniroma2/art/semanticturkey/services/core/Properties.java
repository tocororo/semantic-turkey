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

	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopProperties() {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopRDFProperties() {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> geTopObjectProperties() {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopDatatypeProperties() {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopAnnotationProperties() {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getTopOntologyProperties() {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getPropertiesInfo(IRI[] propList) {
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
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSubProperties(@LocallyDefined Resource superProperty) {
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
		qb.setBinding("superProperty", superProperty);
		return qb.runQuery();
	}
	
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSuperProperties(@LocallyDefined Resource subProperty) {
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
		qb.setBinding("subProperty", subProperty);
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForResource(@LocallyDefined Resource res) {
		QueryBuilder qb;
		qb = createQueryBuilder(
				// @formatter:off
				" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>					\n" +
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>				\n" +
				" PREFIX owl: <http://www.w3.org/2002/07/owl#>							\n" +
                "																		\n" +
				" SELECT ?resource WHERE {												\n" +
				"     ?res 	rdf:type	?type		 .									\n" +
				"     ?resource 	rdf:domain	?type		 .							\n" +
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
		qb.setBinding("res", res);
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantPropertiesForClass(@LocallyDefined Resource classUri) {
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
		qb.setBinding("classUri", classUri);
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantDomainClasses(@LocallyDefined Resource propeperty) {
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
		qb.setBinding("propeperty", propeperty);
		return qb.runQuery();
	}
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getRelevantRangeClasses(@LocallyDefined Resource propeperty) {
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