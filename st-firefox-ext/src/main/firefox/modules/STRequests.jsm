Components.utils.import("resource://stmodules/SemturkeyHTTPLegacy.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests"];

/**
 * @class
 */
STRequests = function(){};

/**
 * @class
 */
STRequests.Administration = function(){};
/**
 * @class
 */
STRequests.Alignment = function(){};
/**
 * @class
 */
STRequests.Annotate = function(){};
/**
 * @class
 */
STRequests.Annotation = function(){};    

/**
 * @class
 */
STRequests.RangeAnnotation = function(){};    

/**
 * @class
 */
STRequests.Cls = function(){};
/**
 * @class
 */
STRequests.CustomRanges = function(){};
/**
 * @class
 */
STRequests.Delete = function(){};        
/**
 * @class
 */
STRequests.Graph = function(){};
/**
 * @class
 */
STRequests.ICV = function(){};
/**
 * @class
 */
STRequests.Individual = function(){};    
/**
 * @class
 */
STRequests.InputOutput = function(){};   
/**
 * @class
 */
STRequests.Metadata = function(){};

/**
 * @class
 */
STRequests.MetadataRegistry = function(){};      

/**
 * @class
 */
STRequests.Refactor = function(){};    
/**
 * @class
 */
STRequests.OntoSearch = function(){};
/**
 * @class
 */
STRequests.Page = function(){};          
/**
 * @class
 */
STRequests.Property = function(){};      
/**
 * @class
 */
STRequests.SPARQL = function(){};
/**
 * @class
 */
STRequests.SKOS = function(){};
/**
 * @class
 */
STRequests.SKOSXL = function(){};
/**
 * @class
 */
STRequests.Statement = function(){};
/**
 * @class
 */
STRequests.Synonyms = function(){};      
/**
 * @class
 */
STRequests.SystemStart = function(){};
/**
 * @class
 */
STRequests.Plugins = function(){};
/**
 * @class
 */
STRequests.ProjectsOLD = function(){};
/**
 * @class
 */
STRequests.Projects = function(){};
/**
 * @class
 */
STRequests.OntManager = function(){};
/**
 * @class
 */
STRequests.Resource = function(){};

/**
 * @class
 */
STRequests.ResourceView = function(){};

/**
 * @class
 */
STRequests.XMLSchema = function(){};

/**
 * @class
 */
STRequests.Manchester = function(){};

//Alignment service requests
STRequests.Alignment.serviceName = "Alignment";
STRequests.Alignment.addAlignmentRequest = "addAlignment";
STRequests.Alignment.getMappingRelationsRequest = "getMappingRelations";
STRequests.Alignment.loadAlignmentRequest = "loadAlignment";
STRequests.Alignment.listCellsRequest = "listCells";
STRequests.Alignment.acceptAlignmentRequest = "acceptAlignment";
STRequests.Alignment.acceptAllAlignmentRequest = "acceptAllAlignment";
STRequests.Alignment.acceptAllAboveRequest = "acceptAllAbove";
STRequests.Alignment.rejectAlignmentRequest = "rejectAlignment";
STRequests.Alignment.rejectAllAlignmentRequest = "rejectAllAlignment";
STRequests.Alignment.rejectAllUnderRequest = "rejectAllUnder";
STRequests.Alignment.changeRelationRequest = "changeRelation";
STRequests.Alignment.changeMappingPropertyRequest = "changeMappingProperty";
STRequests.Alignment.applyValidationRequest = "applyValidation";
STRequests.Alignment.listSuggestedPropertiesRequest = "listSuggestedProperties";
STRequests.Alignment.exportAlignmentRequest = "exportAlignment";
STRequests.Alignment.closeSessionRequest = "closeSession";

//systemStart service requests
STRequests.SystemStart.serviceName = "systemStart";
STRequests.SystemStart.startRequest = "start";
STRequests.SystemStart.listTripleStoresRequest = "listTripleStores";

