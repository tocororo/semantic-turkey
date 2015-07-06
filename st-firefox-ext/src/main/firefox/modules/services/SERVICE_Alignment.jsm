Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Alignment;
var serviceName = service.serviceName;

const currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());

function addAlignment(sourceResource, predicate, targetResource) {
	Logger.debug('[SERVICE_Alignment.jsm] addAlignment');
	p_source = "sourceResource=" + sourceResource;
	p_predicate = "predicate=" + predicate;
	p_target = "targetResource=" + targetResource;
	currentSTHttpMgr.GET(null, serviceName, service.addAlignmentRequest, this.context, p_source, p_predicate, p_target);
}

function getMappingRelations(resource, allMappingProps) {
	Logger.debug("[SERVICE_Alignment.jsm] getMappingRelations");
	var params = [];
	params.push("resource="+resource);
	if (typeof allMappingProps != "undefined")
		params.push("allMappingProps=" + allMappingProps);
	return Deserializer.createRDFArray(currentSTHttpMgr.GET(
			null, serviceName, service.getMappingRelationsRequest, this.context, params));
}

service.prototype.addAlignment = addAlignment;
service.prototype.getMappingRelations = getMappingRelations;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;