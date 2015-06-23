Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Plugins;
var serviceName = service.serviceName;

function getAvailablePlugins(extensionPoint){
	Logger.debug('[SERVICE_Plugins.jsm] getAvailablePlugins');
	extensionPoint = "extensionPoint=" + extensionPoint;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.getAvailablePluginsRequest, this.context, extensionPoint);
}

function getPluginConfigurations(factoryID) {
	Logger.debug('[SERVICE_Plugins.jsm] getPluginConfigurations');
	factoryID = "factoryID=" + factoryID;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.getPluginConfigurationsRequest, this.context, factoryID);
}

service.prototype.getAvailablePlugins = getAvailablePlugins;
service.prototype.getPluginConfigurations = getPluginConfigurations;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;