Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Resource;
var serviceName = service.serviceName;

const httpManager = STHttpMgrFactory.getInstance("it.uniroma2.art.semanticturkey", "st-core-services");

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
	
	var response = SemTurkeyHTTPLegacy.GET(serviceName, service.getRoleRequest,resource_p, contextAsArray);
	return response.getElementsByTagName("value")[0].textContent.trim();
}

function removePropertyValue(subject, predicate, object) {
	var subject_p = "subject=" + subject;
	var predicate_p = "predicate=" + predicate;
	var object_p = "object=" + object;
	
	httpManager.GET(null, "Resource", service.removePropertyValueRequest, this.context, subject_p, predicate_p, object_p);
}

// RESOURCE SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getRole = getRole;
service.prototype.removePropertyValue=removePropertyValue;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
