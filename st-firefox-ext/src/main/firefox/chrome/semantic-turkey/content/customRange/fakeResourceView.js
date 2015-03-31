if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Resource.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);

var propsWithCR = [];

window.onload = function() {
	var mainbox = document.getElementById("mainbox");

	var subject = window.arguments[0].subject;
	
	var crPropPairsXml = art_semanticturkey.STRequests.CustomRanges.getCustomRangeConfigMap().getElementsByTagName("configEntry");
	for (var i=0; i<crPropPairsXml.length; i++){
		propsWithCR.push(crPropPairsXml[i].getAttribute("property"));
	}
	
	var getResViewXmlResp = art_semanticturkey.STRequests.ResourceView.getResourceView(subject);
	var predicateObjects = getResViewXmlResp.getElementsByTagName("predicateObjects");
	for (var i=0; i<predicateObjects.length; i++){
		var predObjXml = predicateObjects[i];
		mainbox.appendChild(createGroupboxForPredicate(predObjXml));
	}
}

createGroupboxForPredicate = function(predObjXml) {
	var predicate = predObjXml.getElementsByTagName("predicate")[0].getElementsByTagName("uri")[0].textContent;
	var predQName = predObjXml.getElementsByTagName("predicate")[0].getElementsByTagName("uri")[0].getAttribute("show");
	//create the groupbox
	var groupbox = document.createElement("groupbox");
	var caption = document.createElement("caption");
	caption.setAttribute("label", predQName);
	caption.setAttribute("style", "font-weight:bold");
	groupbox.appendChild(caption);
	//create the box to contain the values (flat or reified)
	var objectsBox = document.createElement("vbox");
	
	var objectsColl = predObjXml.getElementsByTagName("objects")[0].getElementsByTagName("collection")[0].children;
	//for every object check if is flat or uri
	for (var i=0; i<objectsColl.length; i++){
		if (objectsColl[i].tagName == "uri"){
			//if uri, check if its property has a CustomRange
			if (propsWithCR.indexOf(predicate) != -1){
				//in case get the description
				var reifiedRes = objectsColl[i].textContent;
//				objectsBox.appendChild(crateGroupboxForReifiedRes(reifiedRes, predicate));
				objectsBox.appendChild(crateRowForReifiedRes(reifiedRes, predicate));
			} else {//if not, don't expand the resource
				var label = document.createElement("label");
				label.setAttribute("value", objectsColl[i].textContent);
				objectsBox.appendChild(label);
			}
		} else if (objectsColl[i].tagName == "plainLiteral"){
			var label = document.createElement("label");
			label.setAttribute("value", objectsColl[i].textContent);
			label.setAttribute("tooltiptext", "\"" + objectsColl[i].textContent + "\"@" + objectsColl[i].getAttribute("lang"));
			objectsBox.appendChild(label);
		} else if (objectsColl[i].tagName == "typedLiteral"){
			var label = document.createElement("label");
			label.setAttribute("value", objectsColl[i].textContent);
			label.setAttribute("tooltiptext", "\"" + objectsColl[i].textContent + "\"^^" + objectsColl[i].getAttribute("type"));
			objectsBox.appendChild(label);
		}
	}
	groupbox.appendChild(objectsBox);
	return groupbox;
}

crateGroupboxForReifiedRes = function(reifiedRes, predicate){
	var xmlResp = art_semanticturkey.STRequests.CustomRanges.getReifiedResDescription(reifiedRes, predicate);
	var resource = xmlResp.getElementsByTagName("description")[0].getAttribute("resource"); 
	
	var groupbox = document.createElement("groupbox");
	var caption = document.createElement("caption");
	caption.setAttribute("align", "center");
	var captLabel = document.createElement("label");
	captLabel.setAttribute("value", resource);
	captLabel.addEventListener("dblclick", function(){
		art_semanticturkey.ResourceViewLauncher.openResourceView(this.value);
	}, false);
	var captExpandBtn = document.createElement("toolbarbutton");//it would be better as toolbarbutton with image that change when open and when close
	captExpandBtn.setAttribute("label", "+");
	captExpandBtn.setAttribute("tooltiptext", "Expand");
	caption.appendChild(captLabel);
	caption.appendChild(captExpandBtn);
	groupbox.appendChild(caption);
	//creazione grid espandibile
	var resourceInfoGrid = document.createElement("grid");
	resourceInfoGrid.setAttribute("hidden", "true");
	var gridColumns = document.createElement("columns");
	var gridColumn = document.createElement("column");
	gridColumns.appendChild(gridColumn);
	gridColumn = document.createElement("column");
	gridColumn.setAttribute("flex", "1");
	gridColumns.appendChild(gridColumn);
	resourceInfoGrid.appendChild(gridColumns);
	var gridRows = document.createElement("rows");
	//creazione e aggiunta row interne alla griglia
	var propertyCollXml = xmlResp.getElementsByTagName("property");
	for (var i=0; i<propertyCollXml.length; i++){
		var objectCollXml = propertyCollXml[i].getElementsByTagName("object")[0].childNodes;
		for (var j=0; j<objectCollXml.length; j++){
			if (typeof (objectCollXml[j].tagName) != 'undefined') {
				var row = document.createElement("row");
				row.setAttribute("align", "center");
				var label = document.createElement("label");
				label.setAttribute("value", propertyCollXml[i].getAttribute("show"));
				row.appendChild(label);
				var txtbox = document.createElement("textbox");
				txtbox.setAttribute("readonly", "true");
				txtbox.setAttribute("value", objectCollXml[j].textContent);
				var tooltiptext = objectCollXml[j].textContent;
				if (objectCollXml[j].tagName == 'typedLiteral') {
					tooltiptext = "\"" + tooltiptext + "\"^^"+objectCollXml[j].getAttribute("datatype");
				} else if (objectCollXml[j].tagName == 'plainLiteral') {
					if (objectCollXml[j].getAttribute("lang") != null){
						tooltiptext = "\"" + tooltiptext + "\"@"+objectCollXml[j].getAttribute("lang");
					}
				} else if (objectCollXml[j].tagName == 'uri') {
					txtbox.addEventListener("dblclick", function(){
						art_semanticturkey.ResourceViewLauncher.openResourceView(this.value);
					}, false);
				}
				txtbox.setAttribute("tooltiptext", tooltiptext);
				row.appendChild(txtbox);
				gridRows.appendChild(row);
			}
		}
		resourceInfoGrid.appendChild(gridRows);
	}
	groupbox.appendChild(resourceInfoGrid);
	
	captExpandBtn.addEventListener("command", function(){
		if (this.getAttribute("label") == "+"){
			this.setAttribute("label", "-");
			this.setAttribute("tooltiptext", "Collapse");
		} else {
			this.setAttribute("label", "+");
			this.setAttribute("tooltiptext", "Expand");
		}
		if (resourceInfoGrid.getAttribute("hidden") == "true")
			resourceInfoGrid.setAttribute("hidden", "false");
		else 
			resourceInfoGrid.setAttribute("hidden", "true");
	}, false);
	
	return groupbox;
}


