Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Refactor;
var serviceName = service.serviceName;

/**
 * renames an existing resource
 * 
 * @member STRequests.Refactor
 * @param oldName
 * @param newName
 * @return
 */
function rename(oldName,newName){
	Logger.debug('[SERVICE_Refactor.jsm] rename '+oldName+" "+newName);
	var oldName_p = "oldName=" + oldName;
	var newName_p = "newName=" + newName;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.renameRequest, oldName_p, newName_p, contextAsArray);
	
	if (!reply.isFail()) {
		evtMgr.fireEvent("resourceRenamed", {getOldName : function(){return oldName;}, getNewName : function(){return newName;}});
	}

	return reply;
}

function replaceBaseURI(newBaseUri, oldBaseUri){
	Logger.debug('[SERVICE_Refactor.jsm] replaceBaseURI '+newBaseUri+" "+oldBaseUri);
	var targetBaseURI = "targetBaseURI="+newBaseUri;
	var sourceBaseURI = null;
	if(typeof oldBaseUri != 'undefined'&& oldBaseUri != null){
		sourceBaseURI = "sourceBaseURI="+oldBaseUri;
	}
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply;
	if(sourceBaseURI!= null) {
		reply = SemTurkeyHTTPLegacy.GET(serviceName, service.replaceBaseURIRequest, targetBaseURI, sourceBaseURI, contextAsArray);
	}else{ 
		reply = SemTurkeyHTTPLegacy.GET(serviceName, service.replaceBaseURIRequest, targetBaseURI, contextAsArray);
	}
	
	if (!reply.isFail()) {
		evtMgr.fireEvent("replaceBaseURI", {getOldBaseUri : function(){return oldBaseUri;}, 
			getNewBaseUri : function(){return ewBaseUri;}});
	}
	
	return reply;
}

//Refactor SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.rename = rename;
service.prototype.replaceBaseURI = replaceBaseURI;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;