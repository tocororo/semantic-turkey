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
STRequests.Delete = function(){};        
/**
 * @class
 */
STRequests.Graph = function(){};         
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
STRequests.SKOS_ICV = function(){};
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


//systemStart service requests
STRequests.SystemStart.serviceName = "systemStart";
STRequests.SystemStart.startRequest = "start";
STRequests.SystemStart.listTripleStoresRequest = "listTripleStores";

//cls service requests
STRequests.Cls.serviceName = "cls";
STRequests.Cls.addTypeRequest = "addType";
STRequests.Cls.removeTypeRequest = "removeType";
STRequests.Cls.removeTypeRequest = "removeType";
STRequests.Cls.addSuperClsRequest = "addSuperCls";
STRequests.Cls.removeSuperClsRequest = "removeSuperCls";
STRequests.Cls.getClassTreeRequest = "getClassTree";
STRequests.Cls.getClassDescriptionRequest = "getClsDescription";
STRequests.Cls.getClassAndInstancesInfoRequest = "getClassAndInstancesInfo";
STRequests.Cls.createInstanceRequest = "createInstance";
STRequests.Cls.createClassRequest = "createClass";
STRequests.Cls.getSubClassesRequest = "getSubClasses";
STRequests.Cls.getSuperClassesRequest = "getSuperClasses";
STRequests.Cls.getClassesInfoAsRootsForTreeRequest = "getClassesInfoAsRootsForTree";

//Refactor service Request (old service name was ModifyName)
STRequests.Refactor.serviceName = "Refactor";
STRequests.Refactor.renameRequest = "rename";
STRequests.Refactor.replaceBaseURIRequest = "replaceBaseUri";



//property service requests
STRequests.Property.serviceName = "property";
STRequests.Property.getPropertiesTreeRequest = "getPropertiesTree";
STRequests.Property.getObjPropertiesTreeRequest = "getObjPropertiesTree";
STRequests.Property.getDatatypePropertiesTreeRequest = "getDatatypePropertiesTree";
STRequests.Property.getAnnotationPropertiesTreeRequest = "getAnnotationPropertiesTree";
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

//administartion service request
STRequests.Administration.serviceName = "administration";
STRequests.Administration.getOntologyMirrorRequest = "getOntologyMirror";
STRequests.Administration.setAdminLevelRequest = "setAdminLevel";
STRequests.Administration.deleteOntMirrorEntryRequest = "deleteOntMirrorEntry";
STRequests.Administration.updateOntMirrorEntryRequest = "updateOntMirrorEntry";
STRequests.Administration.getVersionRequest = "getVersion";

// inputoutput service request
STRequests.InputOutput.serviceName = "inputOutput";
STRequests.InputOutput.saveRepositoryRequest = "saveRDF";
STRequests.InputOutput.loadRepositoryRequest = "loadRDF";
STRequests.InputOutput.clearRepositoryRequest = "clearData";

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

//SKOS_ICV requests
STRequests.SKOS_ICV.serviceName = "SKOS_ICV";
STRequests.SKOS_ICV.listDanglingConceptsRequest = "listDanglingConcepts";
STRequests.SKOS_ICV.listCyclicConceptsRequest = "listCyclicConcepts";
STRequests.SKOS_ICV.listConceptSchemesWithNoTopConceptRequest = "listConceptSchemesWithNoTopConcept";
STRequests.SKOS_ICV.listConceptsWithNoSchemeRequest = "listConceptsWithNoScheme";
STRequests.SKOS_ICV.listTopConceptsWithBroaderRequest = "listTopConceptsWithBroader";
STRequests.SKOS_ICV.listConceptsWithSameSKOSPrefLabelRequest = "listConceptsWithSameSKOSPrefLabel";
STRequests.SKOS_ICV.listConceptsWithSameSKOSXLPrefLabelRequest = "listConceptsWithSameSKOSXLPrefLabel";
STRequests.SKOS_ICV.listConceptsWithOnlySKOSAltLabelRequest = "listConceptsWithOnlySKOSAltLabel";
STRequests.SKOS_ICV.listConceptsWithOnlySKOSXLAltLabelRequest = "listConceptsWithOnlySKOSXLAltLabel";
STRequests.SKOS_ICV.listConceptsWithNoLabelRequest = "listConceptsWithNoLabel";
STRequests.SKOS_ICV.listConceptSchemesWithNoLabelRequest = "listConceptSchemesWithNoLabel";
STRequests.SKOS_ICV.listConceptsWithMultipleSKOSPrefLabelRequest = "listConceptsWithMultipleSKOSPrefLabel";
STRequests.SKOS_ICV.listConceptsWithMultipleSKOSXLPrefLabelRequest = "listConceptsWithMultipleSKOSXLPrefLabel";
STRequests.SKOS_ICV.listConceptsWithNoLanguageTagSKOSLabelRequest = "listConceptsWithNoLanguageTagSKOSLabel";
STRequests.SKOS_ICV.listConceptsWithNoLanguageTagSKOSXLLabelRequest = "listConceptsWithNoLanguageTagSKOSXLLabel";
STRequests.SKOS_ICV.listConceptsWithOverlappedSKOSLabelRequest = "listConceptsWithOverlappedSKOSLabel";
STRequests.SKOS_ICV.listConceptsWithOverlappedSKOSXLLabelRequest = "listConceptsWithOverlappedSKOSXLLabel";
STRequests.SKOS_ICV.listConceptsWithExtraWhitespaceInSKOSLabelRequest = "listConceptsWithExtraWhitespaceInSKOSLabel";
STRequests.SKOS_ICV.listConceptsWithExtraWhitespaceInSKOSXLLabelRequest = "listConceptsWithExtraWhitespaceInSKOSXLLabel";

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


//ontManager service request
STRequests.OntManager.serviceName = "ontManager";
STRequests.OntManager.getOntManagerParametersRequest = "getOntManagerParameters";

//OntoSearch service request
STRequests.OntoSearch.serviceName = "OntoSearch";
STRequests.OntoSearch.searchOntologyRequest = "searchOntology";

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