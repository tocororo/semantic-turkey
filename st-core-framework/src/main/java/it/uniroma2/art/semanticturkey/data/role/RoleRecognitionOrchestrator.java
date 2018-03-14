package it.uniroma2.art.semanticturkey.data.role;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilderProcessor;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;

public class RoleRecognitionOrchestrator implements QueryBuilderProcessor {

	public static final int MAXIMUM_REMOTE_DATASET_ACCESS = 5;

	private static RoleRecognitionOrchestrator instance;

	private RoleRecognitionOrchestrator() {

	}

	public static synchronized RoleRecognitionOrchestrator getInstance() {
		if (instance == null) {
			instance = new RoleRecognitionOrchestrator();
		}

		return instance;
	}

	@Override
	public GraphPattern getGraphPattern(Project currentProject) {
		return GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE).prefix("rdfs", RDFS.NAMESPACE)
				.prefix("owl", OWL.NAMESPACE).prefix("skos", SKOS.NAMESPACE).prefix("ontolex", ONTOLEX.NAMESPACE).prefix("lime", LIME.NAMESPACE)
				.prefix("skosxl", SKOSXL.NAMESPACE).projection(ProjectionElementBuilder.variable("attr_role"))
				.pattern(
				// @formatter:off
				"BIND(IF(EXISTS {?metaClass rdfs:subClassOf* lime:Lexicon . ?resource a ?metaClass .}, \"limeLexicon\", " +
						"IF(EXISTS {?metaClass rdfs:subClassOf* ontolex:LexicalEntry . ?resource a ?metaClass .}, \"ontolexLexicalEntry\", " +
							"IF(EXISTS {?metaClass rdfs:subClassOf* ontolex:Form . ?resource a ?metaClass .}, \"ontolexForm\", " +
								"IF(EXISTS {?metaClass rdfs:subClassOf* skos:Concept . ?resource a ?metaClass .}, \"concept\", " +
									"IF(EXISTS {?metaClass rdfs:subClassOf* skos:ConceptScheme . ?resource a ?metaClass . }, \"conceptScheme\", " + 
										"IF(EXISTS {?metaClass rdfs:subClassOf* skosxl:Label . ?resource a ?metaClass . }, \"xLabel\", " +
											"IF(EXISTS {?metaClass rdfs:subClassOf* skos:OrderedCollection . ?resource a ?metaClass . }, \"skosOrderedCollection\", " +
												"IF(EXISTS {?metaClass rdfs:subClassOf* skos:Collection . ?resource a ?metaClass . }, \"skosCollection\", " +
													"IF(EXISTS {?metaClass rdfs:subClassOf* rdfs:Datatype . ?resource a ?metaClass . }, \"dataRange\", " +
														"IF(EXISTS {?metaClass rdfs:subClassOf* rdfs:Class . ?resource a ?metaClass . }, \"cls\", " +
															"IF(EXISTS {?metaClass rdfs:subClassOf* owl:DatatypeProperty . ?resource a ?metaClass . }, \"datatypeProperty\", " +
																"IF(EXISTS {?metaClass rdfs:subClassOf* owl:OntologyProperty . ?resource a ?metaClass . }, \"ontologyProperty\", " +
																	"IF(EXISTS {?metaClass rdfs:subClassOf* owl:AnnotationProperty . ?resource a ?metaClass . }, \"annotationProperty\", " +
																		"IF(EXISTS {?metaClass rdfs:subClassOf* owl:ObjectProperty . ?resource a ?metaClass . }, \"objectProperty\", " +
																			"IF(EXISTS {?metaClass rdfs:subClassOf* rdf:Property . ?resource a ?metaClass . }, \"property\", " +
																				"IF(EXISTS {?metaClass rdfs:subClassOf* owl:Ontology . ?resource a ?metaClass . }, \"ontology\", " +
							"\"individual\")))))))))))))))) as ?attr_role)"		
				// @formatter:on
				).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public Map<Value, Literal> processBindings(Project project, List<BindingSet> resultTable) {
		return null;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}

	public static RDFResourceRole computeRole(Resource resource, RepositoryConnection repoConn) {
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
				+ "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + "SELECT ?role WHERE { \n"
				+ "BIND(?tempResource as ?resource) \n"
				+ "BIND(IF(EXISTS {?metaClass rdfs:subClassOf* skos:Concept . ?resource a ?metaClass .}, \"concept\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* skos:ConceptScheme . ?resource a ?metaClass . }, \"conceptScheme\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* skosxl:Label . ?resource a ?metaClass . }, \"xLabel\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* skos:OrderedCollection . ?resource a ?metaClass . }, \"skosOrderedCollection\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* skos:Collection . ?resource a ?metaClass . }, \"skosCollection\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* rdfs:Datatype . ?resource a ?metaClass . }, \"dataRange\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* rdfs:Class . ?resource a ?metaClass . }, \"cls\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* owl:DatatypeProperty . ?resource a ?metaClass . }, \"datatypeProperty\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* owl:OntologyProperty . ?resource a ?metaClass . }, \"ontologyProperty\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* owl:AnnotationProperty . ?resource a ?metaClass . }, \"annotationProperty\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* owl:ObjectProperty . ?resource a ?metaClass . }, \"objectProperty\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* rdf:Property . ?resource a ?metaClass . }, \"property\", \n"
				+ "IF(EXISTS {?metaClass rdfs:subClassOf* owl:Ontology . ?resource a ?metaClass . }, \"ontology\", \n"
				+ "\"individual\"))))))))))))) as ?role)}";
		TupleQuery tq = repoConn.prepareTupleQuery(query);
		tq.setBinding("tempResource", resource);
		try (TupleQueryResult result = tq.evaluate()) {
			String roleValue = result.next().getValue("role").stringValue();
			return RDFResourceRole.valueOf(roleValue);
		}
	}

}
