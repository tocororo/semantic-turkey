Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = ["STRequests"];

var service = STRequests.OntManager;
var serviceName = service.serviceName;


/**
 * returns all the parameters (and theirs value) associated to all the configurations
 * of the triple store <code>ontMgrID</code>
 * 
 * @member STRequests.OntManager
 * @param ontMgrID
 * @return
 */
function getOntManagerParameters(ontMgrID){
	var ontMgrID = "ontMgrID="+ontMgrID;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getOntManagerParametersRequest,ontMgrID, contextAsArray);
}

//OntManager SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getOntManagerParameters = getOntManagerParameters;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;