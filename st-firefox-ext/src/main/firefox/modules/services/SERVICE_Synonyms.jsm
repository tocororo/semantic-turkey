Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Synonyms;
var serviceName = service.serviceName;


/**
 * adds a value on the rdf:label property to resource identified by <em>resourceName</em>
 * 
 * @member STRequests.Synonyms
 * @param resourceName
 * @param language
 * @param synonym
 * @return
 */
function addSynonyms(resourceName,language,synonym) {
	Logger.debug('[SERVICE_Synonyms.jsm] addSynonym');
		var resourceName ="name="+resourceName;
		var synonym ="synonym="+synonym;
		var language ="language="+language;
		var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
		SemTurkeyHTTPLegacy.GET(serviceName, service.addSynonymsRequest,resourceName,synonym,language, contextAsArray);
	}


//Synonym SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.addSynonyms = addSynonyms;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
