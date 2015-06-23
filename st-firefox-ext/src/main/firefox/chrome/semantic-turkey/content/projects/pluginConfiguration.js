if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function(){
	
	var extPoint = window.arguments[0].extensionPoint;
	document.getElementById("pluginConfiguration").setAttribute("title", extPoint + " Configuration");
	
	var shortName = window.arguments[0].shortName;
	document.getElementById("confLabel").setAttribute("value", "Configuration parameters for: "+shortName);

	//prepare the configuration parameters
	var parArray = window.arguments[0].parArray;
	art_semanticturkey.populateParameterListbox(parArray);
	
	document.getElementById("addParBtn").addEventListener("command", art_semanticturkey.addConfigurationParameter, true);
	document.getElementById("removeParBtn").addEventListener("command", art_semanticturkey.removeConfigurationParameter, true);
	document.getElementById("restoreParBtn").addEventListener("command", art_semanticturkey.restoreConfigurationParameter, true);
	
	document.getElementById("okBtn").addEventListener("command", art_semanticturkey.accept, true);
	document.getElementById("cancelBtn").addEventListener("command", function(){close();}, true);
};

/**
 * Given an array of configuration parameter (object with the following field: name, value, required
 * and description), fills a listbox
 */
art_semanticturkey.populateParameterListbox = function(parArray) {
	var parListbox = document.getElementById("parListbox");
	for (var i=0; i<parArray.length; i++){
		var listitem = document.createElement("listitem");
		listitem.setAttribute("align", "center");
		listitem.setAttribute("allowevents", "true");
		listitem.setAttribute("tooltiptext", parArray[i].description);
		var parName = parArray[i].name;
		listitem.parName = parName;
		listitem.required = parArray[i].required;
		var label = document.createElement("label");
		if(parArray[i].required == "true"){
			parName += "*";
			document.getElementById("requireParLegend").setAttribute("hidden", "false");
		}
		label.setAttribute("value", parName);
		var textbox = document.createElement("textbox");
		textbox.setAttribute("value", parArray[i].value);
		listitem.appendChild(label);
		listitem.appendChild(textbox);
		parListbox.appendChild(listitem);
	}
	window.sizeToContent();
	parListbox.addEventListener("select", function(){
			document.getElementById("removeParBtn").setAttribute("disabled", "false");
		}, false);
}

/**
 * Listener to the "Restore" button. Restores the default configuration.
 */
art_semanticturkey.restoreConfigurationParameter = function(){
	var parListbox = document.getElementById("parListbox");
	//reset listbox
	while (parListbox.itemCount > 0){
		parListbox.removeItemAt(0);
	}
	//repopulate
	var defaultParamsXml = window.arguments[0].defaultParamsXml;
	var parArray = [];
	for(var i=0; i<defaultParamsXml.length; i++){
		parArray[i] = new Object();
		parArray[i].name = defaultParamsXml[i].getAttribute("name");
		parArray[i].required = defaultParamsXml[i].getAttribute("required");
		parArray[i].description = defaultParamsXml[i].getAttribute("description");
		parArray[i].value = defaultParamsXml[i].textContent;
	}
	art_semanticturkey.populateParameterListbox(parArray);
}

/**
 * Listener to the "Add" button. Add a configuration parameter to the listbox.
 */
art_semanticturkey.addConfigurationParameter = function(){
	var parListbox = document.getElementById("parListbox");
	
	var prompts = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
    	.getService(Components.interfaces.nsIPromptService);

	var check = {value: false};
	var input = {value: ""};
	var result = prompts.prompt(null, "New configuration parameter", "Insert parameter's name", input, null, check);
	if (result) {
		var parName = input.value;
		//check a parameter with the same name already exists
		var found = false;
		for (var i=0; i<parListbox.itemCount; i++){
			if (parListbox.getItemAtIndex(i).parName == parName){
				found = true;
				break;
			}
		}
		if (found) {
			alert("Parameter '" + parName + "' already exists, cannot create a parameter with the same name.");
		} else {
			var listitem = document.createElement("listitem");
			listitem.setAttribute("align", "center");
			listitem.setAttribute("allowevents", "true");
			listitem.setAttribute("tooltiptext", parName);
			listitem.required = "false";
			listitem.parName = parName;
			var label = document.createElement("label");
			label.setAttribute("value", parName);
			var textbox = document.createElement("textbox");
			listitem.appendChild(label);
			listitem.appendChild(textbox);
			parListbox.appendChild(listitem);
		}
	}
}

/**
 * Listener to the "Remove" button. Removes a parameter from the configuration
 */
art_semanticturkey.removeConfigurationParameter = function(){
	var parListbox = document.getElementById("parListbox");
	var item = parListbox.selectedItem;
	if (item != null) {
		var parName = item.parName;
		//check if the parameter can be deleted
		if (item.required == "true") {
			alert("'" + parName + "' is a required configuration parameter, you cannot delete it.");
		} else {
			parListbox.removeItemAt(parListbox.getIndexOfItem(item));
		}
	}
}

/**
 * Listener to the "OK" button. Validates and then returns the configuration parameters to the
 * parent window
 */
art_semanticturkey.accept = function(){
	var parListbox = document.getElementById("parListbox");
	
	//check if all required parameters are set (still not tested, since there is no required param)
	for (var i=0; i<parListbox.itemCount; i++){
		var item = parListbox.getItemAtIndex(i);
		if(item.required == "true"  && item.children[1].value==""){
			alert("Please add a value to all the required parameters");
			return;
		}
	}
	//reset fill the parArray with <name, value> pairs
	window.arguments[0].parArray = [];
	for (var i=0; i<parListbox.itemCount; i++){
		var item = parListbox.getItemAtIndex(i);
		window.arguments[0].parArray[i] = new Object();
		window.arguments[0].parArray[i].name = item.parName;
		window.arguments[0].parArray[i].value = item.children[1].value;
		window.arguments[0].parArray[i].required = item.required;
		window.arguments[0].parArray[i].description = item.getAttribute("tooltiptext");
	}
	
	window.arguments[0].saved = true; //useful to inform that the conf has been edited and saved (necessary if editRequired is true)
	close();
}