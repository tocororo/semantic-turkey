package it.uniroma2.art.semanticturkey.versioning;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.PlaceholderStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.CODATriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataAssociation;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataAssociationStore;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataPattern;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataPatternStore;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.event.annotation.EventListener;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.events.ResourceCreated;
import it.uniroma2.art.semanticturkey.services.events.ResourceDeleted;
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.versioning.OperationContext.OpCtxKeys;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Managed metadata about a resource across its life cycle.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ResourceMetadataManager {

	private static final Logger logger = LoggerFactory.getLogger(ResourceMetadataManager.class);

	private static final String OPERATION_CONTEXT_FEATURE_NAME = "opCtx";
	private static final String OPERATION_CONTEXT_TYPE_PATH = "it.uniroma2.art.semanticturkey.opCtxFS";

	@Autowired
	protected ExtensionPointManager exptManager;

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	/*
	TODO
	catch or forward the exceptions eventually thrown using the ResourceMetadataPattern?
	forwarding might break creation/update/deletion of resources in case of invalid references in the ResourceMetadata
	configuration, so it is better to catch the exceptions and simply skip the application of the patterns
	*/

	/**
	 * Manages metadata about a resource just created. This listener is executed before the current
	 * transaction is committed, so that it can add metadata within the same transaction that created a
	 * resource.
	 *
	 * @param event
	 */
	@TransactionalEventListener(phase = Phase.beforeCommit)
	public void onCreation(ResourceCreated event) {
		logger.debug("Created: " + event.getResource());

		Project project = event.getProject();
		Repository repository = event.getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);
		Resource workingGraph = event.getWGraph();
		Resource resource = event.getResource();
		RDFResourceRole role = event.getRole();

		try {
			OperationContext opCtx = createOperationContext(resource);
			for (ResourceMetadataPattern pattern : getPatternsForRole(project, role)) {
				if (pattern.construction != null) {
					UpdateTripleSet updates = executePearl(conn, pattern.construction, opCtx);
					applyUpdates(conn, updates, workingGraph);
				}
			}
		} catch (STPropertyAccessException | NoSuchConfigurationManager | IOException e) {
			e.printStackTrace();
		}

		// Usually, we expect either one created resource or one modified resource, so
		// there should be no need of complex buffering techniques

//		ValueFactory vf = conn.getValueFactory();
//		Literal currentTime = vf.createLiteral(new Date());
//		String creationDateProp = project.getProperty(Project.CREATION_DATE_PROP);
//		if (creationDateProp != null) {
//			IRI creationDatePropIRI = vf.createIRI(creationDateProp);
//			if (determineNecessityOfMetadata(conn, r, event.getRole(), project)) {
//				conn.remove(r, creationDatePropIRI, null, workingGraph);
//				conn.add(r, creationDatePropIRI, currentTime, workingGraph);
//			}
//		}
	}

	/**
	 * Manages metadata about a resource that was updated. This listener is executed before the current
	 * transaction is committed, so that it can add metadata within the same transaction that updated a
	 * resource.
	 *
	 * @param event
	 */
	@TransactionalEventListener(phase = Phase.beforeCommit)
	public void onUpdate(ResourceModified event) {
		logger.debug("Updated: " + event.getResource());

		Project project = event.getProject();
		Repository repository = event.getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);
		Resource workingGraph = event.getWGraph();
		Resource resource = event.getResource();
		RDFResourceRole role = event.getRole();

		try {
			OperationContext opCtx = createOperationContext(resource);
			for (ResourceMetadataPattern pattern : getPatternsForRole(project, role)) {
				if (pattern.update != null) {
					UpdateTripleSet updates = executePearl(conn, pattern.update, opCtx);
					applyUpdates(conn, updates, workingGraph);
				}
			}
		} catch (STPropertyAccessException | NoSuchConfigurationManager | IOException e) {
			e.printStackTrace();
		}

		// Usually, we expect either one created resource or one modified resource, so
		// there should be no need of complex buffering techniques

