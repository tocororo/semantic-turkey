if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);

var projectName;

window.onload = function() {
	projectName = window.arguments[0].projectName;
	
	var editRawBtn = document.getElementById("editRawBtn");
	editRawBtn.addEventListener("command", art_semanticturkey.editRawListener, false);
	
	art_semanticturkey.initUI();
}

art_semanticturkey.initUI = function() {
	//get property value mapping and fill the UI
	var listbox = document.getElementById("listbox");
	//clear listbox
	var count = listbox.itemCount;
    while(count > 0){
        listbox.removeItemAt(0);
        count--;
    }
    
	var xmlResp = art_semanticturkey.STRequests.Projects.getProjectPropertyMap(projectName);
	var data = xmlResp.getElementsByTagName("data")[0];
	var property = data.getElementsByTagName("property");
	
	for (var i=0; i<property.length; i++){
		var name = property[i].getAttribute("name");
		var value = property[i].getAttribute("value");
		
		var listitem = document.createElement("listitem");
		var cellName = document.createElement("listcell");
		cellName.setAttribute("label", name);
	    listitem.appendChild(cellName);
	    var cellValue = document.createElement("listcell");
	    cellValue.setAttribute("label", value);
	    listitem.addEventListener("dblclick", art_semanticturkey.dblClickListener, false);
	    listitem.appendChild(cellValue);
	    listbox.appendChild(listitem);
	}
}

art_semanticturkey.dblClickListener = function() {
	var item = this;
	var name = item.childNodes[0].getAttribute("label");
	var value = item.childNodes[1].getAttribute("label");
	var parameters = {
			name : name,
			value : value,
			projectName : projectName
		};
	parameters.name = name;
	parameters.value = value;
	window.openDialog('chrome://semantic-turkey/content/projects/projectPropertiesEditor/valueEditor.xul',
			 '_blank', 'chrome,dependent,dialog,modal=yes,resizable,centerscreen', parameters);
	
	item.childNodes[1].setAttribute("label", parameters.value);
}

art_semanticturkey.editRawListener = function() {
	var parameters = {
			changeApplied : false,
			projectName : projectName
	};
	window.openDialog('chrome://semantic-turkey/content/projects/projectPropertiesEditor/rawEditor.xul',
			 '_blank', 'chrome,dependent,dialog,modal=yes,resizable,centerscreen', parameters);
	//update the UI in case of property change
	if (parameters.changeApplied){
		art_semanticturkey.initUI();
	}
}

