Components.utils.import("resource://stmodules/StartST.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Annotation.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_RangeAnnotation.jsm", art_semanticturkey);

Components.utils.import("resource://stmodules/AnnotationManager.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/AnnotationCommons.jsm", art_semanticturkey);

art_semanticturkey.registerAnnotationFamilies = function() {
	try {
		// // Bookmarking family
		var bookmarking = art_semanticturkey.annotation.AnnotationManager
				.createFamily("it.uniroma2.art.semanticturkey.annotation.bookmarking");
		bookmarking.setName("bookmarking");
		bookmarking.setDescription("Bookmarking family");

		// Bind conventional functions
		bookmarking.checkAnnotationsForContent = art_semanticturkey.annotation.bookmarking.checkAnnotationsForContent;
		bookmarking.getAnnotationsForContent = function(urlPage){
			return art_semanticturkey.STRequests.Annotation.getPageAnnotations(urlPage);
		};
		bookmarking.deleteAnnotation = function(id){
			return art_semanticturkey.STRequests.Annotation.removeAnnotation(id);
		};
		bookmarking.annotation2ranges = art_semanticturkey.annotation.bookmarking.annotation2ranges;
		bookmarking.furtherAnn = art_semanticturkey.annotation.bookmarking.furtherAnn;
		bookmarking.getAnnotatedContentResources = function(resource) {
			return art_semanticturkey.STRequests.Annotation.getAnnotatedContentResources(resource);
		};

		// Register default handlers
		art_semanticturkey.annotation.commons.registerCommonHandlers(bookmarking);

		// Range annotation family
		var rangeannotation = art_semanticturkey.annotation.AnnotationManager
				.createFamily("it.uniroma2.art.semanticturkey.annotation.rangeannotation");
		rangeannotation.setName("range annotation");
		rangeannotation.setDescription("Range annotation family");

		// Bind conventional functions
		rangeannotation.checkAnnotationsForContent = art_semanticturkey.annotation.rangeannotation.checkAnnotationsForContent;
		rangeannotation.getAnnotationsForContent = function(urlPage){
			return art_semanticturkey.STRequests.RangeAnnotation.getPageAnnotations(urlPage);
		};
		rangeannotation.deleteAnnotation = function(id){
			return art_semanticturkey.STRequests.RangeAnnotation.deleteAnnotation(id);
		};
		rangeannotation.annotation2ranges = art_semanticturkey.annotation.rangeannotation.annotation2ranges;
		rangeannotation.furtherAnn = art_semanticturkey.annotation.rangeannotation.furtherAnn;
		rangeannotation.getAnnotatedContentResources = function(resource) {
			return art_semanticturkey.STRequests.RangeAnnotation.getAnnotatedContentResources(resource);
		};

		// Register default handlers
		art_semanticturkey.annotation.commons.registerCommonHandlers(rangeannotation);
	} catch (e) {
		// TODO change this code with a unique object (module) to be invoked for
		// alerting the user
		var prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
				.getService(Components.interfaces.nsIPromptService);
		prompts.alert(null, "Annotation Extension Point Initialization Error",
				"an error occurred during initialization of the Annotation Component:\n"
				// + e.getMessage());
				+ e.toString());
	}
};
