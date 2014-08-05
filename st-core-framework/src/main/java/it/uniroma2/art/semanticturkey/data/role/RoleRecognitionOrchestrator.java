package it.uniroma2.art.semanticturkey.data.role;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTNamespace;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.navigation.ARTNamespaceIterator;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.DataAccessFactory;
import it.uniroma2.art.semanticturkey.data.access.PropertyPatternDataAccess;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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

	public Map<ARTResource, RDFResourceRolesEnum> computeRoleOf(Project<?> project, ARTResource subject,
			Collection<ARTStatement> statements, ARTResource... resources) throws ModelAccessException,
			DataAccessException {

		Collection<ARTURIResource> uriResources = new ArrayList<ARTURIResource>();
		Collection<ARTURIResource> localResources = new ArrayList<ARTURIResource>();

		DatasetMetadataRepository metaRepo = DatasetMetadataRepository.getInstance();
		Multimap<DatasetMetadata, ARTURIResource> externalResources = HashMultimap.create();

		for (ARTResource res : resources) {
			if (res.isURIResource()) {
				ARTURIResource uriRes = res.asURIResource();
				uriResources.add(uriRes);

				if (isLocalResource(project.getOWLModel(), uriRes)) {
					localResources.add(uriRes);
				} else {
					DatasetMetadata meta = metaRepo.findDatasetForResource(uriRes);

					if (meta != null) {
						externalResources.put(meta, uriRes);
					}
				}
			}
		}

		Map<ARTResource, RDFResourceRolesEnum> result = new HashMap<ARTResource, RDFResourceRolesEnum>();

		Multimap<ARTURIResource, ARTNode> propertyPattern = HashMultimap.create();
		propertyPattern.put(RDF.Res.TYPE, NodeFilters.ANY);

		if (!localResources.isEmpty()) {
			try {
				PropertyPatternDataAccess dataAccess = DataAccessFactory
						.createPropertyPatternDataAccess(project);
				Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> infoAboutResources = dataAccess
						.retrieveInformationAbout(localResources, propertyPattern);
				processResources(localResources, infoAboutResources, result);
			} catch (DataAccessException e) {
				// Ignores exception
			}
		}

		// TODO: better quota management
		int remainingDataset = MAXIMUM_REMOTE_DATASET_ACCESS;

		for (DatasetMetadata meta : externalResources.keySet()) {
			if (remainingDataset == 0) {
				break;
			} else {
				remainingDataset--;
			}

			Collection<ARTURIResource> resourcesInDataset = externalResources.get(meta);

			PropertyPatternDataAccess dataAccess = DataAccessFactory.createPropertyPatternDataAccess(project,
					meta);

			try {
				Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> infoAboutResources = dataAccess
						.retrieveInformationAbout(resourcesInDataset, propertyPattern);
				processResources(resourcesInDataset, infoAboutResources, result);
			} catch (DataAccessException e) {
				// Ignores exception
			}
		}

		return result;
	}

	private void processResources(Collection<ARTURIResource> resourcesInDataset,
			Map<ARTURIResource, Multimap<ARTURIResource, ARTNode>> infoAboutResources,
			Map<ARTResource, RDFResourceRolesEnum> result) throws ModelAccessException {
		for (ARTURIResource uriRes : resourcesInDataset) {
			Multimap<ARTURIResource, ARTNode> infoAboutRes = infoAboutResources.get(uriRes);

			RDFResourceRolesEnum role = RDFResourceRolesEnum.individual;

			if (infoAboutRes != null) {
				Collection<ARTURIResource> types = RDFIterators.getCollectionFromIterator(RDFIterators.filterURIs(RDFIterators
						.createARTNodeIterator(infoAboutRes.get(RDF.Res.TYPE).iterator())));

				// TODO: consider also the given dataset metadata (e.g. whether the model is modelled in SKOS
				// or not)

				if (types != null) {
					if (types.contains(SKOS.Res.CONCEPT)) {
						role = RDFResourceRolesEnum.concept;
					} else if (types.contains(SKOS.Res.CONCEPTSCHEME)) {
						role = RDFResourceRolesEnum.conceptScheme;
					} else if (types.contains(SKOSXL.Res.LABEL)) {
						role = RDFResourceRolesEnum.xLabel;
					} else if (types.contains(OWL.Res.ONTOLOGY)) {
						role = RDFResourceRolesEnum.ontology;
					} else if (types.contains(OWL.Res.ONTOLOGYPROPERTY)) {
						role = RDFResourceRolesEnum.ontologyProperty;
					} else if (types.contains(OWL.Res.OBJECTPROPERTY)) {
						role = RDFResourceRolesEnum.objectProperty;
					} else if (types.contains(OWL.Res.DATATYPEPROPERTY)) {
						role = RDFResourceRolesEnum.datatypeProperty;
					} else if (types.contains(OWL.Res.ANNOTATIONPROPERTY)) {
						role = RDFResourceRolesEnum.annotationProperty;
					} else if (types.contains(RDF.Res.PROPERTY)) {
						role = RDFResourceRolesEnum.property;
					} else if (types.contains(OWL.Res.DATARANGE)) {
						role = RDFResourceRolesEnum.dataRange;
					} else if (types.contains(OWL.Res.CLASS) || types.contains(RDFS.Res.CLASS)) {
						role = RDFResourceRolesEnum.cls;
					} else {
						role = RDFResourceRolesEnum.individual;
					}
				}

				if (uriRes.getLocalName().equals("Persona")) {
					System.out.println("@@@@1" + uriRes);
					System.out.println("@@@@a" + types.contains(OWL.Res.CLASS));

					System.out.println("@@@@a" + new ArrayList<ARTNode>(types).contains(OWL.Res.CLASS));

					for (ARTNode t : types) {
						System.out.println("@@@@2" + t.toString());
						System.out.println("@@@@2" + t.equals(OWL.Res.CLASS));
						System.out.println("@@@@2" + OWL.Res.CLASS.equals(t));
						System.out.println("@@@@3" + t.asURIResource().equals(OWL.Res.CLASS));
						System.out.println("@@@@2" + OWL.Res.CLASS.equals(t.asURIResource()));
					}
				}
			}

			result.put(uriRes, role);
		}
	}

	// TODO: find better name??
	private boolean isLocalResource(RDFModel rdfModel, ARTURIResource uriResource)
			throws ModelAccessException {
		ARTNamespaceIterator it = rdfModel.listNamespaces();

		try {
			while (it.streamOpen()) {
				ARTNamespace ns = it.getNext();

				if (ns.getName().equals(uriResource.getNamespace())) {
					return true;
				}
			}
		} finally {
			it.close();
		}

		return false;
	}

}
