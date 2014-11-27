package it.uniroma2.art.semanticturkey.data.role;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.filter.StatementWithAnyOfGivenComponents_Predicate;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.NodeFilters;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Collections2;

public class RoleRecognitionOrchestrator {

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
			ResourcePosition subjectPosition, ARTResource subject, Collection<ARTStatement> statements,
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
		for (ARTStatement stmt : Collections2.filter(statements, StatementWithAnyOfGivenComponents_Predicate
				.getFilter(NodeFilters.ANY, RDF.Res.TYPE, NodeFilters.ANY))) {

			processTypeAssignment(stmt.getSubject(), stmt.getObject().asResource(), result);
			
		}

		// ///////////////////////
		// Process tuple bindings

		String resourceTypeVar = varPrefix + "type";
		String resourceVar = "resource";

		for (TupleBindings aBinding : bindings) {
			ARTResource res;
			ARTResource type = null;

			if (aBinding.hasBinding(resourceTypeVar)) {
				res = aBinding.getBoundValue(resourceVar).asResource();
				type = aBinding.getBoundValue(resourceTypeVar).asResource();
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
		} else if ((type.equals(RDFS.Res.CLASS) || type.equals(OWL.Res.CLASS)) && role == RDFResourceRolesEnum.individual) {
			newRole = RDFResourceRolesEnum.cls;
		} else if (type.equals(OWL.Res.OBJECTPROPERTY)
				&& (role == RDFResourceRolesEnum.individual | role == RDFResourceRolesEnum.property || role == null)) {
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
		} else if (type.equals(OWL.Res.DATARANGE)
				&& (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.dataRange;
		} else if (type.equals(OWL.Res.ONTOLOGY)
				&& (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.ontology;
		} else if (type.equals(RDF.Res.PROPERTY)
				&& (role == RDFResourceRolesEnum.individual || role == null)) {
			newRole = RDFResourceRolesEnum.property;
		}

		if (newRole != null) {
			result.put(resource, newRole);
		}

	}

	public String getGraphPatternForDescribe(ResourcePosition resourcePosition,
			ARTResource resourceToBeRoled, String varPrefix) {
		return String.format("{?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?%1$stype .}",
				varPrefix);
	}
}
