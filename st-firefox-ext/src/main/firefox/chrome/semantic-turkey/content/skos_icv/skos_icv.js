if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function() {
	var gridRows = document.getElementById("gridRows");
	var rows = gridRows.children;
	for (var i=0; i<rows.length; i++){
		if (rows[i].tagName == "row"){ 
			var row = rows[i];
			var btn = row.children[2];
			btn.addEventListener("command", art_semanticturkey.btnCheckListener, false);
		}
	}
}

art_semanticturkey.btnCheckListener = function() {
	var btn = this;
	var row = btn.parentNode;
	var target = row.getAttribute("target");
//	art_semanticturkey.Logger.debug("opening dialog " + target);
	window.openDialog(target, "_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen");
}