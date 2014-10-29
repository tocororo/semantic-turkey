Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Metadata;
var serviceName = service.serviceName;


/**
 * returns the description of resource representing the current ontology
 * 
 * @member STRequests.Metadata
 * @return
 */
function getOntologyDescription() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getOntologyDescriptionRequest, contextAsArray);
}

/**
 * returns the baseuri of the current ontology
 * 
 * @member STRequests.Metadata
 * @return
 */
function getBaseuri() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getBaseuriRequest, contextAsArray);
}

/**
 * returns the default namespace of the current ontology
 * 
 * @member STRequests.Metadata
 * @return
 */
function getDefaultNamespace() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getDefaultNamespaceRequest, contextAsArray);
}

/**
 * returns a tree forest representing current ontology imports. The roots of this forest list ontologies
 * directly imported from the current ontology, while subsequent nodes represent recursive imports
 * 
 * @member STRequests.Metadata
 * @return
 */
function getImports() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getImportsRequest, contextAsArray);
}

/**
 * returns a list of mappings between prefixes and namespaces. Also, for each entry it is told whether this
 * mapping has been provided by the system (because it has been automatically guessed from the namespace name,
 * or because it is a mapping specified in one of the imported ontologies) or if has been explictly setup by
 * the user
 * 
 * @member STRequests.Metadata
 * @return
 */
function getNSPrefixMappings() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getNSPrefixMappingsRequest, contextAsArray);
}

/**
 * returns the list of named graphs for the current ontology. By default, Semantic Turkey associates named
 * graphs to triples of imported ontologies, as well as to <code>application</code> and <code>support</code>
 * ontologies
 * 
 * @member STRequests.Metadata
 * @return
 */
function getNamedGraphs() {
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getNamedGraphsRequest, contextAsArray);
}

/**
 * sets both baseuri and default namespace for the ontology of the current project
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param namespace
 * @return
 */
/*function setBaseuriDefNamespace(baseuri, namespace) {
	var baseuri = "baseuri=" + baseuri;
	var namespace = "namespace=" + namespace;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.setBaseuriDefNamespaceRequest, baseuri, namespace, contextAsArray);
}*/

/**
 * sets the default namespace for the ontology of the current project
 * 
 * @member STRequests.Metadata
 * @param namespace
 * @return
 */
function setDefaultNamespace(namespace) {
	var namespace = "namespace=" + namespace;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.setDefaultNamespaceRequest, namespace, contextAsArray);
}

/**
 * sets the baseuri for the ontology of the current project
 * 
 * @member STRequests.Metadata
 * @param uri
 * @return
 */
/*function setBaseuri(uri) {
	var uri = "baseuri=" + uri;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.setBaseuriRequest, uri, contextAsArray);
}*/

/**
 * adds a mapping between a namespace and a chosen prefix
 * 
 * @member STRequests.Metadata
 * @param prefix
 * @param namespace
 * @return
 */
function setNSPrefixMapping(prefix, namespace) {
	var prefix = "prefix=" + prefix;
	var namespace = "namespace=" + namespace;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.setNSPrefixMappingRequest, prefix, namespace, contextAsArray);
}

/**
 * deletes a mapping from the prefix-namespace mapping table
 * 
 * @member STRequests.Metadata
 * @param namespace
 * @return
 */
function removeNSPrefixMapping(namespace) {
	var namespace = "namespace=" + namespace;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.removeNSPrefixMappingRequest, namespace, contextAsArray);
}

/**
 * overwrites an entry in the prefix-namespace mapping table
 * 
 * @member STRequests.Metadata
 * @param prefix
 * @param namespace
 * @return
 */
function changeNSPrefixMapping(prefix, namespace) {
	var prefix = "prefix=" + prefix;
	var namespace = "namespace=" + namespace;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.changeNSPrefixMappingRequest, prefix, namespace, contextAsArray);
}

/**
 * removes an import from the ontology of the current project. This operation deletes the named graph
 * associated to this import and the triples associated to the named graph
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @return
 */
