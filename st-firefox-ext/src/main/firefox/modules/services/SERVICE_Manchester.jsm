Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");
Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.Manchester;
var serviceName = service.serviceName;

const currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());


function getAllDLExpression(classUri, usePrefixes){
	Logger.debug('[SERVICE_Manchester.jsm] getAllDLExpression');
	var p_classUri = "classUri="+classUri;
	if (typeof usePrefixes != "undefined") {
		var p_usePrefixes = "usePrefixes="+usePrefixes;
		return currentSTHttpMgr.GET(null, serviceName, service.getAllDLExpressionRequest, this.context, 
				p_classUri, p_usePrefixes);
	} else{
		return currentSTHttpMgr.GET(null, serviceName, service.getAllDLExpressionRequest, this.context, 
				p_classUri);
	}
}

function getExpression(artNode, usePrefixes){
	Logger.debug('[SERVICE_Manchester.jsm] getExpression');
	var p_artNode = "artNode="+artNode;
	if (typeof usePrefixes != "undefined") {
		var p_usePrefixes = "usePrefixes="+usePrefixes;
		return currentSTHttpMgr.GET(null, serviceName, service.getExpressionRequest, this.context, p_artNode,
				p_usePrefixes);
	} else{
		return currentSTHttpMgr.GET(null, serviceName, service.getExpressionRequest, this.context, p_artNode);
	}
}

function removeExpression(classUri,exprType, artNode){
	Logger.debug('[SERVICE_Manchester.jsm] removeExpression');
	var p_classUri = "classUri="+classUri; 
	var p_exprType = "exprType="+exprType; 
	var p_artNode = "artNode="+artNode; 
	return currentSTHttpMgr.GET(null, serviceName, service.removeExpressionRequest, this.context, p_classUri,
			p_exprType, p_artNode);
}

function checkExpression(manchExpr){
	Logger.debug('[SERVICE_Manchester.jsm] checkExpression');
	var p_manchExpr = "manchExpr="+manchExpr; 
	return currentSTHttpMgr.GET(null, serviceName, service.checkExpressionRequest, this.context, p_manchExpr);
}

function createRestriction(classUri, exprType, manchExpr){
	Logger.debug('[SERVICE_Manchester.jsm] createRestriction');
	var p_classUri = "classUri="+classUri; 
	var p_exprType = "exprType="+exprType; 
	var p_manchExpr = "manchExpr="+manchExpr; 
	return currentSTHttpMgr.GET(null, serviceName, service.createRestrictionRequest, this.context, p_classUri,
			p_exprType, p_manchExpr);
}


service.prototype.getAllDLExpression = getAllDLExpression;
service.prototype.getExpression = getExpression;
service.prototype.removeExpression = removeExpression;
service.prototype.checkExpression = checkExpression;
service.prototype.createRestriction = createRestriction;

service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;


