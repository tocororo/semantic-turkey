Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");	
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "HttpMgr", "STRequests" ];

var service = STRequests.RangeAnnotation;
var serviceName = service.serviceName;

/**
 * given page <code>urlPage</code>, this method tells if the page contains annotations
 * 
 * @member STRequests.RangeAnnotation
 * @param urlPage
 * @return
 */
function chkAnnotation(urlPage) {
	var urlPage="urlPage="+urlPage;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return HttpMgr.GET(serviceName, service.chkAnnotationsRequest,urlPage, contextAsArray);
}

/**
 * given page <code>urlPage</code>, it returns all annotations previously taken on that page
 * 
 * @member STRequests.Annotation
 * @param urlPage
 * @return
 */
function getPageAnnotations(urlPage) {
	var urlPage="urlPage="+urlPage;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reponseXML = HttpMgr.GET(serviceName, service.getPageAnnotationsRequest,urlPage,contextAsArray);
	var annotations = [];
	var annotationsXML = reponseXML.getElementsByTagName("RangeAnnotation");
	
	for (var i = 0 ; i < annotationsXML.length ; i++) {
		var annXML = annotationsXML[i];
		
		annotations.push(
			{
				id : annXML.getAttribute("id"),
				resource : annXML.getAttribute("resource"),
				value : annXML.getAttribute("value"),
				range : annXML.getAttribute("range")
			}
		);
	}
	
	return annotations;
}

/**
 * 
 * @member STRequests.RangeAnnotation
 * @param resource
 * @param lexicalization
 * @param urlPage
 * @param title
 * @param range
 * @return
 */
function addAnnotation(resource, lexicalization, urlPage, title, range) {
	var resource_p = "resource=" + resource;
	var lexicalization_p = "lexicalization=" + lexicalization;
	var urlPage_p = "urlPage=" + urlPage;
	var title_p = "title=" + title;
	var range_p = "range=" + range;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var responseXML = HttpMgr.GET(serviceName, service.addAnnotationRequest,resource_p, lexicalization_p,
	   urlPage_p, title_p, range_p, contextAsArray);

	return responseXML;
}

/**
 * 
 * @member STRequests.RangeAnnotation
 * @param id
 * @return
 */
function deleteAnnotation(id) {
	var id_p = "id=" + id;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var responseXML = HttpMgr.GET(serviceName, service.deleteAnnotationRequest, id_p, contextAsArray);
	
	if (!responseXML.isFail()) {
		var annotation;
		
		evtMgr.fireEvent("annotationDeleted", {
			getId : function(){return id;}
		});
	}

	return responseXML;
}

/**
 * 
 * @member STRequests.RangeAnnotation
 * @param resource
 * @return
 */
function getAnnotatedContentResources(resource) {
	var resource_p = "resource=" + resource;

	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var responseXML = HttpMgr.GET(serviceName, service.getAnnotatedContentResourcesRequest, resource_p,
			contextAsArray);
	
	var response = [];
	
	var urlElements = responseXML.getElementsByTagName("URL");
	
	for (var i = 0 ; i < urlElements.length ; i++) {
		var obj = {};
		obj.title = urlElements[i].getAttribute("title");
		obj.value = urlElements[i].getAttribute("value");
		
		response.push(obj);
	}

	return response;
}

// Range Annotation SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.chkAnnotation = chkAnnotation;
service.prototype.getPageAnnotations = getPageAnnotations;
service.prototype.addAnnotation = addAnnotation;
service.prototype.deleteAnnotation = deleteAnnotation;
service.prototype.getAnnotatedContentResources = getAnnotatedContentResources;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