function removeImport(baseuri) {
	var baseuri = "baseuri=" + baseuri;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.removeImportRequest, baseuri, contextAsArray);
}

/**
 * imports an ontology from the Web. The imported ontology is considered to be always available from the Web,
 * so it is always rrefreshed each time the working ontology is being reloaded
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param alturl
 *            if the baseuri is different from the phisical location of the ontology, then alturl specifies
 *            where to look for the file to be downloaded
 * @return
 */
function addFromWeb(baseuri, alturl, format) {
	var baseuri = "baseuri=" + baseuri;
	var alturlToSend = null;
	if(typeof alturl != "undefined" && alturl != null){
		alturlToSend = "alturl=" + alturl;
	}
	var formatToSend = null;
	if(typeof format != "undefined" && format != null){
		formatToSend = "rdfFormat=" + format;
	}
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	
	if(alturlToSend != null && formatToSend != null){
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebRequest, baseuri, alturlToSend,
				formatToSend, contextAsArray);
	} else if(alturlToSend != null && formatToSend == null){
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebRequest, baseuri, alturlToSend,
				contextAsArray);
	} else if(alturlToSend == null && formatToSend != null) {
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebRequest, baseuri, formatToSend, 
				contextAsArray);
	} else {
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebRequest, baseuri, contextAsArray);
	}
}

/**
 * imports an ontology from the Web to a local cache. The imported ontology is cached in a local file in
 * Semantic Turkey's ontologies mirror location
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param mirrorFile
 *            the name of the cache file which is saved locally in the ontologies mirror of Semantic Turkey
 * @param alturl
 *            if the baseuri is different from the phisical location of the ontology, then alturl specifies
 *            where to look for the file to be downloaded
 * @return
 */
function addFromWebToMirror(baseuri, mirrorFile, alturl, format) {
	var baseuri = "baseuri=" + baseuri;
	var mirrorFile = "mirrorFile=" + mirrorFile;
	if(typeof alturl != "undefined" && alturl != null){
		alturlToSend = "alturl=" + alturl;
	}
	var formatToSend = null;
	if(typeof format != "undefined" && format != null){
		formatToSend = "rdfFormat=" + format;
	}
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	
	if(alturlToSend != null && formatToSend != null){
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebToMirrorRequest, baseuri, mirrorFile, 
				alturlToSend, formatToSend, contextAsArray);
	} else if(alturlToSend != null && formatToSend == null){
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebToMirrorRequest, baseuri, mirrorFile, 
				alturlToSend, contextAsArray);
	} else if(alturlToSend == null && formatToSend != null) {
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebToMirrorRequest, baseuri, mirrorFile, 
				formatToSend, contextAsArray);
	} else {
		return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromWebToMirrorRequest, baseuri, mirrorFile, 
				contextAsArray);
	}
}

/**
 * imports an ontology from a file in the local file system. The imported ontology is cached in a local file
 * in Semantic Turkey's ontologies mirror location
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param localFilePath
 *            the path to the source file which is imported in the current ontology
 * @param mirrorFile
 *            the name of the cache file which is saved locally in the ontologies mirror of Semantic Turkey
 * @return
 */
function addFromLocalFile(baseuri, localFilePath, mirrorFile) {
	var baseuri = "baseuri=" + baseuri;
	var localFilePath = "localFilePath=" + localFilePath;
	var mirrorFile = "mirrorFile=" + mirrorFile;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromLocalFileRequest, baseuri, localFilePath, mirrorFile, contextAsArray);
}

/**
 * imports an ontology by taking its content directly from the ontologies mirror of Semantic Turkey
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param mirrorFile
 *            the name of the cache file which is imported into current ontology
 * @return
 */
function addFromOntologyMirror(baseuri, mirrorFile) {
	baseuri = "baseuri=" + baseuri;
	mirrorFile = "mirrorFile=" + mirrorFile;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.addFromOntologyMirrorRequest, baseuri, mirrorFile, contextAsArray);
}

