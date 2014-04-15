Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Resource;
var serviceName = service.serviceName;

/**
 * Gets a resource role.
 * 
 * @member STRequests.Resource
 * @param resource a resource
 * @return
 */
function getRole(resource) {
	Logger.debug('[SERVICE_Resource.jsm] getRole');
	var resource_p = "resource=" + resource;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	
	var response = HttpMgr.GET(serviceName, service.getRoleRequest,resource_p, contextAsArray);
	return response.getElementsByTagName("value")[0].textContent.trim();
}

// RESOURCE SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getRole = getRole;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
