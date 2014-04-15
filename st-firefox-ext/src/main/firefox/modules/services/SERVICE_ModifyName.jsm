Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "HttpMgr", "STRequests" ];

var service = STRequests.ModifyName;
var serviceName = service.serviceName;

/**
 * renames an existing resource
 * 
 * @member STRequests.ModifyName
 * @param oldName
 * @param newName
 * @return
 */
function rename(oldName,newName){
	var oldName_p = "oldName=" + oldName;
	var newName_p = "newName=" + newName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = HttpMgr.GET(serviceName, service.renameRequest, oldName_p, newName_p, contextAsArray);
	
	if (!reply.isFail()) {
		evtMgr.fireEvent("resourceRenamed", {getOldName : function(){return oldName;}, getNewName : function(){return newName;}});
	}

	return reply;
}
//ModifyName SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.rename = rename;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;