/**
 * as for addFromWebToMirror, excepts that this does not add an owl:imports statement to the current ontology.
 * It is mainly used to recover failed downloads of the content of ontologies which have already been declared
 * to be imported
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param mirrorFile
 * @param alturl
 * @return
 */
function downloadFromWebToMirror(baseuri, mirrorFile, alturl) {
	var baseuri = "baseuri=" + baseuri;
	var mirrorFile = "mirrorFile=" + mirrorFile;
	if (typeof alturl != "undefined") {
		var alturl = "alturl=" + alturl;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.downloadFromWebToMirrorRequest, baseuri, mirrorFile, alturl, contextAsArray);
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.downloadFromWebToMirrorRequest, baseuri, mirrorFile, contextAsArray);
	}
}

/**
 * as for addFromWeb, excepts that this does not add an owl:imports statement to the current ontology. It is
 * mainly used to recover failed downloads of the content of ontologies which have already been declared to be
 * imported
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param alturl
 * @return
 */
function downloadFromWeb(baseuri, alturl) {
	var baseuri = "baseuri=" + baseuri;
	if (typeof alturl != "undefined") {
		var alturl = "alturl=" + alturl;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.downloadFromWebRequest, baseuri, alturl, contextAsArray);
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.downloadFromWebRequest, baseuri, contextAsArray);
	}
}

/**
 * as for addFromLocalFile, excepts that this does not add an owl:imports statement to the current ontology.
 * It is mainly used to recover failed downloads of the content of ontologies which have already been declared
 * to be imported
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param localFilePath
 * @param mirrorFile
 * @param alturl
 * @return
 */
function getFromLocalFile(baseuri, localFilePath, mirrorFile, alturl) {
	var baseuri = "baseuri=" + baseuri;
	var localFilePath = "localFilePath=" + localFilePath;
	var mirrorFile = "mirrorFile=" + mirrorFile;
	if (typeof alturl != "undefined") {
		var alturl = "alturl=" + alturl;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.getFromLocalFileRequest, baseuri, localFilePath, mirrorFile,
				alturl, contextAsArray);
	} else {
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		return SemTurkeyHTTPLegacy.GET(serviceName, service.getFromLocalFileRequest, baseuri, localFilePath, mirrorFile,
				contextAsArray);
	}
}

/**
 * mirrors an ontology which has already been imported
 * 
 * @member STRequests.Metadata
 * @param baseuri
 * @param mirrorFile
 * @return
 */
function mirrorOntology(baseuri, mirrorFile) {
	baseuri = "baseuri=" + baseuri;
	mirrorFile = "mirrorFile=" + mirrorFile;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.mirrorOntologyRequest, baseuri, mirrorFile, contextAsArray);
}


// Annotation SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getOntologyDescription = getOntologyDescription;
service.prototype.getBaseuri = getBaseuri;
service.prototype.getDefaultNamespace = getDefaultNamespace;
service.prototype.getImports = getImports;
service.prototype.getNSPrefixMappings = getNSPrefixMappings;
//service.prototype.setBaseuriDefNamespace = setBaseuriDefNamespace;
service.prototype.setDefaultNamespace = setDefaultNamespace;
//service.prototype.setBaseuri = setBaseuri;
service.prototype.setNSPrefixMapping = setNSPrefixMapping;
service.prototype.removeNSPrefixMapping = removeNSPrefixMapping;
service.prototype.changeNSPrefixMapping = changeNSPrefixMapping;
service.prototype.removeImport = removeImport;
service.prototype.addFromWeb = addFromWeb;
service.prototype.addFromWebToMirror = addFromWebToMirror;
service.prototype.addFromLocalFile = addFromLocalFile;
service.prototype.addFromOntologyMirror = addFromOntologyMirror;
service.prototype.downloadFromWebToMirror = downloadFromWebToMirror;
service.prototype.downloadFromWeb = downloadFromWeb;
service.prototype.getFromLocalFile = getFromLocalFile;
service.prototype.mirrorOntology = mirrorOntology;
service.prototype.getNamedGraphs = getNamedGraphs;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;