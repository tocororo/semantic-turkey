Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/ResponseContentType.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.SPARQL;
var serviceName = service.serviceName;

/**
 * asks for a result to given query <code>queryPar</code>
 * 
 * @member STRequests.SPARQL
 * @param queryPar
 * @param languagePar
 *            (optional) if not specified, it is SPARQL, otherwise it must be the id of a query language
 *            supported by the OntManager implementation backending current ontology
 * @param inferPar
 * @param mode either query mode (read-only) or update (modification of the underlying data)
 * @return
 */
function resolveQuery(queryPar, languagePar, inferPar, mode) {
	var formData = Components.classes["@mozilla.org/files/formdata;1"]
		.createInstance(Components.interfaces.nsIDOMFormData);
	formData.append("query", queryPar);
	Logger.debug("query " + queryPar);
	formData.append("lang", languagePar);
	formData.append("infer", inferPar);
	if (mode != "undefined")
		formData.append("mode", mode);
	
	var respType = RespContType.json;
	
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	//var respType = RespContType.xml;
	var resp=SemTurkeyHTTPLegacy.POST(respType, serviceName, service.resolveQueryRequest, formData, contextAsArray);
	resp.respType = respType;
//	Ramon Orr� (2010): introduzione campo per memorizzare la serializzazione adottata
	//service.serializationType=SemTurkeyHTTPLegacy.serializationType;	
	return resp;
}

// SPARQL SERVICE INITIALIZATION
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.resolveQuery = resolveQuery;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
