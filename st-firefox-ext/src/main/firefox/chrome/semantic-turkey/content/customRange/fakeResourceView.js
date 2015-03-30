if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
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
		art_semanticturkey.Logger.debug("check " + objectsColl[i].textContent + " is uri or literal");
		if (objectsColl[i].tagName == "uri"){
			//if uri, check if its property has a CustomRange
			if (propsWithCR.indexOf(predicate) != -1){
				//in case get the description
				var reifiedRes = objectsColl[i].textContent;
				objectsBox.appendChild(crateGroupboxForReifiedRes(reifiedRes, predicate));
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
		var row = document.createElement("row");
		row.setAttribute("align", "center");
		var label = document.createElement("label");
		label.setAttribute("value", propertyCollXml[i].getAttribute("show"));
		row.appendChild(label);
		var txtbox = document.createElement("textbox");
		txtbox.setAttribute("value", propertyCollXml[i].getElementsByTagName("object")[0].getAttribute("value"));
		txtbox.setAttribute("readonly", "true");
		row.appendChild(txtbox);
		gridRows.appendChild(row);
		resourceInfoGrid.appendChild(gridRows);
	}
	groupbox.appendChild(resourceInfoGrid);
	
	captExpandBtn.addEventListener("command", function(){
		if (captExpandBtn.getAttribute("label") == "+"){
			captExpandBtn.setAttribute("label", "-");
			captExpandBtn.setAttribute("tooltiptext", "Collapse");
		} else {
			captExpandBtn.setAttribute("label", "+");
			captExpandBtn.setAttribute("tooltiptext", "Expand");
		}
		if (resourceInfoGrid.getAttribute("hidden") == "true")
			resourceInfoGrid.setAttribute("hidden", "false");
		else 
			resourceInfoGrid.setAttribute("hidden", "true");
	}, false);
	
	return groupbox;
}