//cls service requests
STRequests.Cls.serviceName = "cls";
STRequests.Cls.addIntersectionOfRequest = "addIntersectionOf";
STRequests.Cls.addUnionOfRequest = "addUnionOf";
STRequests.Cls.addTypeRequest = "addType";
STRequests.Cls.removeTypeRequest = "removeType";
STRequests.Cls.removeTypeRequest = "removeType";
STRequests.Cls.addSuperClsRequest = "addSuperCls";
STRequests.Cls.removeSuperClsRequest = "removeSuperCls";
STRequests.Cls.removeIntersectionOfRequest = "removeIntersectionOf";
STRequests.Cls.removeUnionOfRequest = "removeUnionOf";
STRequests.Cls.getClassTreeRequest = "getClassTree";
STRequests.Cls.getClassDescriptionRequest = "getClsDescription";
STRequests.Cls.getClassAndInstancesInfoRequest = "getClassAndInstancesInfo";
STRequests.Cls.createInstanceRequest = "createInstance";
STRequests.Cls.createClassRequest = "createClass";
STRequests.Cls.getSubClassesRequest = "getSubClasses";
STRequests.Cls.getSuperClassesRequest = "getSuperClasses";
STRequests.Cls.getClassesInfoAsRootsForTreeRequest = "getClassesInfoAsRootsForTree";

STRequests.CustomRanges.serviceName = "CustomRanges";
STRequests.CustomRanges.runCodaRequest = "runCoda";
STRequests.CustomRanges.getReifiedResourceDescriptionRequest = "getReifiedResourceDescription";
STRequests.CustomRanges.removeReifiedResourceRequest = "removeReifiedResource";
STRequests.CustomRanges.executeURIConverterRequest = "executeURIConverter";
STRequests.CustomRanges.executeLiteralConverterRequest = "executeLiteralConverter";
STRequests.CustomRanges.getCustomRangeConfigMapRequest = "getCustomRangeConfigMap"; 
STRequests.CustomRanges.addCustomRangeToPropertyRequest = "addCustomRangeToProperty";
STRequests.CustomRanges.removeCustomRangeFromPropertyRequest = "removeCustomRangeFromProperty";
STRequests.CustomRanges.getCustomRangeRequest = "getCustomRange";
STRequests.CustomRanges.getAllCustomRangesRequest = "getAllCustomRanges";
STRequests.CustomRanges.createCustomRangeRequest = "createCustomRange";
STRequests.CustomRanges.deleteCustomRangeRequest = "deleteCustomRange";
STRequests.CustomRanges.getCustomRangeEntryRequest = "getCustomRangeEntry";
STRequests.CustomRanges.getCustomRangeEntriesRequest = "getCustomRangeEntries";
STRequests.CustomRanges.getAllCustomRangeEntriesRequest = "getAllCustomRangeEntries";
STRequests.CustomRanges.createCustomRangeEntryRequest = "createCustomRangeEntry";
STRequests.CustomRanges.deleteCustomRangeEntryRequest = "deleteCustomRangeEntry";
STRequests.CustomRanges.updateCustomRangeEntryRequest = "updateCustomRangeEntry";
STRequests.CustomRanges.addEntryToCustomRangeRequest = "addEntryToCustomRange";
STRequests.CustomRanges.removeEntryFromCustomRangeRequest = "removeEntryFromCustomRange";

//Refactor service Request (old service name was ModifyName)
STRequests.Refactor.serviceName = "Refactor";
//STRequests.Refactor.renameRequest = "rename";
//STRequests.Refactor.replaceBaseURIRequest = "replaceBaseUri";
STRequests.Refactor.renameResourceRequest = "renameResource";
STRequests.Refactor.replaceBaseURIRequest = "replaceBaseURI";

