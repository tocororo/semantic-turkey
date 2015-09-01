Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.MetadataRegistry;
var serviceName = service.serviceName;

const currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());

/**
 * adds the metadata about a dataset identified by the given <code>baseURI</code>.
 * 
 * @member STRequests.MetadataRegistry
 * @return
 */
function addDatasetMetadata(baseURI, title, sparqlEndpoint, dereferenceable) {
	var baseURI_p = "baseURI=" + baseURI;
	var title_p = "title=" + title;
	var sparqlEndpoint_p = "sparqlEndpoint=" + sparqlEndpoint;
	var dereferenceable_p = "dereferenceable=" + dereferenceable;
	
	var response = currentSTHttpMgr.GET(null, serviceName, service.addDatasetMetadataRequest, this.context, baseURI_p, title_p, sparqlEndpoint_p, dereferenceable_p);
	return response;
}

/**
 * Edits the metadata about the dataset identified by the given <code>baseURI</code>.
 * 
 * @member STRequests.MetadataRegistry
 * @return
 */
function editDatasetMetadata(baseURI, newBaseURI, newTitle, newSparqlEndpoint, newDereferenceable) {
	var baseURI_p = "baseURI=" + baseURI;
	var newBaseURI_p = "newBaseURI=" + newBaseURI;
	var newTitle_p = "newTitle=" + newTitle;
	var newSparqlEndpoint_p = "newSparqlEndpoint=" + newSparqlEndpoint;
	var newDereferenceable_p = "newDereferenceable=" + newDereferenceable;
	
	var response = currentSTHttpMgr.GET(null, serviceName, service.editDatasetMetadataRequest, this.context, baseURI_p, newBaseURI_p, newTitle_p, newSparqlEndpoint_p, newDereferenceable_p);
	return response;
}

/**
 * Returns the metadata about the dataset identified by the given <code>baseURI</code>.
 * 
 * @member STRequests.MetadataRegistry
 * @return
 */
function getDatasetMetadata(baseURI) {
	var baseURI_p = "baseURI=" + baseURI;
	
	var response = currentSTHttpMgr.GET(null, serviceName, service.getDatasetMetadataRequest, this.context, baseURI_p);
	
	var result = {};
	
	var datasetMetadataElement = response.getElementsByTagName("datasetMetadata")[0];
	
	result.baseURI = datasetMetadataElement.getAttribute("baseURI");
	result.title = response.evaluate("title/text()", datasetMetadataElement, null, Components.interfaces.nsIDOMXPathResult.STRING_TYPE, null).stringValue;
	result.sparqlEndpoint = response.evaluate("sparqlEndpoint/text()", datasetMetadataElement, null, Components.interfaces.nsIDOMXPathResult.STRING_TYPE, null).stringValue;
	result.dereferenceable = response.evaluate("dereferenceable/text()", datasetMetadataElement, null, Components.interfaces.nsIDOMXPathResult.STRING_TYPE, null).stringValue == "true";

	return result;
}

/**
 * Deletes the metadata about the dataset identified by the given <code>baseURI</code>.
 * 
 * @member STRequests.MetadataRegistry
 * @return
 */
function deleteDatasetMetadata(baseURI) {
	var baseURI_p = "baseURI=" + baseURI;
	
	var response = currentSTHttpMgr.GET(null, serviceName, service.deleteDatasetMetadataRequest, this.context, baseURI_p);
	return response;
}

/**
 * returns the list of datasets in the "metadata registry"
 * 
 * @member STRequests.MetadataRegistry
 * @return
 */
function listDatasets() {
	var response = currentSTHttpMgr.GET(null, serviceName, service.listDatasetsRequest, this.context);
	
	var datasetElementList = response.getElementsByTagName("dataset");
	
	var result = new Array(datasetElementList.length);
	
	for (var i = 0 ; i < datasetElementList.length ; i++) {
		var datasetElement = datasetElementList[i];
		
		result[i] = {};
		
		result[i].baseURI = datasetElement.getAttribute("baseURI");
		result[i].title = datasetElement.getAttribute("title") || null;
	}
	
	return result;
}

service.prototype.addDatasetMetadata = addDatasetMetadata;
service.prototype.deleteDatasetMetadata = deleteDatasetMetadata;
service.prototype.editDatasetMetadata = editDatasetMetadata;
service.prototype.getDatasetMetadata = getDatasetMetadata;
service.prototype.listDatasets = listDatasets;

service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;