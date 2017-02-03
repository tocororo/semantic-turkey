package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * This class provides services for manipulating OWL/RDFS classes.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Classes extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Classes.class);

	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSubClasses(@LocallyDefined IRI superClass) {
		QueryBuilder qb;

		if (OWL.THING.equals(superClass)) {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                   \n" +                                      
					" prefix owl: <http://www.w3.org/2002/07/owl#>                                \n" +                                      
					" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>                        \n" +                                      
					"                                                                             \n" +                                      
					" SELECT ?resource ?attr_color ?attr_more WHERE {                             \n" +                                                    
					" 	?resource a ?metaClass .                                                  \n" +
					"     FILTER EXISTS {                                                         \n" +
					"      ?metaClass rdfs:subClassOf* owl:Class .                                \n" +
					"     }                                                                       \n" +
					"     FILTER(isIRI(?resource))                                                \n" +
					"     FILTER(?resource != owl:Thing)                                          \n" +
					"     FILTER NOT EXISTS {                                                     \n" +
					"     	?resource rdfs:subClassOf ?superClass2 .                              \n" +
					"         FILTER(isIRI(?superClass2) && ?superClass2 != owl:Thing)            \n" +
					"         ?superClass2 a ?metaClass2 .                                        \n" +
					"         ?metaClass2 rdfs:subClassOf* rdfs:Class .                           \n" +
					"     }                                                                       \n" +
					" }                                                                           \n" +
					" GROUP BY ?resource ?attr_color ?attr_more                                   \n"                             
					// @formatter:on
			);
		} else if (RDFS.RESOURCE.equals(superClass)) {
			qb = createQueryBuilder(
					// @formatter:off
					" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                       \n" +                          
					" prefix owl: <http://www.w3.org/2002/07/owl#>                                    \n" +                          
					" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>                            \n" +                          
					"                                                                                 \n" +                          
					" SELECT ?resource ?attr_color ?attr_more WHERE {                                 \n" +
					" 	{                                                                             \n" +
					" 		BIND(owl:Thing as ?resource)                                              \n" +
					" 	} UNION {                                                                     \n" +
					" 		?resource a ?metaClass .                                                  \n" +
					" 		FILTER EXISTS {                                                           \n" +
					" 			{                                                                     \n" +
					" 				?metaClass rdfs:subClassOf* rdfs:Class .                          \n" +
					" 			} MINUS {                                                             \n" +
					" 				?metaClass rdfs:subClassOf* owl:Class .                           \n" +
					" 			}                                                                     \n" +
					" 		}                                                                         \n" +
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
					" GROUP BY ?resource ?attr_color ?attr_more                                       \n"
					// @formatter:on
			);
		} else {
			qb = createQueryBuilder(
				// @formatter:off
				" SELECT ?resource ?attr_color ?attr_more WHERE {                              \n " +                                                                              
				"    ?resource rdfs:subClassOf " + RenderUtils.toSPARQL(superClass) + "      . \n " +
				"    FILTER(isIRI(?resource))                                                  \n " +
				" }                                                                            \n " +
				" GROUP BY ?resource ?attr_color ?attr_more    		                           \n "
				// @formatter:on
			);

		}
		qb.process(MoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		// qb.setBinding("superClass", superClass);
		qb.setBinding("workingGraph", getWorkingGraph());
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getClassesInfo(IRI[] classList) {
		logger.info("request to get the Propery info, given a property List");

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
		qb.process(MoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRole();
		qb.processRendering();
		qb.processQName();
		return qb.runQuery();

	}

}

class MoreProcessor implements QueryBuilderProcessor {

	public static final MoreProcessor INSTANCE = new MoreProcessor();
	private GraphPattern graphPattern;

	private MoreProcessor() {
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
	public GraphPattern getGraphPattern() {
		return graphPattern;
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> currentProject, List<BindingSet> resultTable) {
		return null;
	}
};