//property service requests
STRequests.Property.serviceName = "property";
STRequests.Property.getPropertiesTreeRequest = "getPropertiesTree";
STRequests.Property.getPropertyListRequest = "getPropertyList";
STRequests.Property.getObjPropertiesTreeRequest = "getObjPropertiesTree";
STRequests.Property.getDatatypePropertiesTreeRequest = "getDatatypePropertiesTree";
STRequests.Property.getAnnotationPropertiesTreeRequest = "getAnnotationPropertiesTree";
STRequests.Property.getRootPropertyListRequest = "getRootPropertyList";
STRequests.Property.getSubPropertiesRequest = "getSubProperties";
STRequests.Property.removePropertyRequest = "removeProperty";
STRequests.Property.addPropertyRequest = "addProperty";
STRequests.Property.getRangeClassesTreeRequest = "getRangeClassesTree";
STRequests.Property.getPropertyDescriptionRequest = "getPropDescription";
STRequests.Property.removePropValueRequest = "removePropValue";
STRequests.Property.addExistingPropValueRequest = "addExistingPropValue";
STRequests.Property.createAndAddPropValueRequest = "createAndAddPropValue";
STRequests.Property.addSuperPropertyRequest = "addSuperProperty";
STRequests.Property.removeSuperPropertyRequest = "removeSuperProperty";
STRequests.Property.addPropertyDomainRequest = "addPropertyDomain";
STRequests.Property.removePropertyDomainRequest = "removePropertyDomain";
STRequests.Property.addPropertyRangeRequest = "addPropertyRange";
STRequests.Property.removePropertyRangeRequest = "removePropertyRange";
STRequests.Property.getDomainRequest = "getDomain";
STRequests.Property.getRangeRequest = "getRange";
STRequests.Property.parseDataRangeRequest = "parseDataRange";

//delete service requests
STRequests.Delete.serviceName = "delete";
STRequests.Delete.removePropertyRequest = "removeProperty";
STRequests.Delete.removeClassRequest = "removeClass";
STRequests.Delete.removeInstanceRequest = "removeInstance";

//synonym service request
STRequests.Synonyms.serviceName = "synonyms";
STRequests.Synonyms.addSynonymsRequest ="addSynonym";
STRequests.Synonyms.getSynonymsRequest ="getSynonym";

//graph service request
STRequests.Graph.serviceName = "graph";
STRequests.Graph.graphRequest = "graph";
STRequests.Graph.partialGraphRequest = "partialGraph";

//page service request
STRequests.Page.serviceName = "page";
STRequests.Page.getBookmarksRequest ="getBookmarks";

//annotation service request
STRequests.Annotation.serviceName = "annotation";
STRequests.Annotation.chkAnnotationsRequest = "chkAnnotations";
STRequests.Annotation.chkBookmarksRequest = "chkBookmarks";
STRequests.Annotation.getPageAnnotationsRequest = "getPageAnnotations";
STRequests.Annotation.createAndAnnotateRequest = "createAndAnnotate";
STRequests.Annotation.createFurtherAnnotationRequest = "addAnnotation";
STRequests.Annotation.relateAndAnnotateRequest = "relateAndAnnotate";
STRequests.Annotation.addAnnotationRequest = "addAnnotation";
STRequests.Annotation.bookmarkPageRequest = "bookmarkPage";
STRequests.Annotation.getPageTopicsRequest = "getPageTopics";
STRequests.Annotation.getBookmarksByTopicRequest = "getBookmarksByTopic";
STRequests.Annotation.removeBookmarkRequest = "removeBookmark";
STRequests.Annotation.removeAnnotationRequest = "removeAnnotation";
STRequests.Annotation.getAnnotatedContentResourcesRequest = "getAnnotatedContentResources";

//rangeannotation service request
STRequests.RangeAnnotation.serviceName = "rangeannotation";
STRequests.RangeAnnotation.chkAnnotationsRequest = "chkAnnotations";
STRequests.RangeAnnotation.getPageAnnotationsRequest = "getPageAnnotations";
STRequests.RangeAnnotation.addAnnotationRequest = "addAnnotation";
STRequests.RangeAnnotation.deleteAnnotationRequest = "deleteAnnotation";
STRequests.RangeAnnotation.getAnnotatedContentResourcesRequest = "getAnnotatedContentResources";

// individual service request
STRequests.Individual.serviceName = "individual";
STRequests.Individual.getIndividualDescriptionRequest = "getIndDescription";
STRequests.Individual.addTypeRequest = "addType";
STRequests.Individual.removeTypeRequest = "removeType";
STRequests.Individual.get_directNamedTypesRequest = "getDirectNamedTypes";

