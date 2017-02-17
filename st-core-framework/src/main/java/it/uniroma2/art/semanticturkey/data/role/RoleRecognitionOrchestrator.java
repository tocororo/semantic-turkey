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

	public Map<ARTResource, RDFResourceRolesEnum> computeRoleOf(Project<?> project,
			ResourcePosition subjectPosition, ARTResource subject, OWLModel statements,
			Collection<ARTResource> resources, Collection<TupleBindings> bindings, String varPrefix)
					throws ModelAccessException, DataAccessException {

		Map<ARTResource, RDFResourceRolesEnum> result = new HashMap<ARTResource, RDFResourceRolesEnum>();

		for (ARTResource r : resources) {
			if (r.isURIResource()) {
				result.put(r, RDFResourceRolesEnum.individual);
			}
		}

		// ///////////////////////////
		// Process subject statements
		try (ARTStatementIterator stmtIt = statements.listStatements(NodeFilters.ANY, RDF.Res.TYPE,
				NodeFilters.ANY, true)) {
			while (stmtIt.streamOpen()) {
				ARTStatement stmt = stmtIt.getNext();
				processTypeAssignment(stmt.getSubject(), stmt.getObject().asResource(), result);
			}
		}

		// ///////////////////////
		// Process tuple bindings

		String resourceTypeVar = varPrefix + "_subject_type";
		String predicateTypeVar = varPrefix + "_predicate_type";
		String objectTypeVar = varPrefix + "_object_type";
		String indirectObjectTypeVar = varPrefix + "_indirectObject_type";

		String objectVar = "object";
		String predicateVar = "predicate";
		String resourceVar = "resource";
		String indirectObjectVar = varPrefix + "_indirectObject";

		for (TupleBindings aBinding : bindings) {
			ARTResource res;
			ARTResource type = null;

			if (aBinding.hasBinding(resourceTypeVar)) {
				res = aBinding.getBoundValue(resourceVar).asResource();
				type = aBinding.getBoundValue(resourceTypeVar).asResource();
			} else if (aBinding.hasBinding(predicateTypeVar)) {
				res = aBinding.getBoundValue(predicateVar).asResource();
				type = aBinding.getBoundValue(predicateTypeVar).asResource();
			} else if (aBinding.hasBinding(objectTypeVar)) {
				res = aBinding.getBoundValue(objectVar).asResource();
				type = aBinding.getBoundValue(objectTypeVar).asResource();
			} else if (aBinding.hasBinding(indirectObjectTypeVar)) {
				res = aBinding.getBoundValue(indirectObjectVar).asResource();
				type = aBinding.getBoundValue(indirectObjectTypeVar).asResource();
			} else
				continue;

			processTypeAssignment(res, type, result);

		}

		return result;

	}

	private void processTypeAssignment(ARTResource resource, ARTResource type,
			Map<ARTResource, RDFResourceRolesEnum> result) {

		RDFResourceRolesEnum role = result.get(resource);

		if (role == null) {
			role = RDFResourceRolesEnum.individual;
			result.put(resource, role);
		}

		RDFResourceRolesEnum newRole = null;

		// if (role == null)
		// continue;
		//
		// TODO: check with respect to model type!!!!

		if (type.equals(SKOS.Res.CONCEPT) && (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.concept;
		} else if (type.equals(SKOS.Res.CONCEPTSCHEME)
				&& (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.conceptScheme;
		} else if (type.equals(SKOSXL.Res.LABEL) && role == RDFResourceRolesEnum.individual) {
			newRole = RDFResourceRolesEnum.xLabel;
		} else if (type.equals(SKOS.Res.ORDEREDCOLLECTION) && (role == RDFResourceRolesEnum.individual
				|| role == RDFResourceRolesEnum.skosCollection || role == null)) {
			newRole = RDFResourceRolesEnum.skosOrderedCollection;
		} else if (type.equals(SKOS.Res.COLLECTION)
				&& (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.skosCollection;
		} else if ((type.equals(RDFS.Res.CLASS) || type.equals(OWL.Res.CLASS))
				&& role == RDFResourceRolesEnum.individual) {
			newRole = RDFResourceRolesEnum.cls;
		} else if (type.equals(OWL.Res.OBJECTPROPERTY)
				&& (role == RDFResourceRolesEnum.individual | role == RDFResourceRolesEnum.property
						|| role == null)) {
			newRole = RDFResourceRolesEnum.objectProperty;
		} else if (type.equals(OWL.Res.DATATYPEPROPERTY)
				&& (role == RDFResourceRolesEnum.individual | role == RDFResourceRolesEnum.objectProperty
						| role == RDFResourceRolesEnum.property || role == null)) {
			newRole = RDFResourceRolesEnum.datatypeProperty;
		} else if (type.equals(OWL.Res.ANNOTATIONPROPERTY)
				&& (role == RDFResourceRolesEnum.individual | role == RDFResourceRolesEnum.objectProperty
						| role == RDFResourceRolesEnum.property || role == null)) {
			newRole = RDFResourceRolesEnum.annotationProperty;
		} else if (type.equals(OWL.Res.ONTOLOGYPROPERTY)
				&& (role == RDFResourceRolesEnum.individual | role == RDFResourceRolesEnum.objectProperty
						| role == RDFResourceRolesEnum.property || role == null)) {
			newRole = RDFResourceRolesEnum.ontologyProperty;
		} else
			if (type.equals(OWL.Res.DATARANGE) && (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.dataRange;
		} else if (type.equals(OWL.Res.ONTOLOGY)
				&& (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.ontology;
		} else
			if (type.equals(RDF.Res.PROPERTY) && (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.property;
		}

		if (newRole != null) {
			result.put(resource, newRole);
		}

	}

	public String getGraphPatternForDescribe(ResourcePosition resourcePosition, ARTResource resourceToBeRoled,
			String varPrefix) {
		return String.format(
				"{{?object <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?%1$s_object_type .} union {?object <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>*/<http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?%1$s_indirectObject . ?%1$s_indirectObject <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?%1$s_indirectObject_type .} union {?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?%1$s_subject_type .} union {?predicate <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?%1$s_predicate_type .}}",
				varPrefix);
	}

	@Override
	public GraphPattern getGraphPattern(Project<?> currentProject) {
		return GraphPatternBuilder.create().prefix("rdf", RDF.NAMESPACE).prefix("rdfs", RDFS.NAMESPACE).prefix("owl", OWL.NAMESPACE)
				.prefix("skos", SKOS.NAMESPACE).prefix("skosxl", SKOSXL.NAMESPACE).projection(ProjectionElementBuilder.variable("attr_role"))
				.pattern(
				// @formatter:off
				"BIND(IF(EXISTS {?resource a [rdfs:subClassOf* skos:Concept]}, \"concept\", " +
						"IF(EXISTS {?resource a [rdfs:subClassOf* skos:ConceptScheme]}, \"conceptScheme\", " + 
							"IF(EXISTS {?resource a [rdfs:subClassOf* skosxl:Label]}, \"xLabel\", " +
								"IF(EXISTS {?resource a [rdfs:subClassOf* skos:OrderedCollection]}, \"skosOrderedCollection\", " +
									"IF(EXISTS {?resource a [rdfs:subClassOf* skos:Collection]}, \"skosCollection\", " +
										"IF(EXISTS {?resource a [rdfs:subClassOf* rdfs:Class]}, \"cls\", " +
											"IF(EXISTS {?resource a [rdfs:subClassOf* owl:DatatypeProperty]}, \"datatypeProperty\", " +
												"IF(EXISTS {?resource a [rdfs:subClassOf* owl:OntologyProperty]}, \"ontologyProperty\", " +
													"IF(EXISTS {?resource a [rdfs:subClassOf* owl:AnnotationProperty]}, \"annotationProperty\", " +
														"IF(EXISTS {?resource a [rdfs:subClassOf* owl:ObjectProperty]}, \"objectProperty\", " +
															"IF(EXISTS {?resource a [rdfs:subClassOf* owl:DataRange]}, \"dataRange\", " +
																"IF(EXISTS {?resource a [rdfs:subClassOf* rdf:Property]}, \"property\", " +
																	"IF(EXISTS {?resource a [rdfs:subClassOf* owl:Ontology]}, \"ontology\", " +
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
