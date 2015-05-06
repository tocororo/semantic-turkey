Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.InputOutput;
var serviceName = service.serviceName;

const currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());

/**
 * saves an RDF file with the content of current ontology.<br/>
 * The chosen extension for the file determines the format for the output
 * 
 * @member STRequests.InputOutput
 * @param file
 * @return
 */
function saveRDF(format){
	Logger.debug('[SERVICE_InputOutput.jsm] saveRDF');
	var format = "format="+format;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	//get and open directly the request url that returns the file in the response
	var target = currentSTHttpMgr.getRequestUrl(serviceName, service.saveRDFRequest, this.context, format);
	var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
    	.getService(Components.interfaces.nsIWindowMediator);
	var mainWindow = wm.getMostRecentWindow("navigator:browser");
	mainWindow.openDialog(target, "_blank");
}

/**
 * loads data from an RDF file. The extension of the file determines the format for reading data
 * 
 * @member STRequests.InputOutput
 * @param file
 * @param baseUri
 * @param format
 * @return
 */
function loadRDF(file, baseUri, format){
	Logger.debug('[SERVICE_InputOutput.jsm] loadRDF');
	var formData = Components.classes["@mozilla.org/files/formdata;1"]
		.createInstance(Components.interfaces.nsIDOMFormData);
	formData.append("inputFile", file);
	formData.append("baseUri", baseUri);
	if(typeof format != 'undefined'){
		formData.append("formatName", format);
	}
	return currentSTHttpMgr.POST(null, serviceName, service.loadRDFRequest, this.context, formData);
}

/**
 * clears all data in current ontology. It leaves only baseuri and namespace unchanged
 * 
 * @member STRequests.InputOutput
 * @return
 */
function clearData(){
	Logger.debug('[SERVICE_InputOutput.jsm] clearData');
	return currentSTHttpMgr.GET(null, serviceName, service.clearDataRequest, this.context);
}


//InputOutput SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.saveRDF = saveRDF;
service.prototype.loadRDF = loadRDF;
service.prototype.clearData = clearData;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;

