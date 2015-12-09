if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Preferences.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_SystemStart.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_OntManager.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Plugins.jsm", art_semanticturkey);

window.onload = function(){
	
	document.getElementById("newProject").addEventListener("command", art_semanticturkey.onAccept, true);
	document.getElementById("cancel").addEventListener("command", art_semanticturkey.cancel, true);
	document.getElementById("openTripleStoreConf").addEventListener("command", art_semanticturkey.openTripleStoreConfiguration, true);
	document.getElementById("projectName").focus();
	if(window.arguments[0].fromFile == false){
		document.getElementById("fromFileRow").hidden = true;
	}
	else{
		document.getElementById("dirBtn").addEventListener("click", art_semanticturkey.chooseFile, true);
	}

	// this call will be erased once we provide full support for SKOS
	// see comments on the function definition
	art_semanticturkey.populate_projectType();

	var responseXML = art_semanticturkey.STRequests.SystemStart.listOntManagers();
	art_semanticturkey.populateTripleStoreMenulist_RESPONSE(responseXML);
	
	//optional settings panel
	document.getElementById("expandOptSetBtn").addEventListener("command", art_semanticturkey.expandOptionalSettingsListener, false);
	//in the future this informations (extPoint IDs and optional/mandatory) should be provided by the server and not hardcoded
	art_semanticturkey.buildExtensionPointUI("it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator", false);
	art_semanticturkey.buildExtensionPointUI("it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine", false);

	window.sizeToContent();
};

// this function will be erased once we provide full support for SKOS
// now this allows user to enable the hidden SKOS support (still unstable) through the preference:
// extensions.semturkey.skos.enabled
art_semanticturkey.populate_projectType = function() {
	
	var skos_enabled = art_semanticturkey.Preferences.get("extensions.semturkey.skos.enabled", false);
	if (skos_enabled == true) {
	
			var ontologyTypeMenupopup = document.getElementById("ontologyTypeMenupopup");
			
			var menuitem = document.createElement("menuitem");
			menuitem.setAttribute("value","it.uniroma2.art.owlart.models.SKOSModel");
			menuitem.setAttribute("label","SKOS");
			ontologyTypeMenupopup.appendChild(menuitem);
			
			var menuitem2 = document.createElement("menuitem");
			menuitem2.setAttribute("value","it.uniroma2.art.owlart.models.SKOSXLModel");
			menuitem2.setAttribute("label","SKOS-XL");
			ontologyTypeMenupopup.appendChild(menuitem2);
	 }
}

art_semanticturkey.populateTripleStoreMenulist_RESPONSE = function(responseElement){
	var repList = responseElement.getElementsByTagName('Repository');
	var tripleStoreMenupopup = document.getElementById("tripleStoreMenupopup");
	
	var menuitem = document.createElement("menuitem");
	menuitem.setAttribute("id","---");
	menuitem.setAttribute("label","---");
	tripleStoreMenupopup.appendChild(menuitem);
	var tripleStoreMenulist = document.getElementById("tripleStoreMenulist");
	tripleStoreMenulist.selectedItem = menuitem;
	
	for ( var i = 0; i < repList.length; i++) {
		var repositoryName = repList[i].getAttribute("repName");
		var repositoryNameReduced = repositoryName.substring(repositoryName.lastIndexOf('.') + 1);
		
		var menuitem = document.createElement("menuitem");
		menuitem.setAttribute("id",repositoryName);
		menuitem.setAttribute("label",repositoryNameReduced);
		tripleStoreMenupopup.appendChild(menuitem);
	}
	tripleStoreMenulist.addEventListener("command", art_semanticturkey.setOntManager, false);
};

art_semanticturkey.setOntManager = function(){
	var repositoryName = this.selectedItem.getAttribute("id");
	if (repositoryName != "---"){
		var responseXML = art_semanticturkey.STRequests.OntManager.getOntManagerParameters(repositoryName);
		art_semanticturkey.setOntManager_RESPONSE(responseXML);
	} else {
		var modeMenupopup = document.getElementById("modeMenupopup");
		while (modeMenupopup.hasChildNodes()) {
			modeMenupopup.removeChild(modeMenupopup.lastChild);
		}
		document.getElementById("modeMenulist").setAttribute("disabled", true);
		document.getElementById("modeMenulist").selectedIndex = -1;
	}
};

