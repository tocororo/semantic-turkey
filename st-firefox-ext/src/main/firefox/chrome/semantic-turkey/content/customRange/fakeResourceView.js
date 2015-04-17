if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Resource.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_ResourceView.jsm", art_semanticturkey);

window.onload = function() {
	var mainbox = document.getElementById("mainbox");

	var subject = window.arguments[0].subject;
	
	var getResViewXmlResp = art_semanticturkey.STRequests.ResourceView.getResourceView(subject);
	var predicateObjects = getResViewXmlResp.getElementsByTagName("predicateObjects");
	for (var i=0; i<predicateObjects.length; i++){
		var predObjXml = predicateObjects[i];
		mainbox.appendChild(createGroupboxForPredicate(predObjXml));
	}
}

createGroupboxForPredicate = function(predObjXml) {
	var predicateUriXml = predObjXml.getElementsByTagName("predicate")[0].getElementsByTagName("uri")[0];
	var predicate = predicateUriXml.textContent;
	var predQName = predicateUriXml.getAttribute("show");
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
			if (predicateUriXml.getAttribute("hasCustomRange") == "true"){
				//in case get the description
				var reifiedRes = objectsColl[i].textContent;
				objectsBox.appendChild(crateRowForReifiedRes(reifiedRes, predicate));
			} else {//if not, don't expand the resource
				var label = document.createElement("label");
				label.setAttribute("value", objectsColl[i].textContent);
				label.addEventListener("dblclick", function(){
					art_semanticturkey.ResourceViewLauncher.openResourceView(this.value);
				}, false);
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

crateRowForReifiedRes = function(reifiedRes, predicate){
	var xmlResp = art_semanticturkey.STRequests.CustomRanges.getReifiedResourceDescription(reifiedRes, predicate);
	var resource = xmlResp.getElementsByTagName("description")[0].getAttribute("resource");
	var resourceShow = xmlResp.getElementsByTagName("description")[0].getAttribute("show");
	
	var propertyCollXml = xmlResp.getElementsByTagName("property");
	if (propertyCollXml.length > 0){ //create the expandable section only if the description has some prop-value pairs
		var container = document.createElement("vbox");
		//box with two children: label (for resource uri) and toolbarbutton (for preview of description)
		var reifIdBox = document.createElement("hbox");
		reifIdBox.setAttribute("align", "center");
		var resLabel = document.createElement("label");
		resLabel.setAttribute("value", resourceShow);
		resLabel.setAttribute("resource", resource);
		resLabel.setAttribute("flex", "1");
		resLabel.addEventListener("dblclick", function(){
			art_semanticturkey.ResourceViewLauncher.openResourceView(this.getAttribute("resource"));
		}, false);
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
		//create and add grid rows (property-value)
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
	} else {//if the resource doesn't have a description, return just a label (uri of the resource)
		var label = document.createElement("label");
		label.setAttribute("value", resourceShow);
		label.setAttribute("resource", resource);
		label.addEventListener("dblclick", function(){
			art_semanticturkey.ResourceViewLauncher.openResourceView(this.getAttribute("resource"));
		}, false);
		return label;
		art_semanticturkey.Logger.debug(lable.innerHTML);
	}
}