//		String modificationDateProp = project.getProperty(Project.MODIFICATION_DATE_PROP);
//		ValueFactory vf = conn.getValueFactory();
//		Literal currentTime = vf.createLiteral(new Date());
//		if (modificationDateProp != null) {
//			IRI modificationDatePropIRI = vf.createIRI(modificationDateProp);
//			if (determineNecessityOfMetadata(conn, r, event.getRole(), project)) {
//				conn.remove(r, modificationDatePropIRI, null, workingGraph);
//				conn.add(r, modificationDatePropIRI, currentTime, workingGraph);
//			}
//		}
	}

	/**
	 * Manages metadata about a resource that was created. This listener (which is not transactional) is
	 * executed Immediately as the event is first published. Indeed, deletions are published before they
	 * actually occur, because otherwise all 1st level statements of the deleted resource would have been
	 * removed from the repository.
	 *
	 * @param event
	 */
	@EventListener
	public void onDeletion(ResourceDeleted event) {
		logger.debug("Deleted: " + event.getResource());

		Project project = event.getProject();
		Repository repository = event.getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository, false);
		Resource workingGraph = event.getWGraph();
		Resource resource = event.getResource();
		RDFResourceRole role = event.getRole();

		try {
			OperationContext opCtx = createOperationContext(resource);
			for (ResourceMetadataPattern pattern : getPatternsForRole(project, role)) {
				if (pattern.destruction != null) {
					UpdateTripleSet updates = executePearl(conn, pattern.destruction, opCtx);
					applyUpdates(conn, updates, workingGraph);
				}
			}
		} catch (STPropertyAccessException | NoSuchConfigurationManager | IOException e) {
			e.printStackTrace();
		}
	}

