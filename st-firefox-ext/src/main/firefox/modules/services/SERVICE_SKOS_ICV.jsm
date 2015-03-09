Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.SKOS_ICV;
var serviceName = service.serviceName;

function listDanglingConcepts() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listDanglingConcepts');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listDanglingConceptsRequest, this.context);
}

function listCyclicConcepts() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listCyclicConcepts');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listCyclicConceptsRequest, this.context);
}

function listConceptSchemesWithNoTopConcept() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptSchemesWithNoTopConcept');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptSchemesWithNoTopConceptRequest, this.context);
}

function listConceptsWithNoScheme() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithNoScheme');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithNoSchemeRequest, this.context);
}

function listTopConceptsWithBroader() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listTopConceptsWithBroader');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listTopConceptsWithBroaderRequest, this.context);
}

function listConceptsWithSameSKOSPrefLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithSameSKOSPrefLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithSameSKOSPrefLabelRequest, this.context);
}

function listConceptsWithSameSKOSXLPrefLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithSameSKOSXLPrefLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithSameSKOSXLPrefLabelRequest, this.context);
}

function listConceptsWithOnlySKOSAltLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithOnlySKOSAltLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithOnlySKOSAltLabelRequest, this.context);
}

function listConceptsWithOnlySKOSXLAltLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithOnlySKOSXLAltLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithOnlySKOSXLAltLabelRequest, this.context);
}

function listConceptsWithNoLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithNoLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithNoLabelRequest, this.context);
}

function listConceptSchemesWithNoLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptSchemesWithNoLabel');	
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptSchemesWithNoLabelRequest, this.context);
}

function listConceptsWithMultipleSKOSPrefLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithMultipleSKOSPrefLabel');	
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithMultipleSKOSPrefLabelRequest, this.context);
}

function listConceptsWithMultipleSKOSXLPrefLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithMultipleSKOSXLPrefLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithMultipleSKOSXLPrefLabelRequest, this.context);
}

function listConceptsWithNoLanguageTagSKOSLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithNoLanguageTagSKOSLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithNoLanguageTagSKOSLabelRequest, this.context);
}

function listConceptsWithNoLanguageTagSKOSXLLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithNoLanguageTagSKOSXLLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithNoLanguageTagSKOSXLLabelRequest, this.context);
}

function listConceptsWithOverlappedSKOSLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithOverlappedSKOSLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithOverlappedSKOSLabelRequest, this.context);
}

function listConceptsWithOverlappedSKOSXLLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithOverlappedSKOSXLLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithOverlappedSKOSXLLabelRequest, this.context);
}

function listConceptsWithExtraWhitespaceInSKOSLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithExtraWhitespaceInSKOSLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithExtraWhitespaceInSKOSLabelRequest, this.context);
}

function listConceptsWithExtraWhitespaceInSKOSXLLabel() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listConceptsWithExtraWhitespaceInSKOSXLLabel');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listConceptsWithExtraWhitespaceInSKOSXLLabelRequest, this.context);
}

function listHierarchicallyRedundantConcepts() {
	Logger.debug('[SERVICE_SKOS_ICV.jsm] listHierarchicallyRedundantConcepts');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.listHierarchicallyRedundantConceptsRequest, this.context);
}

//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}

service.prototype.listDanglingConcepts = listDanglingConcepts;
service.prototype.listCyclicConcepts = listCyclicConcepts;
service.prototype.listConceptSchemesWithNoTopConcept = listConceptSchemesWithNoTopConcept;
service.prototype.listConceptsWithNoScheme = listConceptsWithNoScheme;
service.prototype.listTopConceptsWithBroader = listTopConceptsWithBroader;
service.prototype.listConceptsWithSameSKOSPrefLabel = listConceptsWithSameSKOSPrefLabel;
service.prototype.listConceptsWithSameSKOSXLPrefLabel = listConceptsWithSameSKOSXLPrefLabel;
service.prototype.listConceptsWithOnlySKOSAltLabel = listConceptsWithOnlySKOSAltLabel;
service.prototype.listConceptsWithOnlySKOSXLAltLabel = listConceptsWithOnlySKOSXLAltLabel;
service.prototype.listConceptsWithNoLabel = listConceptsWithNoLabel;
service.prototype.listConceptSchemesWithNoLabel = listConceptSchemesWithNoLabel;
service.prototype.listConceptsWithMultipleSKOSPrefLabel = listConceptsWithMultipleSKOSPrefLabel;
service.prototype.listConceptsWithMultipleSKOSXLPrefLabel = listConceptsWithMultipleSKOSXLPrefLabel;
service.prototype.listConceptsWithNoLanguageTagSKOSLabel = listConceptsWithNoLanguageTagSKOSLabel;
service.prototype.listConceptsWithNoLanguageTagSKOSXLLabel = listConceptsWithNoLanguageTagSKOSXLLabel;
service.prototype.listConceptsWithOverlappedSKOSLabel = listConceptsWithOverlappedSKOSLabel;
service.prototype.listConceptsWithOverlappedSKOSXLLabel = listConceptsWithOverlappedSKOSXLLabel;
service.prototype.listConceptsWithExtraWhitespaceInSKOSLabel = listConceptsWithExtraWhitespaceInSKOSLabel;
service.prototype.listConceptsWithExtraWhitespaceInSKOSXLLabel = listConceptsWithExtraWhitespaceInSKOSXLLabel;
service.prototype.listHierarchicallyRedundantConcepts = listHierarchicallyRedundantConcepts;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;