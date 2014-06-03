Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Deserializer.jsm");	
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

Components.utils.import("resource://stmodules/Context.jsm");

EXPORTED_SYMBOLS = [ "SemTurkeyHTTPLegacy", "STRequests" ];

var service = STRequests.Annotation;
var serviceName = service.serviceName;

/**
 * create and annotate an istance with drag n drop
 * 
 * @member STRequests.Annotation
 * @return
 */
function createAndAnnotate(clsQName, instanceQName, urlPage, title) {
	Logger.debug('[SERVICE_Annotation.jsm] createAndAnnotate');
	var clsQName = "clsQName=" + clsQName;
	var instanceQName = "instanceQName=" + instanceQName;
	var urlPage = "urlPage=" + urlPage;
	var title = "title=" + title;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.createAndAnnotateRequest, clsQName, instanceQName, urlPage, title, contextAsArray);
	var resArray = new Array();
	resArray["class"] = Deserializer.createURI(reply.getElementsByTagName("Class")[0]);
	resArray["instance"] = Deserializer.createURI(reply.getElementsByTagName("Instance")[0]);
	return resArray;
}


/**
 * given qname <code>instanceQName</code> of an existing instance, it adds an annotation according to the
 * other arguments of the method
 * 
 * @member STRequests.Annotation
 * @param instanceQName
 * @param text
 * @param urlPage
 * @param title
 * @return
 */
function createFurtherAnnotation(instanceQName,text,urlPage,title) {
	Logger.debug('[SERVICE_Annotation.jsm] createFurtherAnnotationRequest');
	var instanceQName = "instanceQName=" + instanceQName;
	var text = "text=" + text;
	var urlPage = "urlPage=" + urlPage;
	var title = "title=" + title;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.createFurtherAnnotationRequest, instanceQName, text, urlPage, title, contextAsArray);
}



/**
 * as for <code>relateAndAnnotate</code> with some of its arguments set for binding annotated value to an
 * existing resource
 * 
 * @member STRequests.Annotation
 * @param instanceQName
 * @param propertyQName
 * @param objectQName
 * @param urlPage
 * @param title
 * @param lexicalization
 * @return
 */
function relateAndAnnotateBindAnnot(instanceQName,propertyQName,objectQName,urlPage,title,lexicalization) {
	return relateAndAnnotate(instanceQName,propertyQName,objectQName,urlPage,title,null,null,null,lexicalization); 
}

/**
 * as for <code>relateAndAnnotate</code> with some of its arguments set for creating an element
 * (resource/literal) from the annotated text
 * 
 * @member STRequests.Annotation
 * @param instanceQName
 * @param propertyQName
 * @param objectQName
 * @param objectClsName
 * @param urlPage
 * @param title
 * @param lang
 * @param type
 * @return
 */
function relateAndAnnotateBindCreate(instanceQName,propertyQName,objectQName,urlPage,title,objectClsName,lang,type) {
	return relateAndAnnotate(instanceQName,propertyQName,objectQName,urlPage,title,objectClsName,lang,type); 
}


/**
 * this method is used to get information from web pages and use it to:
 * <ul>
 * <li>relate existing objects in the ontology with new objects got from the web </li>
 * <li>take the annotation related to the new object</li>
 * </ul>
 * 
 * @member STRequests.Annotation
 * @param instanceQName
 *            the qname of the selected object which is related to the annotated one
 * @param propertyQName
 *            the property which is used to qualify the selected object with new annotated information
 * @param objectQName
 *            the annotated object (this may be an ontology object, or a literal)
 * @param objectClsName
 *            if objectQName is a resource which is to be created, then this is the class associated that
 *            resource
 * @param urlPage
 *            the url of the web page from which the annotation has been taken
 * @param title
 *            the title of the web page from which the annotation has been taken
 * @param lang
 *            the iso code for th language of the annotated text
 * @param type
 *            used to qualify the nature of the annotated object; this argument is mandatory only if the
 *            property specified by <code>propertyQName</code>is a standard property, otherwise the nature
 *            of the object is desumed from the nature of the property (resource for object properties and
 *            literal in other cases).<br/> In case of a standard property, "literal" must be specified,
 *            otherwise a resource is added to the property
 * 
 * @param lexicalization
 * @return
 */