//metadata service request
STRequests.Metadata.serviceName = "metadata";
STRequests.Metadata.getOntologyDescriptionRequest = "getOntologyDescription";
STRequests.Metadata.getBaseuriRequest = "getBaseuri";
STRequests.Metadata.getDefaultNamespaceRequest = "getDefaultNamespace";
STRequests.Metadata.getImportsRequest = "getImports";
STRequests.Metadata.getNSPrefixMappingsRequest = "getNSPrefixMappings";
//STRequests.Metadata.setBaseuriDefNamespaceRequest = "setBaseuriDefNamespace";
STRequests.Metadata.setDefaultNamespaceRequest = "setDefaultNamespace";
//STRequests.Metadata.setBaseuriRequest = "setBaseuri";
STRequests.Metadata.setNSPrefixMappingRequest = "setNSPrefixMapping";
STRequests.Metadata.removeNSPrefixMappingRequest = "removeNSPrefixMapping";
STRequests.Metadata.changeNSPrefixMappingRequest = "changeNSPrefixMapping";
STRequests.Metadata.expandQNameRequest = "expandQName";
STRequests.Metadata.removeImportRequest = "removeImport";
STRequests.Metadata.addFromWebRequest = "addFromWeb";
STRequests.Metadata.addFromWebToMirrorRequest = "addFromWebToMirror";
STRequests.Metadata.addFromLocalFileRequest = "addFromLocalFile";
STRequests.Metadata.addFromOntologyMirrorRequest = "addFromOntologyMirror";
STRequests.Metadata.downloadFromWebToMirrorRequest = "downloadFromWebToMirror";
STRequests.Metadata.downloadFromWebRequest = "downloadFromWeb";
STRequests.Metadata.getFromLocalFileRequest = "getFromLocalFile";
STRequests.Metadata.mirrorOntologyRequest = "mirrorOntology";
STRequests.Metadata.getNamedGraphsRequest = "getNamedGraphs";

//metadata registry service request
STRequests.MetadataRegistry.serviceName = "MetadataRegistry";
STRequests.MetadataRegistry.addDatasetMetadataRequest = "addDatasetMetadata";
STRequests.MetadataRegistry.deleteDatasetMetadataRequest = "deleteDatasetMetadata";
STRequests.MetadataRegistry.editDatasetMetadataRequest = "editDatasetMetadata";
STRequests.MetadataRegistry.getDatasetMetadataRequest = "getDatasetMetadata";
STRequests.MetadataRegistry.listDatasetsRequest = "listDatasets";

//administartion service request
STRequests.Administration.serviceName = "administration";
STRequests.Administration.getOntologyMirrorRequest = "getOntologyMirror";
STRequests.Administration.setAdminLevelRequest = "setAdminLevel";
STRequests.Administration.deleteOntMirrorEntryRequest = "deleteOntMirrorEntry";
STRequests.Administration.updateOntMirrorEntryRequest = "updateOntMirrorEntry";
STRequests.Administration.getVersionRequest = "getVersion";