crateRowForReifiedRes = function(reifiedRes, predicate){
	var xmlResp = art_semanticturkey.STRequests.CustomRanges.getReifiedResDescription(reifiedRes, predicate);
	var resource = xmlResp.getElementsByTagName("description")[0].getAttribute("resource");
	
	var container = document.createElement("vbox");
	//box with two children: label (for resource uri) and toolbarbutton (for preview of description)
	var reifIdBox = document.createElement("hbox");
	reifIdBox.setAttribute("align", "center");
	var resLabel = document.createElement("label");
	resLabel.setAttribute("value", resource);
	resLabel.setAttribute("flex", "1");
	var expandBtn = document.createElement("toolbarbutton");
	expandBtn.setAttribute("tooltiptext", "Expand");
	expandBtn.setAttribute("label", "+");
	reifIdBox.appendChild(resLabel);
	reifIdBox.appendChild(expandBtn);
	
	var refInfoGroupbox = document.createElement("groupbox");
	refInfoGroupbox.setAttribute("hidden", "true");
	//description grid creation
	var resourceInfoGrid = document.createElement("grid");
	var gridColumns = document.createElement("columns");
	var gridColumn = document.createElement("column");
	gridColumns.appendChild(gridColumn);
	gridColumn = document.createElement("column");
	gridColumn.setAttribute("flex", "1");
	gridColumns.appendChild(gridColumn);
	resourceInfoGrid.appendChild(gridColumns);
	var gridRows = document.createElement("rows");
	//creazione e aggiunta row interne alla griglia
	var propertyCollXml = xmlResp.getElementsByTagName("property");
	for (var i=0; i<propertyCollXml.length; i++){
		var objectCollXml = propertyCollXml[i].getElementsByTagName("object")[0].childNodes;
		for (var j=0; j<objectCollXml.length; j++){
			if (typeof (objectCollXml[j].tagName) != 'undefined') {
				var row = document.createElement("row");
				row.setAttribute("align", "center");
				var label = document.createElement("label");
				label.setAttribute("value", propertyCollXml[i].getAttribute("show"));
				row.appendChild(label);
				var txtbox = document.createElement("textbox");
				txtbox.setAttribute("readonly", "true");
				txtbox.setAttribute("value", objectCollXml[j].textContent);
				var tooltiptext = objectCollXml[j].textContent;
				if (objectCollXml[j].tagName == 'typedLiteral') {
					tooltiptext = "\"" + tooltiptext + "\"^^"+objectCollXml[j].getAttribute("datatype");
				} else if (objectCollXml[j].tagName == 'plainLiteral') {
					if (objectCollXml[j].getAttribute("lang") != null){
						tooltiptext = "\"" + tooltiptext + "\"@"+objectCollXml[j].getAttribute("lang");
					}
				} else if (objectCollXml[j].tagName == 'uri') {
					txtbox.addEventListener("dblclick", function(){
						art_semanticturkey.ResourceViewLauncher.openResourceView(this.value);
					}, false);
				}
				txtbox.setAttribute("tooltiptext", tooltiptext);
				row.appendChild(txtbox);
				gridRows.appendChild(row);
			}
		}
		resourceInfoGrid.appendChild(gridRows);
	}
	refInfoGroupbox.appendChild(resourceInfoGrid);

	container.appendChild(reifIdBox);
	container.appendChild(refInfoGroupbox);
	
	expandBtn.addEventListener("command", function(){
		if (this.getAttribute("label") == "+"){
			this.setAttribute("label", "-");
			this.setAttribute("tooltiptext", "Collapse");
		} else {
			this.setAttribute("label", "+");
			this.setAttribute("tooltiptext", "Expand");
		}
		if (refInfoGroupbox.getAttribute("hidden") == "true")
			refInfoGroupbox.setAttribute("hidden", "false");
		else 
			refInfoGroupbox.setAttribute("hidden", "true");
	}, false);
	
	return container;
}





