Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.InputOutput;
var serviceName = service.serviceName;


/**
 * saves an RDF file with the content of current ontology.<br/>
 * The chosen extension for the file determines the format for the output
 * 
 * @member STRequests.InputOutput
 * @param file
 * @return
 */
function saveRepository(file){
	var file = "file="+file;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.saveRepositoryRequest, file, contextAsArray);
}

/**
 * loads data from an RDF file. The extension of the file determines the format for reading data
 * 
 * @member STRequests.InputOutput
 * @param file
 * @param baseUri
 * @return
 */
function loadRepository(file, baseUri){
	var file = "file="+file;
	var baseUri = "baseUri="+baseUri;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.loadRepositoryRequest, file, baseUri, contextAsArray);
}

/**
 * clears all data in current ontology. It leaves only baseuri and namespace unchanged
 * 
 * @member STRequests.InputOutput
 * @return
 */
function clearRepository(){
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.clearRepositoryRequest, contextAsArray);
}


//InputOutput SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.saveRepository = saveRepository;
service.prototype.loadRepository = loadRepository;
service.prototype.clearRepository = clearRepository;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;

