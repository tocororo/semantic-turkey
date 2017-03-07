package it.uniroma2.art.semanticturkey.data.role;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
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
	public GraphPattern getGraphPattern(Project<?> currentProject) {
		return GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE).prefix("rdfs", RDFS.NAMESPACE).prefix("owl", OWL.NAMESPACE)
				.prefix("skos", SKOS.NAMESPACE).prefix("skosxl", SKOSXL.NAMESPACE).projection(ProjectionElementBuilder.variable("attr_role"))
				.pattern(
				// @formatter:off
				"BIND(IF(EXISTS {?metaClass rdfs:subClassOf* skos:Concept . ?resource a ?metaClass .}, \"concept\", " +
						"IF(EXISTS {?metaClass rdfs:subClassOf* skos:ConceptScheme . ?resource a ?metaClass . }, \"conceptScheme\", " + 
							"IF(EXISTS {?metaClass rdfs:subClassOf* skosxl:Label . ?resource a ?metaClass . }, \"xLabel\", " +
								"IF(EXISTS {?metaClass rdfs:subClassOf* skos:OrderedCollection . ?resource a ?metaClass . }, \"skosOrderedCollection\", " +
									"IF(EXISTS {?metaClass rdfs:subClassOf* skos:Collection . ?resource a ?metaClass . }, \"skosCollection\", " +
										"IF(EXISTS {?metaClass rdfs:subClassOf* rdfs:Class . ?resource a ?metaClass . }, \"cls\", " +
											"IF(EXISTS {?metaClass rdfs:subClassOf* owl:DatatypeProperty . ?resource a ?metaClass . }, \"datatypeProperty\", " +
												"IF(EXISTS {?metaClass rdfs:subClassOf* owl:OntologyProperty . ?resource a ?metaClass . }, \"ontologyProperty\", " +
													"IF(EXISTS {?metaClass rdfs:subClassOf* owl:AnnotationProperty . ?resource a ?metaClass . }, \"annotationProperty\", " +
														"IF(EXISTS {?metaClass rdfs:subClassOf* owl:ObjectProperty . ?resource a ?metaClass . }, \"objectProperty\", " +
															"IF(EXISTS {?metaClass rdfs:subClassOf* owl:DataRange . ?resource a ?metaClass . }, \"dataRange\", " +
																"IF(EXISTS {?metaClass rdfs:subClassOf* rdf:Property . ?resource a ?metaClass . }, \"property\", " +
																	"IF(EXISTS {?metaClass rdfs:subClassOf* owl:Ontology . ?resource a ?metaClass . }, \"ontology\", " +
							"\"individual\"))))))))))))) as ?attr_role)"		
				// @formatter:on
		).graphPattern();
	}

	@Override
	public boolean introducesDuplicates() {
		return true;
	}

	@Override
	public Map<Value, Literal> processBindings(Project<?> project, List<BindingSet> resultTable) {
		return null;
	}

	@Override
	public String getBindingVariable() {
		return "resource";
	}
}
