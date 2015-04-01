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
	return SemTurkeyHTTPLegacy.GET(serviceName, service.setAdminLevelRequest, adminLevel, contextAsArray);
}

/**
 * this method returns the list of cached ontology files which replicate the content of ontologies on the web
 * 
 * @member STRequests.Administration
 * @return
 */
function getOntologyMirror() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getOntologyMirrorRequest, contextAsArray);
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
	return SemTurkeyHTTPLegacy.GET(serviceName, service.deleteOntMirrorEntryRequest, ns, file, contextAsArray);
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
	var p_baseURI = "baseURI=" + baseURI;
	var p_mirrorFileName = "mirrorFileName=" + mirrorFileName;
	var p_srcLoc = "srcLoc=" + srcLoc;

	Logger.debug("inizio updateOntMirrorEntry");
	Logger.debug("dentro updateOntMirrorEntry e location = " + location);

	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	if (srcLoc == "wbu"){
		return SemTurkeyHTTPLegacy.GET(serviceName, service.updateOntMirrorEntryRequest, p_baseURI, p_mirrorFileName, p_srcLoc, contextAsArray);
	} else if (srcLoc == "walturl"){
		var p_location = "altURL=" + location;
		return SemTurkeyHTTPLegacy
				.GET(serviceName, service.updateOntMirrorEntryRequest, p_baseURI, p_mirrorFileName, p_srcLoc, p_location, contextAsArray);
	}
}

/**
 * this method tells the Ontology Mirror to refresh the cached file of an ontology by downloading again the
 * RDF content of its original ontology from a local file
 * 
 * @member STRequests.Administration
 * @param baseURI
 * @param mirrorFileName
 * @param localFile
 * @return
 */
function updateOntMirrorEntryFromLocalFile(baseURI, mirrorFileName, localFile){
	var formData = Components.classes["@mozilla.org/files/formdata;1"]
		.createInstance(Components.interfaces.nsIDOMFormData);
	formData.append("baseURI", baseURI);
	formData.append("localFile", localFile);
	formData.append("mirrorFileName", mirrorFileName);
	formData.append("srcLoc", "lf");
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.POST(serviceName, service.updateOntMirrorEntryRequest, formData, contextAsArray);
}

/**
 * this method get the version from the server
 */
function getVersion(){
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy
		.GET(serviceName, service.getVersionRequest, contextAsArray);
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
service.prototype.updateOntMirrorEntryFromLocalFile = updateOntMirrorEntryFromLocalFile;
service.prototype.getVersion = getVersion;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
