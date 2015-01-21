if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

var projectName;
var currentLockLevel;
var projectConsumerAndACLList;
var aclMenuIdxs = { RW:0, R:1 };

var projectElement;
var lockElement;

window.onload = function() {
	projectName = window.arguments[0].projectName;
	//do request and retrieve the DOM element about the requested project
	var xmlResp = art_semanticturkey.STRequests.Projects.getAccessStatusMap();
	var dataElement = xmlResp.getElementsByTagName("data")[0];
	var projectElements = dataElement.getElementsByTagName("project");
	for (var i=0; i<projectElements.length; i++){
		var name = projectElements[i].getAttribute("name");
		if (name == projectName){
			projectElement = projectElements[i];
			var consumerElements = projectElement.getElementsByTagName("consumer");
			projectConsumerAndACLList = new Array();
			for (var j=0; j<consumerElements.length; j++){
				var consumerName = consumerElements[j].getAttribute("name");
				var aclLevel = consumerElements[j].getAttribute("availableACLLevel");
				projectConsumerAndACLList[j] = new Object();
				projectConsumerAndACLList[j].consumerName = consumerName;
				projectConsumerAndACLList[j].aclLevel = aclLevel;
			}
			currentLockLevel = projectElement.getElementsByTagName("lock")[0].getAttribute("availableLockLevel");
		}
	}
	document.getElementById("aclEditor").setAttribute("title", "ACL editor ("+projectName+")");
	document.getElementById("lockMenu").addEventListener("command", art_semanticturkey.lockMenuListener, false);
	
	art_semanticturkey.initUI();
}

art_semanticturkey.initUI = function() {
	window.document.documentElement.minWidth = 300;
	var rows = document.getElementById("aclRows"); //rows of the ACL grid
	for (var i=0; i<projectConsumerAndACLList.length; i++){
		var row = document.createElement("row");
		row.setAttribute("align", "center");
		var label = document.createElement("label");
		label.setAttribute("value", projectConsumerAndACLList[i].consumerName);
		row.appendChild(label);
		var menulist = document.createElement("menulist");
		menulist.addEventListener("command", art_semanticturkey.aclMenuListener, false);
		var menupopup = document.createElement("menupopup");
		var menuitem = document.createElement("menuitem");
		menuitem.setAttribute("label", "RW");
		menuitem.setAttribute("value", "RW");
		menupopup.appendChild(menuitem);
		menuitem = document.createElement("menuitem");
		menuitem.setAttribute("label", "R");
		menuitem.setAttribute("value", "R");
		menupopup.appendChild(menuitem);
		menulist.appendChild(menupopup);
		menulist.value = projectConsumerAndACLList[i].aclLevel;
		row.appendChild(menulist);
		rows.appendChild(row);
	}
	var lockMenu = document.getElementById("lockMenu");
	lockMenu.value = currentLockLevel;
}

art_semanticturkey.lockMenuListener = function(){
	var menulist = this;//this in an actionListener represents the target of the listener (exactly the menulist)
	var newLockLevel = menulist.selectedItem.label;
	if (newLockLevel != currentLockLevel){
		if (window.confirm("Are you sure to change project lock level to '" + newLockLevel + "'?")){
			try {
				art_semanticturkey.Logger.debug("update lock level of " + projectName + " to " + newLockLevel);
				art_semanticturkey.STRequests.Projects.updateLockLevel(projectName, newLockLevel);
				newLockLevel = currentLockLevel;
			} catch (e) {
				art_semanticturkey.Alert.alert(e);
				menulist.value = currentLockLevel;
			}
		} else {//undo the menulist change
			menulist.value = currentLockLevel;
		}
	}
}

art_semanticturkey.aclMenuListener = function(){
	var menulist = this;//this in an actionListener represents the target of the listener (exactly the menulist)
	var selectedItem = menulist.selectedItem;
	var newAclLevel = selectedItem.label;
	var row = menulist.parentNode;//from menulist to row
	var projectConsumerName = row.childNodes[0].value;
	for (var i=0; i<projectConsumerAndACLList.length; i++){
		if (projectConsumerAndACLList[i].consumerName == projectConsumerName){
			if (projectConsumerAndACLList[i].aclLevel != newAclLevel){
				if (window.confirm("Are you sure to change project Access level to '" + newAclLevel + "' for the consumer '" + projectConsumerName + "'?")){
					try {
						art_semanticturkey.Logger.debug("update access level of " + projectName + " to " + projectConsumerName + " at " + newAclLevel);
						art_semanticturkey.STRequests.Projects.updateAccessLevel(projectName, projectConsumerName, newAclLevel);
						projectConsumerAndACLList[i].aclLevel = newAclLevel; //update the acl level in the map
					} catch (e) {
						art_semanticturkey.Alert.alert(e);
						menulist.selectedIndex = aclMenuIdxs[projectConsumerAndACLList[i].aclLevel];
					}
				} else {
					menulist.selectedIndex = aclMenuIdxs[projectConsumerAndACLList[i].aclLevel];
				}
			} else return;
		}
	}
}
