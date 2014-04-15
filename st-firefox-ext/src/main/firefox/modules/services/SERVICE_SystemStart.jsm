Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "HttpMgr", "STRequests" ];

var service = STRequests.SystemStart;
var serviceName = service.serviceName;

/**
 * asks Semantic Turkey to start. This involves creation of the main project and setting of its
 * characteristic, if it has not already been setup<br/> <em>baseuri</em> and <em>ontmanager</em> are
 * optional. If baseuri is not specified, Semantic Turkey tries to get it from the current main project. If
 * ontmanager is not specified, Semantic Turkey tries to get it from the current project or to get the
 * currently available one if there is only one, otherwise, an exception is being thrown
 * 
 * @member STRequests.SystemStart
 * @param baseuri
 *            (optional) If not specified, Semantic Turkey tries to get it from the current main project or it
 *            throws an exception
 * @param ontmanager
 *            (optional) If not specified, Semantic Turkey tries to get it from the current project or to get
 *            the currently available one if there is only one, otherwise, an exception is being thrown
 * @return
 */
function start(baseuri, ontmanager) {
	Logger.debug('[SERVICE_SystemStart.jsm] baseuri: ' + baseuri + '  | ontmanager: ' + ontmanager);
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	if ((typeof baseuri != "undefined") && (typeof ontmanager != "undefined")) {
		var baseuri = "baseuri=" + baseuri;
		var ontmanager = "ontmanager=" + ontmanager;
		return HttpMgr.GET(serviceName, service.startRequest, baseuri, ontmanager, contextAsArray);
	} else
		return HttpMgr.GET(serviceName, service.startRequest, contextAsArray);
}

/**
 * gets the list of ontology manager implementations (those Semantic Turkey extensions with an OSGi service
 * implementing the <code>STOntologyManager</code> interface)
 * 
 * @member STRequests.SystemStart
 * @return
 */
function listOntManagers() {
	Logger.debug('[SERVICE_SystemStart.jsm] listOntManager');
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.listTripleStoresRequest, contextAsArray);
}

// SystemStart SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.start = start;
service.prototype.listOntManagers = listOntManagers;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;