//	private boolean determineNecessityOfMetadata(RepositoryConnection conn, Resource resource,
//			RDFResourceRole role, Project project) {
//
//		logger.debug("Given role: {}", role);
//
//		if (role == RDFResourceRole.undetermined) {
//			role = RDFResourceRole.valueOf(RoleRecognitionOrchestrator.computeRole(resource, conn).name());
//		}
//
//		logger.debug("After computation role: {}", role);
//
//		for (RDFResourceRole updatableRole : project.getUpdateForRoles()) {
//			if (RDFResourceRole.subsumes(updatableRole, role, true)) {
//				logger.debug("Role {} is subsumed by role {}", role, updatableRole);
//				return true;
//			}
//		}
//
//		logger.debug("Do not update");
//
//		return false;
//	}

	public ResourceMetadataPatternStore getResourceMetadataPatternStore() throws NoSuchConfigurationManager {
		return (ResourceMetadataPatternStore) exptManager
				.getConfigurationManager(ResourceMetadataPatternStore.class.getName());
	}

	public ResourceMetadataAssociationStore getResourceMetadataAssociationStore() throws NoSuchConfigurationManager {
		return (ResourceMetadataAssociationStore) exptManager
				.getConfigurationManager(ResourceMetadataAssociationStore.class.getName());
	}

	private OperationContext createOperationContext(Resource resource) {
		OperationContext opCtx = new OperationContext();
		opCtx.addEntry(OpCtxKeys.resource, resource.stringValue());
		opCtx.addEntry(OpCtxKeys.user, UsersManager.getLoggedUser().getIRI().stringValue());
		return opCtx;
	}

	/**
	 * Returns a list of {@link ResourceMetadataPattern} associated with the given role.
	 *
	 * @param project
	 * @param role
	 * @return
	 */
	private List<ResourceMetadataPattern> getPatternsForRole(Project project, RDFResourceRole role)
			throws NoSuchConfigurationManager, IOException, STPropertyAccessException {
		List<ResourceMetadataPattern> patterns = new ArrayList<>();
		ResourceMetadataAssociationStore associationStore = getResourceMetadataAssociationStore();
		//search an association for the given role
		for (Reference ref : associationStore.getConfigurationReferences(project, null)) {
			ResourceMetadataAssociation association = associationStore.getConfiguration(ref);
			if (RDFResourceRole.subsumes(association.role, role, true)) {
				ResourceMetadataPattern pattern = getResourceMetadataPattern(project, association.patternReference);
				patterns.add(pattern);
			}
		}
		return patterns;
	}

	/**
	 * Returns the {@link ResourceMetadataPattern} with the given reference.
	 * This method looks among the configurations stored at any level (pu, usr, proj, system) and also among the factory-provided
	 *
	 * @param project
	 * @param reference
	 * @return
	 * @throws IOException
	 * @throws STPropertyAccessException
	 */
	public ResourceMetadataPattern getResourceMetadataPattern(Project project, String reference)
			throws IOException, STPropertyAccessException, NoSuchConfigurationManager {
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		if (reference.startsWith("factory")) {
			String fileName = reference.substring(reference.indexOf(":") + 1) + ".cfg";
			File factConfFile = store.getFactoryConfigurationFile(fileName);
			return STPropertiesManager.loadSTPropertiesFromYAMLFiles(ResourceMetadataPattern.class, true, factConfFile);
		} else {
			Reference ref = parseReference(project, reference);
			return store.getConfiguration(ref);
		}
	}

	private Reference parseReference(Project project, String relativeReference) {
		int colonPos = relativeReference.indexOf(":");
		if (colonPos == -1)
			throw new IllegalArgumentException("Invalid reference: " + relativeReference);

		Scope scope = Scope.deserializeScope(relativeReference.substring(0, colonPos));
		String identifier = relativeReference.substring(colonPos + 1);

		switch (scope) {
			case SYSTEM:
				return new Reference(null, null, identifier);
			case PROJECT:
				return new Reference(project, null, identifier);
			case USER:
				return new Reference(null, UsersManager.getLoggedUser(), identifier);
			case PROJECT_USER:
				return new Reference(project, UsersManager.getLoggedUser(), identifier);
			default:
				throw new IllegalArgumentException("Unsupported scope: " + scope);
		}
	}

	/* ========== CODA and PEARL stuff ========== */

	private void applyUpdates(RepositoryConnection conn, UpdateTripleSet updates, Resource workingGraph) {
		Model modelRemovals = new LinkedHashModel();
		for (CODATriple triple : updates.getDeleteTriples()) {
			logger.debug("ResourceMetadata, remove: " + triple.getSubject() + ", " + triple.getPredicate() + ", " + triple.getObject());
//			System.out.println("REM " + triple.getSubject() + ", " + triple.getPredicate() + ", " + triple.getObject());
			modelRemovals.add(triple.getSubject(), triple.getPredicate(), triple.getObject());
		}
		Model modelAdditions = new LinkedHashModel();
		for (CODATriple triple : updates.getInsertTriples()) {
			logger.debug("ResourceMetadata, add: " + triple.getSubject() + ", " + triple.getPredicate() + ", " + triple.getObject());
//			System.out.println("ADD " + triple.getSubject() + ", " + triple.getPredicate() + ", " + triple.getObject());
			modelAdditions.add(triple.getSubject(), triple.getPredicate(), triple.getObject());
		}
		conn.remove(modelRemovals, workingGraph);
		conn.add(modelAdditions, workingGraph);
	}

	private UpdateTripleSet executePearl(RepositoryConnection connection, String pearl, OperationContext opCtx) {
		UpdateTripleSet uts = new UpdateTripleSet();

		try {
			CODACore codaCore = getInitializedCodaCore(connection);
			ProjectionRulesModel prRulesModel = codaCore.setProjectionRulesModel(new ByteArrayInputStream(pearl.getBytes(StandardCharsets.UTF_8)));
			ProjectionRule rule = prRulesModel.getProjRule().values().iterator().next(); //I assume there is only one rule in the pearl
			TypeSystemDescription tsd = createTypeSystemDescription(rule);

			// this jcas has the structure defined by the TSD (created following the pearl)
			JCas jcas = JCasFactory.createJCas(tsd);
			CAS aCAS = jcas.getCas();
			TypeSystem ts = aCAS.getTypeSystem();
			//Create an annotation named after the pearlRule
			Type annotationType = ts.getType(rule.getUIMAType());
			AnnotationFS ann = aCAS.createAnnotation(annotationType, 0, 0);
			// create a FS type opCtxFS and fill its features with the value found in OperationContext
			Type opCtxType = ts.getType(OPERATION_CONTEXT_TYPE_PATH);
			Feature opCtxFeature = annotationType.getFeatureByBaseName(OPERATION_CONTEXT_FEATURE_NAME);
			FeatureStructure opCtxFS = createAndFillOpCtxFS(opCtxType, aCAS, opCtx.asMap());
			ann.setFeatureValue(opCtxFeature, opCtxFS);
			aCAS.addFsToIndexes(ann);
//			analyseCas(aCAS);

			// run coda with the given pearl and the cas just created (the pearl has been set to codaCore in createTypeSystemDescription)
			codaCore.setJCas(jcas);
			while (codaCore.isAnotherAnnotationPresent()) {
				SuggOntologyCoda suggOntCoda = codaCore.processNextAnnotation();
				// get only triples of relevant annotations (those triples that start with it.uniroma2.
				if (suggOntCoda.getAnnotation().getType().getName().startsWith("it.uniroma2")) {
					uts.addInsertTriples(suggOntCoda.getAllInsertARTTriple());
					uts.addDeleteTriples(suggOntCoda.getAllDeleteARTTriple());
				}
			}
		} catch (PRParserException | UIMAException | ComponentProvisioningException | ConverterException |
				DependencyException | RDFModelNotSetException | ProjectionRuleModelNotSet |
				UnassignableFeaturePathException e) {
			e.printStackTrace();
		}
		return uts;
	}

	/**
	 * Given a PEARL projection rule creates the TypeSystemDescription based on the internal opCtx/ used
	 *
	 * @param rule
	 * @return
	 * @throws ResourceInitializationException
	 */
	private TypeSystemDescription createTypeSystemDescription(ProjectionRule rule) throws ResourceInitializationException {
		TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
		/*
		TypeSystemDescription will contain only an Annotation named after the rule.
		This Annotation will contain a feature (Top) named "opCtx".
		Then this feature contains in turn as much features (TypeDescription) which the name is
		defined dynamically according the feature path found in the input PEARL.

		construction/update/destruction PEARLs could contain a feature structure opCtx/... (e.g. opCtx/user).
		So, if the FS opCtx/user and opCtx/resource are found the returned TSD will be:
		it.uniroma2.art.RULE_NAME	//type uima.tcas.Annotation
			- opCtx					//type uima.cas.TOP
				- user				//type uima.cas.String
				- resource			//type uima.cas.String

		 */
		//create the main Annotation named after the feature path in the pearl (e.g. "rule it.uniroma2.art......")
		TypeDescription annotationType = tsd.addType(rule.getUIMAType(), "", CAS.TYPE_NAME_ANNOTATION);
		//create the Type it.uniroma2.art...opCtxFS
		TypeDescription opCtxType = tsd.addType(OPERATION_CONTEXT_TYPE_PATH, "", CAS.TYPE_NAME_TOP);
		// get the pearl nodes section
		Map<String, PlaceholderStruct> placeHolderMap = rule.getPlaceholderMap();
		for (PlaceholderStruct placeholderStruct : placeHolderMap.values()) { //for each placeholder defined in the nodes section
			if (placeholderStruct.hasFeaturePath()) { //if it has a feature path (third element of the node definition)
				String featurePath = placeholderStruct.getFeaturePath();
				//check if the featurePath starts with "opCtx/"
				if (featurePath.startsWith(OPERATION_CONTEXT_FEATURE_NAME + "/")) {
					//in case get its feature name and add a feature (with that name) to the type previously created
					String featureName = featurePath.substring(OPERATION_CONTEXT_FEATURE_NAME.length() + 1);
					opCtxType.addFeature(featureName, "", CAS.TYPE_NAME_STRING);
				}
			}
		}
		annotationType.addFeature(OPERATION_CONTEXT_FEATURE_NAME, "", opCtxType.getName());
//		describeTSD(tsd);
		return tsd;
	}

	private FeatureStructure createAndFillOpCtxFS(Type annType, CAS aCAS, Map<String, Object> opCtxValueMap) {
		FeatureStructure opCtxFS = aCAS.createFS(annType);
		//get the features inside opCtx/
		List<Feature> featuresList = annType.getFeatures();
		//fill the feature with the values specified in the inputMap
		for (Feature f : featuresList) {
			//consider only feature starting with the given type (opCtx/)
			if (f.getName().startsWith(annType.getName())) {
				String featureName = f.getShortName();
				//get the value (if any) of the given feature from the map
				Object featureValue = opCtxValueMap.get(featureName);
				//add the value to the FS only if has value
				if (featureValue != null) {
					opCtxFS.setStringValue(f, featureValue.toString());
				}
			}
		}
		return opCtxFS;
	}

	protected CODACore getInitializedCodaCore(RepositoryConnection repoConnection) {
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(repoConnection);
		return codaCore;
	}

	@SuppressWarnings("unused")
	private void describeTSD(TypeSystemDescription tsd) {
		System.out.println("================ TSD structure ================");
		TypeDescription[] types = tsd.getTypes();
		System.out.println("type list:");
		for (TypeDescription type : types) {
			if (type.getName().startsWith("it.uniroma2.art.semanticturkey")) {
				System.out.println("\nType: " + type.getName());
				FeatureDescription[] features = type.getFeatures();
				System.out.println("features:");
				for (FeatureDescription feature : features) {
					System.out.println("\t" + feature.getName() + "\t" + feature.getRangeTypeName());
				}
			}
		}
		System.out.println("===============================================");
	}

	@SuppressWarnings("unused")
	private void analyseCas(CAS aCAS) {
		System.out.println("======== CAS ==========");
		AnnotationIndex<AnnotationFS> anIndex = aCAS.getAnnotationIndex();
		for (AnnotationFS an : anIndex) {
			// I want to explode only my annotation (ignore DocumentAnnotation)
			if (an.getType().getName().startsWith("it.uniroma")) {
				System.out.println("Annotation: " + an.getType().getName());
				Feature feature = an.getType().getFeatureByBaseName(OPERATION_CONTEXT_FEATURE_NAME);
				System.out.println("\tFeature: " + feature.getName());
				FeatureStructure userPromptFS = an.getFeatureValue(feature);
				Type userPromptType = userPromptFS.getType();
				List<Feature> upFeatures = userPromptType.getFeatures();
				for (Feature upF : upFeatures) {
					String upfValue = userPromptFS.getStringValue(upF);
					System.out.println("\t\tFeature: " + upF.getShortName() + "; value: " + upfValue);
				}

			}
		}
		System.out.println("=======================");
	}
}
