Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");

Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.ResourceView;
var serviceName = service.serviceName;

const
httpManager = STHttpMgrFactory.getInstance("it.uniroma2.art.semanticturkey",
		"st-core-services");
/**
 * Gets a resource view.
 * 
 * @member STRequests.ResourceView
 * @param resource
 *            a resource
 * @param resourcePosition
 *            the resource position (optional)
 * @return
 */
function getResourceView(resource, resourcePosition) {
	Logger.debug('[SERVICE_ResourceView.jsm] getResourceView');
	var resource_p = "resource=" + resource;
	var resourcePosition_p = typeof resourcePosition != "undefined" && resourcePosition != null ? "resourcePosition=" + resourcePosition : "";

	var response = httpManager.GET(null, serviceName,
			service.getResourceViewRequest, this.context, resource_p, resourcePosition_p);

	return response;
}

/**
 * Gets the list of known lexicalization properties. If the parameter
 * <code>resource</code> is bound to a value, then only properties applicable
 * to that resource are returned.
 * 
 * @member STRequests.ResourceView
 * @param resource
 *            a resource
 * @return
 */
function getLexicalizationProperties(resource) {
	Logger.debug('[SERVICE_ResourceView.jsm] getLexicalizationProperties');

	var response;

	if (typeof resource != "undefined") {
		var resource_p = "resource=" + resource;
		response = httpManager.GET(null, serviceName,
				service.getLexicalizationPropertiesRequest, this.context, resource_p);
	} else {
		response = httpManager.GET(null, serviceName,
				service.getLexicalizationPropertiesRequest, this.context);
	}

	return Deserializer.createRDFArray(response);
}

// RESOURCE VIEW SERVICE INITIALIZATION
// this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext) {
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getResourceView = getResourceView;
service.prototype.getLexicalizationProperties = getLexicalizationProperties;
service.prototype.context = new Context(); // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