function relateAndAnnotate(instanceQName,propertyQName,objectQName,urlPage,title,objectClsName,lang,type,lexicalization) {
	var instanceQName = "instanceQName=" + instanceQName;
	var propertyQName = "propertyQName=" + propertyQName;
	var objectQName = "objectQName=" + objectQName;
	var urlPage = "urlPage=" + urlPage;
	var title = "title=" + title;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	if(typeof lexicalization != 'undefined'){
		var op = "op=bindAnnot";
		var lex = "lexicalization=" + lexicalization;
		return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
	    		instanceQName,
	    		propertyQName,
	    		objectQName,
	    		lex,
	    		urlPage, 
	    		title,
	    		op,
	    		contextAsArray);
	}else{
		var op = "op=bindCreate";
		if(lang != ""){
			var lang = "lang=" + lang;
			if(typeof type != 'undefined'){
				var type = "type=" +type;
				if(typeof objectClsName != 'undefined'){
					var objectClsName = "objectClsName=" + objectClsName;
					return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			objectClsName, 
		    			urlPage, 
		    			title,
		    			lang,
		    			type,
		    			op,
		    			contextAsArray);
		    	}else{
		    		return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			urlPage, 
		    			title,
		    			lang,
		    			type,
		    			op,
		    			contextAsArray);
		    	}
	    	} else{
		    	if(typeof objectClsName != 'undefined'){
					var objectClsName = "objectClsName=" + objectClsName;
					return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			objectClsName, 
		    			urlPage, 
		    			title,
		    			lang,
		    			op,
		    			contextAsArray);
		    	}else{
		    		return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			urlPage, 
		    			title,
		    			lang,
		    			op,
		    			contextAsArray);
		    	}
		    }
		}else{
			if(typeof type != 'undefined'){
				var type = "type=" +type;
				if(typeof objectClsName != 'undefined'){
					var objectClsName = "objectClsName=" + objectClsName;
					return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			objectClsName, 
		    			urlPage, 
		    			title,
		    			type,
		    			op,
		    			contextAsArray);
		    	}else{
		    		return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			urlPage, 
		    			title,
		    			type,
		    			op,contextAsArray);
		    	}
    		} else{
	    		if(typeof objectClsName != 'undefined'){
					var objectClsName = "objectClsName=" + objectClsName;
					return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
	    				instanceQName,
	    				propertyQName,
	    				objectQName,
	    				objectClsName, 
	    				urlPage, 
	    				title,
	    				op,
	    				contextAsArray);
		    	}else{
		    		return SemTurkeyHTTPLegacy.GET(serviceName, service.relateAndAnnotateRequest, 
		    			instanceQName,
		    			propertyQName,
		    			objectQName,
		    			urlPage, 
		    			title,
		    			op,
		    			contextAsArray);
		    	}
		    }
		}
	}
   }

/**
 * given page <code>urlPage</code>, this method tells if the page contains annotations
 * 
 * @member STRequests.Annotation
 * @param urlPage
 * @return
 */
function chkAnnotation(urlPage) {
	var urlPage="urlPage="+urlPage;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.chkAnnotationsRequest,urlPage,contextAsArray);
}

 /** given page <code>urlPage</code>, this method tells if the page has topics
 * 
 * @member STRequests.Annotation
 * @param urlPage
 * @return
 */
function chkBookmarks(urlPage) {
	var urlPage="urlPage="+urlPage;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.chkBookmarksRequest,urlPage,contextAsArray);
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
	var reponseXML = SemTurkeyHTTPLegacy.GET(serviceName, service.getPageAnnotationsRequest,urlPage,contextAsArray);
	var annotations = [];
	var annotationsXML = reponseXML.getElementsByTagName("Annotation");
	
	for (var i = 0 ; i < annotationsXML.length ; i++) {
		var annXML = annotationsXML[i];
		
		annotations.push(
			{
				id : annXML.getAttribute("id"),
				resource : annXML.getAttribute("resource"),
				value : annXML.getAttribute("value")
			}
		);
	}
	
	return annotations;
}

