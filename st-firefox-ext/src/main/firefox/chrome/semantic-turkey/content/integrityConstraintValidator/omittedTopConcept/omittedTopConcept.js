if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

art_semanticturkey.init = function(){
	var listbox = document.getElementById("listbox");
	try {
		var xmlResp = art_semanticturkey.STRequests.ICV.listConceptSchemesWithNoTopConcept();
		var data = xmlResp.getElementsByTagName("data")[0];
		var schemes = data.getElementsByTagName("conceptScheme");
		for (var i=0; i<schemes.length; i++){
			var scheme = schemes[i].textContent;
			
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", scheme);
		    cell.addEventListener("dblclick", art_semanticturkey.schemeDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var button = document.createElement("button");
		    button.setAttribute("label", "Edit scheme");
		    button.setAttribute("flex", "1");
		    button.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(button);
		    
		    listbox.appendChild(listitem);
		}
	} catch (e){
		alert(e.message);
	}
}

art_semanticturkey.schemeDblClickListener = function() {
	var scheme = this.getAttribute("label");//this in an actionListener represents the target of the listener
	var parameters = new Object();
	parameters.sourceType = "conceptscheme";
	parameters.sourceElement = null;
	parameters.sourceElementName = scheme;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
}

art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode;
	var scheme = listitem.children[0].getAttribute("label");
	var parameters = new Object();
	parameters.sourceType = "conceptscheme";
	parameters.sourceElement = null;
	parameters.sourceElementName = scheme;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
}