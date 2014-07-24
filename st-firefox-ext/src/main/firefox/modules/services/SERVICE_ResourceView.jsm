Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.ResourceView;
var serviceName = service.serviceName;

const httpManager = STHttpMgrFactory.getInstance("it.uniroma2.art.semanticturkey", "st-core-services");
/**
 * Gets a resource view.
 * 
 * @member STRequests.ResourceView
 * @param resource a resource
 * @return
 */
function getResourceView(resource) {
	Logger.debug('[SERVICE_ResourceView.jsm] getResourceView');
	var resource_p = "resource=" + resource;
	
	var response = httpManager.GET(null, serviceName, service.getResourceViewRequest, this.context, resource_p);

	return response;
}

// RESOURCE VIEW SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getResourceView = getResourceView;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
