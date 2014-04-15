Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Administration;
var serviceName = service.serviceName;

/**
 * this service sets the configuration for the amount of information which is shown to the user.<br/>
 * Possible values of adminLevel are "on" or "off". Default is off<br/> With value set to "on", user is able
 * to see also the content of triples related to application ontologies (such as the annotation ontology
 * present in Semantic Turkey, or other ontologies adopted by installed Semantic Turkey extensions)
 * 
 * @member STRequests.Administration
 * @param adminLevel
 * @return
 */
function setAdminLevel(adminLevel) {
	var adminLevel = "adminLevel=" + adminLevel;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.setAdminLevelRequest, adminLevelm contextAsArray);
}

/**
 * this method returns the list of cached ontology files which replicate the content of ontologies on the web
 * 
 * @member STRequests.Administration
 * @return
 */
function getOntologyMirror() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.getOntologyMirrorRequest, contextAsArray);
}

/**
 * given the baseuri and name of cached file of an ontology in the ontology mirror, this method deletes the
 * corresponding entry on Semantic Turkey Ontology Mirror
 * 
 * @member STRequests.Administration
 * @param ns
 * @param file
 * @return
 */
function deleteOntMirrorEntry(ns, file) {
	var ns = "ns=" + ns;
	var file = "file=" + file;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.deleteOntMirrorEntryRequest, ns, file, contextAsArray);
}

/**
 * this method tells the Ontology Mirror to refresh the cached file of an ontology by downloading again the
 * RDF content of its original ontology from the web
 * 
 * @member STRequests.Administration
 * @param baseURI
 * @param mirrorFileName
 * @param srcLoc
 * @param location
 * @return
 */
function updateOntMirrorEntry(baseURI, mirrorFileName, srcLoc, location) {
	var baseURI = "baseURI=" + baseURI;
	var mirrorFileName = "mirrorFileName=" + mirrorFileName;
	var srcLoc = "srcLoc=" + srcLoc;
	var loc;

	Logger.debug("inizio updateOntMirrorEntry");

	Logger.debug("dentro updateOntMirrorEntry e location = " + location);

	if (srcLoc == "srcLoc=wbu"){
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return HttpMgr.GET(serviceName, service.updateOntMirrorEntryRequest, baseURI, mirrorFileName, srcLoc, contextAsArray);
	}
	else if (srcLoc == "srcLoc=walturl")
		loc = "altURL=" + location;
	else if (srcLoc == "srcLoc=lf")
		loc = "updateFilePath=" + location;

	Logger.debug("dentro updateOntMirrorEntry e loc = " + loc);

	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr
			.GET(serviceName, service.updateOntMirrorEntryRequest, baseURI, mirrorFileName, srcLoc, loc, contextAsArray);
}

// Administration SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.setAdminLevel = setAdminLevel;
service.prototype.getOntologyMirror = getOntologyMirror;
service.prototype.deleteOntMirrorEntry = deleteOntMirrorEntry;
service.prototype.updateOntMirrorEntry = updateOntMirrorEntry;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