//ICV requests
STRequests.ICV.serviceName = "ICV";
STRequests.ICV.listDanglingConceptsRequest = "listDanglingConcepts";
STRequests.ICV.listCyclicConceptsRequest = "listCyclicConcepts";
STRequests.ICV.listConceptSchemesWithNoTopConceptRequest = "listConceptSchemesWithNoTopConcept";
STRequests.ICV.listConceptsWithNoSchemeRequest = "listConceptsWithNoScheme";
STRequests.ICV.listTopConceptsWithBroaderRequest = "listTopConceptsWithBroader";
STRequests.ICV.listConceptsWithSameSKOSPrefLabelRequest = "listConceptsWithSameSKOSPrefLabel";
STRequests.ICV.listConceptsWithSameSKOSXLPrefLabelRequest = "listConceptsWithSameSKOSXLPrefLabel";
STRequests.ICV.listConceptsWithOnlySKOSAltLabelRequest = "listConceptsWithOnlySKOSAltLabel";
STRequests.ICV.listConceptsWithOnlySKOSXLAltLabelRequest = "listConceptsWithOnlySKOSXLAltLabel";
STRequests.ICV.listConceptsWithNoSKOSPrefLabelRequest = "listConceptsWithNoSKOSPrefLabel";
STRequests.ICV.listConceptsWithNoSKOSXLPrefLabelRequest = "listConceptsWithNoSKOSXLPrefLabel";
STRequests.ICV.listConceptSchemesWithNoSKOSPrefLabelRequest = "listConceptSchemesWithNoSKOSPrefLabel";
STRequests.ICV.listConceptSchemesWithNoSKOSXLPrefLabelRequest = "listConceptSchemesWithNoSKOSXLPrefLabel";
STRequests.ICV.listConceptsWithMultipleSKOSPrefLabelRequest = "listConceptsWithMultipleSKOSPrefLabel";
STRequests.ICV.listConceptsWithMultipleSKOSXLPrefLabelRequest = "listConceptsWithMultipleSKOSXLPrefLabel";
STRequests.ICV.listConceptsWithNoLanguageTagSKOSLabelRequest = "listConceptsWithNoLanguageTagSKOSLabel";
STRequests.ICV.listConceptsWithNoLanguageTagSKOSXLLabelRequest = "listConceptsWithNoLanguageTagSKOSXLLabel";
STRequests.ICV.listConceptsWithOverlappedSKOSLabelRequest = "listConceptsWithOverlappedSKOSLabel";
STRequests.ICV.listConceptsWithOverlappedSKOSXLLabelRequest = "listConceptsWithOverlappedSKOSXLLabel";
STRequests.ICV.listConceptsWithExtraWhitespaceInSKOSLabelRequest = "listConceptsWithExtraWhitespaceInSKOSLabel";
STRequests.ICV.listConceptsWithExtraWhitespaceInSKOSXLLabelRequest = "listConceptsWithExtraWhitespaceInSKOSXLLabel";
STRequests.ICV.listHierarchicallyRedundantConceptsRequest = "listHierarchicallyRedundantConcepts";
STRequests.ICV.listResourcesURIWithSpaceRequest = "listResourcesURIWithSpace";

// inputoutput service request
STRequests.InputOutput.serviceName = "InputOutput";
STRequests.InputOutput.saveRDFRequest = "saveRDF";
STRequests.InputOutput.loadRDFRequest = "loadRDF";
STRequests.InputOutput.clearDataRequest = "clearData";

// SPARQL service request
STRequests.SPARQL.serviceName = "sparql";
STRequests.SPARQL.resolveQueryRequest = "resolveQuery";

// SKOS service request
STRequests.SKOS.serviceName = "skos";
// GET REQUESTS
STRequests.SKOS.getTopConceptsRequest = "getTopConcepts";
STRequests.SKOS.getNarrowerConceptsRequest = "getNarrowerConcepts";
STRequests.SKOS.getAllSchemesListRequest = "getAllSchemesList";
STRequests.SKOS.getConceptDescriptionRequest = "getConceptDescription";
STRequests.SKOS.getConceptSchemeDescriptionRequest = "getConceptSchemeDescription";
STRequests.SKOS.getPrefLabelRequest = "getPrefLabel";
STRequests.SKOS.getShowRequest = "getShow";
// ADD REQUESTS
STRequests.SKOS.addAltLabelRequest = "addAltLabel";
STRequests.SKOS.addHiddenLabelRequest = "addHiddenLabel";
STRequests.SKOS.addBroaderConceptRequest = "addBroaderConcept";
STRequests.SKOS.addTopConceptRequest = "addTopConcept";
STRequests.SKOS.addConceptToSchemeRequest = "addConceptToScheme";

// SET REQUESTS
STRequests.SKOS.setPrefLabelRequest = "setPrefLabel";
// CREATE REQUESTS
STRequests.SKOS.createConceptRequest = "createConcept";
STRequests.SKOS.createSchemeRequest = "createScheme";
// DELETE REQUESTS
STRequests.SKOS.deleteConceptRequest = "deleteConcept";
STRequests.SKOS.deleteSchemeRequest = "deleteScheme";
// REMOVE REQUESTS
STRequests.SKOS.removeBroaderConceptRequest = "removeBroaderConcept";
STRequests.SKOS.removeTopConceptRequest = "removeTopConcept";
STRequests.SKOS.removePrefLabelRequest = "removePrefLabel";
STRequests.SKOS.removeAltLabelRequest = "removeAltLabel";
STRequests.SKOS.removeHiddenLabelRequest = "removeHiddenLabel";
STRequests.SKOS.removeConceptFromSchemeRequest = "removeConceptFromScheme";


