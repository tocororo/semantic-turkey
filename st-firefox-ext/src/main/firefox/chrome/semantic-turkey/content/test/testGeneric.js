
if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);


art_semanticturkey.onloadTextGeneric = function() {
	document.getElementById("testButton10").addEventListener("command",
			art_semanticturkey.useGenericFunction, true);
	

	
}

art_semanticturkey.useGenericFunction = function(){
	art_semanticturkey.compareVersions();
}



/***************************************************************/





window.addEventListener("load",
		art_semanticturkey.onloadTextGeneric, true);
