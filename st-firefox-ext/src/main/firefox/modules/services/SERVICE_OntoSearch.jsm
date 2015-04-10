Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");	
Components.utils.import("resource://stmodules/ARTResources.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = ["STRequests"];

var service = STRequests.OntoSearch;
var serviceName = service.serviceName;


/**
 * returns the result of a search over ontology data of the proposed <code>inputString</code>
 * 
 * @member STRequests.OntoSearch
 * @param inputString
 * @param types can be either <em>property</em>, <em>clsNInd</em> or <em>concept</em>
 * @param scheme useful only if types=="concept"
 * @return
 */
function searchOntology(inputString, types, scheme){
	var inputString = "inputString="+inputString;
	var types = "types="+types;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	if (typeof scheme != "undefined"){
		var scheme = "scheme="+scheme;
		return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.searchOntologyRequest,
				inputString, types, scheme, contextAsArray));
	} else {
		return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.searchOntologyRequest,
				inputString, types, contextAsArray));
	}
}

/**
 * returns a path that goes from a root to the given concept in the given scheme
 */
function getPathFromRoot(concept, scheme){
	var concept = "concept="+concept;
	var scheme = "scheme="+scheme;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getPathFromRootRequest,
			concept, scheme, contextAsArray);
}

//OntoSearch SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.searchOntology = searchOntology;
service.prototype.getPathFromRoot = getPathFromRoot;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;