//SKOSXL service request
STRequests.SKOSXL.serviceName = "skosxl";
// GET REQUESTS
STRequests.SKOSXL.getTopConceptsRequest = "getTopConcepts";
STRequests.SKOSXL.getNarrowerConceptsRequest = "getNarrowerConcepts";
STRequests.SKOSXL.getAllSchemesListRequest = "getAllSchemesList";
STRequests.SKOSXL.getConceptDescriptionRequest = "getConceptDescription";
STRequests.SKOSXL.getConceptSchemeDescriptionRequest = "getConceptSchemeDescription";
STRequests.SKOSXL.getPrefLabelRequest = "getPrefLabel";
STRequests.SKOSXL.getShowRequest = "getShow";
// ADD REQUESTS
STRequests.SKOSXL.addBroaderConceptRequest = "addBroaderConcept";
STRequests.SKOSXL.addTopConceptRequest = "addTopConcept";
STRequests.SKOSXL.addAltLabelRequest = "addAltLabel";
STRequests.SKOSXL.addHiddenLabelRequest = "addHiddenLabel";
// SET REQUESTS
STRequests.SKOSXL.setPrefLabelRequest = "setPrefLabel";
// CREATE REQUESTS
STRequests.SKOSXL.createConceptRequest = "createConcept";
STRequests.SKOSXL.createSchemeRequest = "createScheme";
// DELETE REQUESTS
STRequests.SKOSXL.deleteConceptRequest = "deleteConcept";
STRequests.SKOSXL.deleteSchemeRequest = "deleteScheme";
// REMOVE REQUESTS
STRequests.SKOSXL.removeBroaderConceptRequest = "removeBroaderConcept";
STRequests.SKOSXL.removeTopConceptRequest = "removeTopConcept";
STRequests.SKOSXL.removePrefLabelRequest = "removePrefLabel";
STRequests.SKOSXL.removeAltLabelRequest = "removeAltLabel";
STRequests.SKOSXL.removeHiddenLabelRequest = "removeHiddenLabel";

//projects service request (OLD VERSION, this it not used anymore, so maybe it could be removed)
//STRequests.ProjectsOLD.serviceName = "projects";
//STRequests.ProjectsOLD.openProjectRequest = "openProject";
//STRequests.ProjectsOLD.repairProjectRequest = "repairProject";
//STRequests.ProjectsOLD.newProjectRequest = "newProject";
//STRequests.ProjectsOLD.newProjectFromFileRequest = "newProjectFromFile";
//STRequests.ProjectsOLD.closeProjectRequest = "closeProject";
//STRequests.ProjectsOLD.deleteProjectRequest = "deleteProject";
//STRequests.ProjectsOLD.exportProjectRequest = "exportProject";
//STRequests.ProjectsOLD.importProjectRequest = "importProject";
//STRequests.ProjectsOLD.cloneProjectRequest = "cloneProject";
//STRequests.ProjectsOLD.saveProjectAsRequest = "saveProjectAs";
//STRequests.ProjectsOLD.saveProjectRequest = "saveProject";
//STRequests.ProjectsOLD.listProjectsRequest = "listProjects";
//STRequests.ProjectsOLD.getCurrentProjectRequest = "getCurrentProject";
//STRequests.ProjectsOLD.getProjectPropertyRequest = "getProjectProperty";
//STRequests.ProjectsOLD.setProjectPropertyRequest = "setProjectProperty";
//STRequests.ProjectsOLD.isCurrentProjectActiveRequest = "isCurrentProjectActive";