art_semanticturkey.setOntManager_RESPONSE = function(responseElement){
	var modeMenupopup = document.getElementById("modeMenupopup");
	var configurationNodeList = responseElement.getElementsByTagName("configuration");
	//first of all remove all the menuitem
	while (modeMenupopup.hasChildNodes()) {
		modeMenupopup.removeChild(modeMenupopup.lastChild);
	}
	//Then add the new menuitem
	for(var i=0; i<configurationNodeList.length; ++i){
		var parNodeList = configurationNodeList[i].getElementsByTagName("par");
		var menuItem = document.createElement("menuitem");
		menuItem.setAttribute("label", configurationNodeList[i].getAttribute("shortName"));
		menuItem.shortName = configurationNodeList[i].getAttribute("shortName");
		menuItem.editRequired = configurationNodeList[i].getAttribute("editRequired");
		menuItem.typeOntMgr = configurationNodeList[i].getAttribute("type");
		menuItem.par = new Array();
		for(var k=0; k<parNodeList.length; ++k){
			menuItem.par[k] = new Object();
			menuItem.par[k].description = parNodeList[k].getAttribute("description");
			menuItem.par[k].name = parNodeList[k].getAttribute("name");
			menuItem.par[k].required = parNodeList[k].getAttribute("required");
			menuItem.par[k].value = parNodeList[k].textContent;
		}
		modeMenupopup.appendChild(menuItem);
		if(i == 0)
			document.getElementById("modeMenulist").selectedItem = menuItem;
	}
	document.getElementById("modeMenulist").setAttribute("disabled", false);
	document.getElementById("openTripleStoreConf").setAttribute("disabled", false);
};

/**
 * listener to "Configure" button of the triple store. Opens a window that allows to edit the 
 * configuration parameters of the triple store
 */
art_semanticturkey.openTripleStoreConfiguration = function(){
	var selectedItem = document.getElementById("modeMenulist").selectedItem;
	var repositoryName = selectedItem.getAttribute("id");
	var parameters = new Object();
	parameters.parentWindow = window;
	parameters.shortName = selectedItem.shortName;
	parameters.editRequired = selectedItem.editRequired;
	parameters.typeOntMgr = selectedItem.tyoeOntMgr;
	parameters.parArray = selectedItem.par;
	parameters.saved = false;
	
	window.openDialog("chrome://semantic-turkey/content/projects/configurationOntMgr.xul", "_blank",
		"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
		parameters);
	
	return parameters.saved;
};

/**
 * Expands/Collapses the box containing the configuration panel of the optional settings
 */
art_semanticturkey.expandOptionalSettingsListener = function() {
	var optionalSettingsBox = document.getElementById("optionalSettingsBox");
	if (optionalSettingsBox.getAttribute("hidden") == "true") {
		optionalSettingsBox.setAttribute("hidden", "false");
		this.setAttribute("label", "▽");
		this.setAttribute("tooltiptext", "Collapse");
	} else {
		optionalSettingsBox.setAttribute("hidden", "true");
		this.setAttribute("label", "▷");
		this.setAttribute("tooltiptext", "Expand");
	}
}

/**
 * Build the UI for an extension point configurator. It creates a groupbox containing:
 * - A menulist to choose one of the available plugin for the given extension point
 * - A menulist to choose one of the available configuration for the chosen plugin
 * - A button to edit the configuration 
 */
art_semanticturkey.buildExtensionPointUI = function(extensionPoint, mandatory) {
	var settingContainer;
	if (!mandatory) {
		//if the extPoint is optional set visible the expandable panel
		var optionalSettingsGroupbox = document.getElementById("optionalSettingsGroupbox");
		optionalSettingsGroupbox.setAttribute("hidden", false);
		settingContainer = document.getElementById("optionalSettingsBox");
	} else {
		settingContainer = document.getElementById("mandatorySettingsBox");
	}
	
	var groupbox = document.createElement("groupbox");
	extPointLocalName = extensionPoint.substring(extensionPoint.lastIndexOf(".")+1);
	groupbox.extensionPoint = extPointLocalName;
	var caption = document.createElement("caption");
	caption.setAttribute("label", extPointLocalName);
	groupbox.appendChild(caption);
	//3-columns grid
	var grid = document.createElement("grid");
	var columns = document.createElement("columns");
	var column = document.createElement("column");
	column.setAttribute("width", "100");
	columns.appendChild(column);
	column = document.createElement("column");
	column.setAttribute("flex", "1");
	columns.appendChild(column);
	column = document.createElement("column");
	column.setAttribute("width", "100");
	columns.appendChild(column);
	grid.appendChild(columns);
	//2 rows
	var rows = document.createElement("rows");
	rows.setAttribute("flex", "1");
	//1st row containing menu listing available plugin for the given extensionPoint
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	var label = document.createElement("label");
	label.setAttribute("value", "Plugin:");
	row.appendChild(label);
	var pluginMenulist = document.createElement("menulist");
	art_semanticturkey.populateAvailablePluginMenulist(extensionPoint, pluginMenulist);
	row.appendChild(pluginMenulist);
	rows.appendChild(row);
	//2nd row containing menu listing available configuration for the plugin chosen in 1st menu
	row = document.createElement("row");
	row.setAttribute("align", "center");
	var label = document.createElement("label");
	label.setAttribute("value", "Configuration:");
	row.appendChild(label);
	var configurationMenulist = document.createElement("menulist");
	configurationMenulist.setAttribute("disabled", "true");
	row.appendChild(configurationMenulist);
	var button = document.createElement("button");
	button.setAttribute("disabled", "true");
	button.setAttribute("label", "Configure");
	button.addEventListener("command", function() {
			art_semanticturkey.openPluginConfiguration(configurationMenulist.selectedItem);
		}, true);
	row.appendChild(button);
	//add listener to plugin menulist that affects configuration menulist
	pluginMenulist.addEventListener("select", function() {
			art_semanticturkey.populatePluginConfigurationMenulist(
					pluginMenulist.selectedItem.id, configurationMenulist);
		}, false);
	
	rows.appendChild(row);
	grid.appendChild(rows);
	groupbox.appendChild(grid);

	settingContainer.appendChild(groupbox);
}

