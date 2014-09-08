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

//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}

service.prototype.listDanglingConcepts = listDanglingConcepts;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;