//projects service request (NEW VERSION)
STRequests.Projects.serviceName = "Projects";	//NEW
STRequests.Projects.accessProjectRequest = "accessProject";	//NEW
STRequests.Projects.repairProjectRequest = "repairProject";	//NEW
STRequests.Projects.createProjectRequest = "createProject";	//NEW
STRequests.Projects.newProjectFromFileRequest = "newProjectFromFile";
STRequests.Projects.disconnectFromProjectRequest = "disconnectFromProject";	//NEW
STRequests.Projects.deleteProjectRequest = "deleteProject"; //NEW
STRequests.Projects.exportProjectRequest = "exportProject";	//NEW
STRequests.Projects.importProjectRequest = "importProject";	//NEW
STRequests.Projects.cloneProjectRequest = "cloneProject";	//NEW
STRequests.Projects.saveProjectRequest = "saveProject";	//NEW
STRequests.Projects.listProjectsRequest = "listProjects";	//NEW
STRequests.Projects.getCurrentProjectRequest = "getCurrentProject";
STRequests.Projects.setProjectPropertyRequest = "setProjectProperty";	//NEW
STRequests.Projects.getProjectPropertyRequest = "getProjectProperty";	//NEW
STRequests.Projects.getProjectPropertyMapRequest = "getProjectPropertyMap";	//NEW
STRequests.Projects.getProjectPropertyFileContentRequest = "getProjectPropertyFileContent";	//NEW
STRequests.Projects.saveProjectPropertyFileContentRequest = "saveProjectPropertyFileContent";	//NEW
STRequests.Projects.isCurrentProjectActiveRequest = "isCurrentProjectActive"; // ???
STRequests.Projects.getAccessStatusMapRequest = "getAccessStatusMap";	//NEW
STRequests.Projects.updateAccessLevelRequest = "updateAccessLevel";	//NEW
STRequests.Projects.updateLockLevelRequest = "updateLockLevel";	//NEW

//Plugins requests
STRequests.Plugins.serviceName = "Plugins";
STRequests.Plugins.getAvailablePluginsRequest = "getAvailablePlugins";
STRequests.Plugins.getPluginConfigurationsRequest = "getPluginConfigurations";

//ontManager service request
STRequests.OntManager.serviceName = "ontManager";
STRequests.OntManager.getOntManagerParametersRequest = "getOntManagerParameters";

//OntoSearch service request
STRequests.OntoSearch.serviceName = "OntoSearch";
STRequests.OntoSearch.searchOntologyRequest = "searchOntology";
STRequests.OntoSearch.getPathFromRootRequest = "getPathFromRoot";

//Statement service request
STRequests.Statement.serviceName = "statement";
STRequests.Statement.getStatementsRequest = "getStatements";
STRequests.Statement.hasStatementRequest = "hasStatement";

//Resource service request
STRequests.Resource.serviceName = "resource";
STRequests.Resource.getRoleRequest = "getRole";
STRequests.Resource.removePropertyValueRequest = "removePropertyValue";

//ResourceView service request
STRequests.ResourceView.serviceName = "ResourceView";
STRequests.ResourceView.getResourceViewRequest = "getResourceView";
STRequests.ResourceView.getLexicalizationPropertiesRequest = "getLexicalizationProperties";

//XMLSchema service request
STRequests.XMLSchema.serviceName = "XMLSchema";
STRequests.XMLSchema.formatDateTimeRequest = "formatDateTime";
STRequests.XMLSchema.formatDateRequest = "formatDate";
STRequests.XMLSchema.formatTimeRequest = "formatTime";
STRequests.XMLSchema.formatDurationRequest = "formatDuration";
STRequests.XMLSchema.formatCurrentLocalDateTimeRequest = "formatCurrentLocalDateTime";
STRequests.XMLSchema.formatCurrentUTCDateTimeRequest = "formatCurrentUTCDateTime";

//Manchester syntax service request
STRequests.Manchester.serviceName = "ManchesterHandler";
STRequests.Manchester.getAllDLExpressionRequest = "getAllDLExpression";
STRequests.Manchester.getExpressionRequest = "getExpression";
STRequests.Manchester.removeExpressionRequest = "removeExpression";
STRequests.Manchester.checkExpressionRequest = "checkExpression";
STRequests.Manchester.createRestrictionRequest = "createRestriction";
