Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = ["STRequests"];

var service = STRequests.Statement;
var serviceName = service.serviceName;

/**
 * basic service for retrieving statements from the RDF graph of current ontology
 * 
 * @member STRequests.Statement
 * @param subj
 * @param pred
 * @param obj
 * @param inference
 * @param ngs
 * 
 * @return
 */
function getStatements(subj, pred, obj, inference, ngs){
	var subj = "subj=" + subj;
	var pred = "pred=" +pred;
	var obj = "obj=" + obj;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	
	if(typeof inference != 'undefined'){
		var inference = "inference" + inference;
		if(typeof ngs != 'undefined'){
			var ngs = "ngs=" + ngs;
			return SemTurkeyHTTPLegacy.GET(serviceName, service.getStatementsRequest, subj, pred, obj, inference, ngs,
					contextAsArray);
		}
		return SemTurkeyHTTPLegacy.GET(serviceName, service.getStatementsRequest, subj, pred, obj, inference, 
				contextAsArray);
	}
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getStatementsRequest, subj, pred, obj, contextAsArray);
}

/**
 * checks if a statement is present in the graph of the current ontology
 * 
 * @member STRequests.Statement
 * @param subj
 * @param pred
 * @param obj
 * @param inference
 * @param ngs
 * 
 * @return
 */
function hasStatements(subj, pred, obj, inference, ngs){
	var subj = "subj=" + subj;
	var pred = "pred=" +pred;
	var obj = "obj=" + obj;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	
	if(typeof inference != 'undefined'){
		var inference = "inference" + inference;
		if(typeof ngs != 'undefined'){
			var ngs = "ngs=" + ngs;
			return SemTurkeyHTTPLegacy.GET(serviceName, service.hasStatementRequest, subj, pred, obj, inference, ngs,
					contextAsArray);
		}
		return SemTurkeyHTTPLegacy.GET(serviceName, service.hasStatementRequest, subj, pred, obj, inference, 
				contextAsArray);
	}
	return SemTurkeyHTTPLegacy.GET(serviceName, service.hasStatementRequest, subj, pred, obj, contextAsArray);
}

//SPARQL SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.getStatements = getStatements;
service.prototype.hasStatement = hasStatement;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
