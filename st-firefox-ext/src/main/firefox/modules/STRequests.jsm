Components.utils.import("resource://stmodules/SemTurkeyHTTP.jsm");

EXPORTED_SYMBOLS = [ "HttpMgr", "STRequests"];

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
STRequests.ModifyName = function(){};    
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
STRequests.Projects = function(){};
/**
 * @class
 */
STRequests.OntManager = function(){};
/**
 * @class
 */
STRequests.Resource = function(){};


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

//ModifyName service Request
STRequests.ModifyName.serviceName = "modifyName";
STRequests.ModifyName.renameRequest = "rename";

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
STRequests.Metadata.setBaseuriDefNamespaceRequest = "setBaseuriDefNamespace";
STRequests.Metadata.setDefaultNamespaceRequest = "setDefaultNamespace";
STRequests.Metadata.setBaseuriRequest = "setBaseuri";
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
//modifyName service request
STRequests.ModifyName.serviceName = "modifyName";
STRequests.ModifyName.renameRequest = "rename";

//administartion service request
STRequests.Administration.serviceName = "administration";
STRequests.Administration.getOntologyMirrorRequest = "getOntologyMirror";
STRequests.Administration.setAdminLevelRequest = "setAdminLevel";
STRequests.Administration.deleteOntMirrorEntryRequest = "deleteOntMirrorEntry";
STRequests.Administration.updateOntMirrorEntryRequest = "updateOntMirrorEntry";

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

//projects service request
STRequests.Projects.serviceName = "projects";
STRequests.Projects.openProjectRequest = "openProject";
STRequests.Projects.repairProjectRequest = "repairProject";
STRequests.Projects.newProjectRequest = "newProject";
STRequests.Projects.newProjectFromFileRequest = "newProjectFromFile";
STRequests.Projects.closeProjectRequest = "closeProject";
STRequests.Projects.deleteProjectRequest = "deleteProject";
STRequests.Projects.exportProjectRequest = "exportProject";
STRequests.Projects.importProjectRequest = "importProject";
STRequests.Projects.cloneProjectRequest = "cloneProject";
STRequests.Projects.saveProjectAsRequest = "saveProjectAs";
STRequests.Projects.saveProjectRequest = "saveProject";
STRequests.Projects.listProjectsRequest = "listProjects";
STRequests.Projects.getCurrentProjectRequest = "getCurrentProject";
STRequests.Projects.getProjectPropertyRequest = "getProjectProperty";
STRequests.Projects.setProjectPropertyRequest = "setProjectProperty";
STRequests.Projects.isCurrentProjectActiveRequest = "isCurrentProjectActive";

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