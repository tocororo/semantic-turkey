var art_semanticturkey = {};

art_semanticturkey.init = function(event) {
	var schemeList = document.getElementById("schemeList");
	schemeList.hidetoolbar = typeof window.arguments[0].hidetoolbar == "undefined" ? true : window.arguments[0].hidetoolbar;
	schemeList.hideheading = typeof window.arguments[0].hideheading == "undefined" ? true : window.arguments[0].hideheading;
	schemeList.mutable = typeof window.arguments[0].mutable == "undefined" ? false : window.arguments[0].mutable;
	document.getElementById("schemeDialog").getButton("accept").focus();
};

art_semanticturkey.onAccept = function(event) {
	var schemeList = document.getElementById("schemeList");
	
	var out = {};
	out.selectedScheme = schemeList.selectedScheme;
	
	window.arguments[0].out = out;
};

window.addEventListener("load", art_semanticturkey.init, true);
window.addEventListener("dialogaccept", art_semanticturkey.onAccept, true);