/**
 * Given an extension point, gets the available plugin and populates a menulist
 */
art_semanticturkey.populateAvailablePluginMenulist = function(extensionPoint, pluginMenulist) {
	var responseXML = art_semanticturkey.STRequests.Plugins.getAvailablePlugins(extensionPoint);
	var pluginList = responseXML.getElementsByTagName('plugin');
	
	var menupopup = document.createElement("menupopup");
	var menuitem = document.createElement("menuitem");
	menuitem.setAttribute("id","---");
	menuitem.setAttribute("label","---");
	menupopup.appendChild(menuitem);
	
	for (var i=0; i<pluginList.length; i++) {
		var factoryID = pluginList[i].getAttribute("factoryID");
		var factoryLocalname = factoryID.substring(factoryID.lastIndexOf(".")+1);
		
		var menuitem = document.createElement("menuitem");
		menuitem.setAttribute("id",factoryID);
		menuitem.setAttribute("label",factoryLocalname);
		menupopup.appendChild(menuitem);
	}
	pluginMenulist.appendChild(menupopup);
}

/**
 *	given a pluginId gets its configurations and populates the configuration menu 
 */
art_semanticturkey.populatePluginConfigurationMenulist = function(pluginId, configurationMenulist) {
	//reset (removing) the menupopup child of the menulist
	if (configurationMenulist.firstChild)
		configurationMenulist.removeChild(configurationMenulist.firstChild);
	if (pluginId != "---"){
		var responseXML = art_semanticturkey.STRequests.Plugins.getPluginConfigurations(pluginId);
		var configList = responseXML.getElementsByTagName("configuration");
		//create new menupopup from scratch
		var menupopup = document.createElement("menupopup");
		for (var i=0; i<configList.length; i++){
			var menuItem = document.createElement("menuitem");
			menuItem.setAttribute("label", configList[i].getAttribute("shortName"));
			menuItem.shortName = configList[i].getAttribute("shortName");
			menuItem.editRequired = configList[i].getAttribute("editRequired");
			menuItem.type = configList[i].getAttribute("type");
			var parList = configList[i].getElementsByTagName("par");
			menuItem.defaultParamsXml = parList;
			menuItem.par = new Array();
			for(var k=0; k<parList.length; k++){
				menuItem.par[k] = new Object();
				menuItem.par[k].description = parList[k].getAttribute("description");
				menuItem.par[k].name = parList[k].getAttribute("name");
				menuItem.par[k].required = parList[k].getAttribute("required");
				menuItem.par[k].value = parList[k].textContent;
			}
			menupopup.appendChild(menuItem);
		}
		configurationMenulist.appendChild(menupopup);
		configurationMenulist.selectedIndex = 0;
		configurationMenulist.setAttribute("disabled", false);
		configurationMenulist.parentNode.getElementsByTagName("button")[0].setAttribute("disabled", false);
	} else {
		configurationMenulist.selectedIndex = -1;
		configurationMenulist.disabled = true;
	}
}

/**
 * listener to "Configure" button of a plugin. Opens a window that allows to edit the configuration
 * parameters of a plugin
 */
