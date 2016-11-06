package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElement;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

/**
 * This class provides services for manipulating OWL constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class OWL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(OWL.class);
	
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getSubClasses(@LocallyDefined Resource superClass) {
		QueryBuilder qb = createQueryBuilder(
			// @formatter:off
			" SELECT ?resource ?g ?metaClass WHERE {                        \n" +  
			"     ?resource rdfs:subClassOf ?superClass .                   \n" +
			"     FILTER(?resource != ?superClass)                          \n" +
			"     optional {                                                \n" +
			"         graph ?g {                                            \n" +
			"             ?resource a ?metaClass .                          \n" + 
			"         }                                                     \n" +
			"         ?metaClass rdfs:subClassOf* rdfs:Class .              \n" +
			"     }                                                         \n" +
			" }                                                             \n" +
			" GROUP BY ?resource ?g ?metaClass                              \n"
			// @formatter:on
		);
		qb.process(MoreProcessor.INSTANCE, "resource", "attr_more");
		qb.processRendering();
		qb.processRole();
		qb.setBinding("superClass", superClass);
		return qb.runQuery();
	}
}

class MoreProcessor implements QueryBuilderProcessor {

	public static final MoreProcessor INSTANCE = new MoreProcessor();
	private GraphPattern graphPattern;

	private MoreProcessor() {
		this.graphPattern = GraphPatternBuilder.create().prefix("rdfs", RDFS.NAMESPACE)
				.projection(ProjectionElementBuilder.variable("attr_more"))
				.pattern(
						"BIND( EXISTS{?aSubClass rdfs:subClassOf ?resource . FILTER(?aSubClass != ?resource)} AS ?attr_more)")
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