/**
 * @member STRequests.Annotation
 * @param urlPage
 * @param instanceQName
 * @param text
 * @param title
 * @return
 */
function addAnnotation(urlPage,instanceQName,text,title){
	var urlPage="urlPage="+urlPage;
	var instanceQName="instanceQName="+instanceQName;
	var text="text="+text;
	var title="title="+title;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.addAnnotationRequest,urlPage,instanceQName,text,title,contextAsArray);
}

/**
 * @member STRequests.Annotation
 * @param urlPage
 * @param title
 * @param topics
 * @return
 */
function bookmarkPage(urlPage, title, topics){
	var urlPage_p="urlPage="+urlPage;
	var title_p="title="+title;
	var topics_p = "topics=";
	
	for (var i = 0 ; i < topics.length ; i++) {
		topics_p += topics[i];
	}
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.bookmarkPageRequest,urlPage_p,title_p,topics_p,contextAsArray));
}

function getPageTopics(urlPage) {
	var urlPage_p = "urlPage=" + urlPage;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return Deserializer.createRDFArray(SemTurkeyHTTPLegacy.GET(serviceName, service.getPageTopicsRequest,urlPage_p,contextAsArray));
}

function getBookmarksByTopic(topic) {
	var topic_p = "topic=" + topic;
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	return SemTurkeyHTTPLegacy.GET(serviceName, service.getBookmarksByTopicRequest,topic_p,contextAsArray);
}

function removeBookmark(urlPage, topic) {
	var urlPage_p = "urlPage=" + urlPage;
	var topic_p = "topic=" + topic;
	
	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var reply = SemTurkeyHTTPLegacy.GET(serviceName, service.removeBookmarkRequest, urlPage_p, topic_p,contextAsArray);

	if (!reply.isFail()) {
		evtMgr.fireEvent("bookmarkRemoved", {
			getPageURL : function() {return urlPage;},
			getTopic : function() {return topic;}
			});
	}
	
	return reply;
}

/**
 * 
 * @member STRequests.RangeAnnotation
 * @param id
 * @return
 */
function removeAnnotation(id) {
	var id_p = "id=" + id;

	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var responseXML = SemTurkeyHTTPLegacy.GET(serviceName, service.removeAnnotationRequest, id_p,contextAsArray);
	
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
 * @member STRequests.Annotation
 * @param resource
 * @return
 */
function getAnnotatedContentResources(resource) {
	var resource_p = "resource=" + resource;

	var contextAsArray = this.context.getContextValuesForHTTPGetAsArray();
	var responseXML = SemTurkeyHTTPLegacy.GET(serviceName, service.getAnnotatedContentResourcesRequest, resource_p,contextAsArray);
	
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

// Annotation SERVICE INITIALIZATION
//this return an implementation for Project with a specified context
service.prototype.getAPI = function(specifiedContext){
	var newObj = new service();
	newObj.context = specifiedContext;
	return newObj;
}
service.prototype.chkAnnotation = chkAnnotation;
service.prototype.chkBookmarks = chkBookmarks;
service.prototype.getPageAnnotations = getPageAnnotations;
service.prototype.createAndAnnotate = createAndAnnotate;
service.prototype.relateAndAnnotateBindAnnot = relateAndAnnotateBindAnnot;
service.prototype.relateAndAnnotateBindCreate = relateAndAnnotateBindCreate;
service.prototype.createFurtherAnnotation = createFurtherAnnotation;
service.prototype.addAnnotation=addAnnotation;
service.prototype.bookmarkPage=bookmarkPage;
service.prototype.getPageTopics=getPageTopics;
service.prototype.getBookmarksByTopic=getBookmarksByTopic;
service.prototype.removeBookmark=removeBookmark;
service.prototype.removeAnnotation = removeAnnotation;
service.prototype.getAnnotatedContentResources = getAnnotatedContentResources;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;