art_semanticturkey.openPluginConfiguration = function(configurationMenuitem){
	var parameters = new Object();
	//menuitem > menupopup > menulist > row > rows > grid > groupbox > caption
	var extPoint = configurationMenuitem.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.
		getElementsByTagName("caption")[0].getAttribute("label");
	parameters.extensionPoint = extPoint; 
	parameters.shortName = configurationMenuitem.shortName;
	parameters.parArray = configurationMenuitem.par;
	parameters.defaultParamsXml = configurationMenuitem.defaultParamsXml;
	parameters.saved = false;
	window.openDialog("chrome://semantic-turkey/content/projects/pluginConfiguration.xul", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", 
			parameters);
	configurationMenuitem.par = parameters.parArray;
	
	return parameters.saved;
}

art_semanticturkey.onAccept = function() {
	art_semanticturkey.DisabledAllButton(true);
	
	if(document.getElementById("modeMenulist").getAttribute("disabled") == "true" ){
		art_semanticturkey.DisabledAllButton(false);
		alert("Please select a Triple Store");
		return;
	}
	//check if this configuration has the attribute editRequired set to true, if so open the configurationOntMgr
	var selectedItem = document.getElementById("modeMenulist").selectedItem;
	if(selectedItem.editRequired == "true"){
		if(art_semanticturkey.openTripleStoreConfiguration() == false){
			art_semanticturkey.DisabledAllButton(false);
			return;
		}
	}
		
	var projectName = document.getElementById("projectName").value;
	var ontologyType = document.getElementById("ontologyTypeMenulist").selectedItem.getAttribute("value");
	var uri = document.getElementById("uri").value;
	
	//ontMgr configuration and parameters
	var tripleStore = document.getElementById("tripleStoreMenulist").selectedItem.getAttribute("id");
	var ontMgrConfiguration = document.getElementById("modeMenulist").selectedItem.typeOntMgr;
	var cfgParsArray = new Array();
	var completecfsArray = document.getElementById("modeMenulist").selectedItem.par;
	for(var i=0; i<completecfsArray.length; ++i){
		cfgParsArray[i] = new Object();
		cfgParsArray[i].name = completecfsArray[i].name;
		cfgParsArray[i].value = completecfsArray[i].value;
	}
	
	//perform checks on extension point configurations and collect settings
	/* NOTE: currently this operations must be performed with predetermined order because the newProject
	 * method get precise parameters so they must be distinguished.
	 * We should generalize the request and then this operations will be generalized too.
	 * Note also that this operations are (at the moment) performed only for optional settings, in
	 * case in the future will be added mandatory extention points, they must be performed for
	 * them too (contained in mandatorySettingsBox)
	 */
	var extPointGroupboxList = document.getElementById("optionalSettingsBox").childNodes;
	
	//First groupbox (index 0): UriGenerator extension point
	var uriGenGroupbox = extPointGroupboxList[0];
	//uriGen configuration and parameters
	var uriGenFactoryMenu = uriGenGroupbox.getElementsByTagName("row")[0].getElementsByTagName("menulist")[0];
	var uriGenFactoryID = "---";
	//if optionalSettingsBox is never expanded, the selectedItem of the menu is not initialized, so perform the check
	if (uriGenFactoryMenu.selectedItem != undefined) {
		uriGenFactoryID = uriGenFactoryMenu.selectedItem.getAttribute("id");
	}
	var uriGenConfiguration = null;
	var uriGenParsArray = null;
	if (uriGenFactoryID != "---"){
		//check if configuration with "editRequired" true have been configured
		//still not tested: there are not yet plugin with configuration that require to be configured
		var configurationMenulist = uriGenGroupbox.getElementsByTagName("row")[1].getElementsByTagName("menulist")[0];
		if (configurationMenulist.selectedItem.editRequired == "true") {
			if(art_semanticturkey.openPluginConfiguration(configurationMenulist.selectedItem) == false){
				art_semanticturkey.DisabledAllButton(false);
				return;
			}
		}
		//collect configuration of UriGenerator
		uriGenConfiguration = configurationMenulist.selectedItem.type;
		uriGenParsArray = new Array();
		var uriGenCfgPars = configurationMenulist.selectedItem.par;
		for (var i=0; i<uriGenCfgPars.length; i++){
			uriGenParsArray[i] = new Object();
			uriGenParsArray[i].name = uriGenCfgPars[i].name;
			uriGenParsArray[i].value = uriGenCfgPars[i].value;
		}
	} else {
		uriGenFactoryID = null;
	}
	
	//Second groupbox (index 1): RenderingEngine extension point
	var renderingEngineGroupbox = extPointGroupboxList[1];
	//RenderingEngine configuration and parameters
	var renderingEngineFactoryMenu = renderingEngineGroupbox.getElementsByTagName("row")[0].getElementsByTagName("menulist")[0];
	var renderingEngineFactoryID = "---";
	//if optionalSettingsBox is never expanded, the selectedItem of the menu is not initialized, so perform the check
	if (renderingEngineFactoryMenu.selectedItem != undefined) {
		renderingEngineFactoryID = renderingEngineFactoryMenu.selectedItem.getAttribute("id");
	}
	var renderingEngineConfiguration = null;
	var renderingEngineParsArray = null;
	if (renderingEngineFactoryID != "---"){
		//check if configuration with "editRequired" true have been configured
		//still not tested: there are not yet plugin with configuration that require to be configured
		var configurationMenulist = renderingEngineGroupbox.getElementsByTagName("row")[1].getElementsByTagName("menulist")[0];
		if (configurationMenulist.selectedItem.editRequired == "true") {
			if(art_semanticturkey.openPluginConfiguration(configurationMenulist.selectedItem) == false){
				art_semanticturkey.DisabledAllButton(false);
				return;
			}
		}
		//collect configuration of RenderingEngine
		renderingEngineConfiguration = configurationMenulist.selectedItem.type;
		renderingEngineParsArray = new Array();
		var renderingEngineCfgPars = configurationMenulist.selectedItem.par;
		for (var i=0; i<renderingEngineCfgPars.length; i++){
			renderingEngineParsArray[i] = new Object();
			renderingEngineParsArray[i].name = renderingEngineCfgPars[i].name;
			renderingEngineParsArray[i].value = renderingEngineCfgPars[i].value;
		}
	} else {
		renderingEngineFactoryID = null;
	}
	
	var srcLocalFile = document.getElementById("srcLocalFile").value;
	
	if((projectName == "") || (uri == "")){
		alert("Please specify a name and a URI for the project");
		art_semanticturkey.DisabledAllButton(false);
		return;
	}
	if(ontologyType == ""){
		alert("Please specify a project type");
		art_semanticturkey.DisabledAllButton(false);
		return;
	}	
	var isurl = art_semanticturkey.isUrl(uri);
	if (isurl == false) {
		alert("please type a valid URI \n An example of valid URI is: http://myontology");
		art_semanticturkey.DisabledAllButton(false);
		return;
		
	}
	if ((window.arguments[0].fromFile == true) && (srcLocalFile == "")){
		alert("Please specify a rdf/owl file for the project");
		art_semanticturkey.DisabledAllButton(false);
		return;
	}
	
	try{
		window.arguments[0].parentWindow.art_semanticturkey.closeProject();
		var responseXML;
		//if UriGenerator has not been configured uriGenFactoryID, uriGenConfiguration, uriGenParsArray are null
		if(window.arguments[0].fromFile == false){
			responseXML = art_semanticturkey.STRequests.Projects.newProject(
					projectName, ontologyType, uri,
					tripleStore, ontMgrConfiguration, cfgParsArray,
					uriGenFactoryID, uriGenConfiguration, uriGenParsArray, 
					renderingEngineFactoryID, renderingEngineConfiguration, renderingEngineParsArray);
		} else {
			responseXML = art_semanticturkey.STRequests.Projects.newProjectFromFile(
				projectName, ontologyType, uri,
				tripleStore, ontMgrConfiguration, cfgParsArray,
				uriGenConfiguration, uriGenFactoryID, uriGenParsArray, 
				renderingEngineFactoryID, renderingEngineConfiguration, renderingEngineParsArray, 
				srcLocalFile);
		}
		art_semanticturkey.newProject_RESPONSE(responseXML, projectName, ontologyType);
	}
	catch (e) {
		alert(e.name + ": " + e.message);
		art_semanticturkey.DisabledAllButton(false);
	}
};

art_semanticturkey.newProject_RESPONSE = function(responseElement, projectName, ontologyType){
	window.arguments[0].newProject = true;
	window.arguments[0].newProjectName = projectName;
	window.arguments[0].newProjectOntoType = ontologyType;
	var type = responseElement.getElementsByTagName("type")[0].textContent;
	window.arguments[0].newProjectType = type;
	close();
};

art_semanticturkey.cancel = function() {
	close();
};

art_semanticturkey.DisabledAllButton = function(disabled){
	document.getElementById("newProject").disabled = disabled;
	document.getElementById("cancel").disabled = disabled;
	document.getElementById("dirBtn").disabled = disabled;
};