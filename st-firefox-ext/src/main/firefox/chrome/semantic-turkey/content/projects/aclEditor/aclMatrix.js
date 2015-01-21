if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Projects.jsm", art_semanticturkey);

var dataElement;
var projectList = [];
var consumerList = [];

window.onload = function() {
	var xmlResp = art_semanticturkey.STRequests.Projects.getAccessStatusMap();
	dataElement = xmlResp.getElementsByTagName("data")[0];
	var projects = dataElement.getElementsByTagName("project");
	for (var i=0; i<projects.length; i++){
		projectList.push(projects[i].getAttribute("name"));
	}
	consumerList = projectList.slice(0);
	consumerList.unshift("SYSTEM");
	
	art_semanticturkey.initUI();
}

art_semanticturkey.initUI = function() {
	var grid = document.getElementById("grid");
	var columns = document.createElement("columns");
	var rows = document.createElement("rows");
	//columns creation
	for (var i=0; i<consumerList.length+1; i++){//+1 counting first column (for projects name)
		var column = document.createElement("column");
		column.setAttribute("class", "gridCol");
		columns.appendChild(column);
	}
	grid.appendChild(columns);
	//first row (headers) creation
	var row = document.createElement("row");
	row.setAttribute("class", "gridRow");
	var label = document.createElement("label");//first empty header
	row.appendChild(label);
	for (var i=0; i<consumerList.length; i++){
		label = document.createElement("label");
		label.setAttribute("class", "headerLabel");
		label.setAttribute("crop", "end");
		label.setAttribute("value", consumerList[i]);
		label.setAttribute("tooltiptext", consumerList[i]);
		row.appendChild(label);
	}
	rows.appendChild(row);
	//grid content rows
	var projects = dataElement.getElementsByTagName("project");
	for (var i=0; i<projects.length; i++){
		var project = projects[i];
		var projectName = project.getAttribute("name");
		var lock = project.getElementsByTagName("lock")[0];
		var availableLockLevel = lock.getAttribute("availableLockLevel");
		var lockingConsumer = lock.getAttribute("lockingConsumer");
		
		row = document.createElement("row");
		row.setAttribute("class", "gridRow");
		label = document.createElement("label");//first cell (project name)
		label.setAttribute("value", projectName);
		label.setAttribute("class", "headerLabel");
		row.appendChild(label);
		
		var consumers = project.getElementsByTagName("consumer");
		for (var j=0; j<consumerList.length; j++){
			var innerGrid = document.createElement("grid");
			var columnName = consumerList[j];
			if (columnName != projectName){//to avoid to fill the cell where the consumer is the project itself  
				for (var k=0; k<consumers.length; k++){
					var consumer = consumers[k];
					var consumerName = consumer.getAttribute("name");
					if (columnName == consumerName){
						//fill inner grid
						var innerCols = document.createElement("columns");
						var innerCol = document.createElement("column");
						innerCol.setAttribute("class", "firstInnerGridCol");
						innerCols.appendChild(innerCol);
						innerCol = document.createElement("column");
						innerCol.setAttribute("class", "innerGridCol");
						innerCols.appendChild(innerCol);
						innerGrid.appendChild(innerCols);
						var innerRows = document.createElement("rows");
						//first row (ACL)
						var innerRow = document.createElement("row");
						innerRow.setAttribute("class", "firstInnerRow");
						var label = document.createElement("label"); //first cell: ACL to the consumer
						var availableACLLevel = consumer.getAttribute("availableACLLevel")
						label.setAttribute("value", availableACLLevel);
						label.setAttribute("tooltiptext", getAvailableACLTooltip(projectName, consumerName, availableACLLevel));
						innerRow.appendChild(label);
						var textbox = document.createElement("textbox");//second cell: (optional) access level which with consumer accesses the project
						textbox.setAttribute("disabled", "true");
						var acquiredACLLevel = consumer.getAttribute("acquiredACLLevel");
						if (acquiredACLLevel != null){
							textbox.setAttribute("value", acquiredACLLevel);
							textbox.setAttribute("class", "aclTextbox");
							textbox.setAttribute("tooltiptext", getAcquiredACLTooltip(projectName, consumerName, availableACLLevel));
						} else {
							textbox.setAttribute("class", "disabledStatusTextbox");
							textbox.setAttribute("tooltiptext", projectName + " is not accessed by " + consumerName);
						}
						innerRow.appendChild(textbox);
						innerRows.appendChild(innerRow);//append 1st row
						//second row (Lock level)
						innerRow = document.createElement("row");
						label = document.createElement("label");
						var availableLockLevel = lock.getAttribute("availableLockLevel");
						label.setAttribute("value", availableLockLevel);
						label.setAttribute("tooltiptext", getAvailableLockTooltip(projectName, availableLockLevel));
						innerRow.appendChild(label);
						textbox = document.createElement("textbox");
						textbox.setAttribute("disabled", "true");
						var acquiredLockLevel = lock.getAttribute("acquiredLockLevel")
						if (acquiredLockLevel != null && lockingConsumer == columnName) {
							textbox.setAttribute("value", acquiredLockLevel);
							textbox.setAttribute("class", "lockTextbox");
							textbox.setAttribute("tooltiptext", getAcquiredLockTooltip(projectName, consumerName, acquiredLockLevel));
						} else {
							textbox.setAttribute("class", "disabledStatusTextbox");
							textbox.setAttribute("tooltiptext", projectName + " is not locked by " + consumerName);
						}
						innerRow.appendChild(textbox);
						innerRows.appendChild(innerRow);//append 2nd row
						innerGrid.appendChild(innerRows);
						break; //stop searching the xml section of the current consumer
					}
				}
			}
			row.appendChild(innerGrid);
		}
		rows.appendChild(row);
	}
	grid.appendChild(rows);		
}

function getAvailableACLTooltip(projectName, consumerName, availableACLLevel){
	if (availableACLLevel == "R"){
		return projectName + " grants only read access level to " + consumerName;
	} else {//RW
		return projectName + " grants read and write access level to " + consumerName;
	}
}

function getAcquiredACLTooltip(projectName, consumerName, availableACLLevel){
	if (availableACLLevel == "R"){
		return projectName + " is accessed by " + consumerName + " in read level";
	} else {//RW
		return projectName + " is accessed by " + consumerName + " in read and write level";
	}
}

function getAvailableLockTooltip(projectName, availableLockLevel){
	if (availableLockLevel == "NO"){
		return projectName + " cannot be locked by any consumer";
	} else if (availableLockLevel == "R"){
		return projectName + " can be locked to prevent writing operations by other consumers";
	} else {//W
		return projectName + " can be locked to prevent any operations by other consumers";
	}
}

function getAcquiredLockTooltip(projectName, consumerName, acquiredLockLevel){
	if (acquiredLockLevel == "NO"){
		return projectName + " is not locked by " + consumerName;
	} else if (acquiredLockLevel == "R"){
		return projectName + " is locked by " + consumerName + " in read level";
	} else {//W
		return projectName + " is locked by " + consumerName + " in write level";
	}
}
	
//	var grid = document.getElementById("grid");
//	var columns = document.createElement("columns");
//	for (var i=0; i<projectConsumerList.length+1; i++){//+1 counting first empty column (dedicated to projects names)
//		var column = document.createElement("column");
//		column.setAttribute("class", "gridCol");
//		columns.appendChild(column);
//		art_semanticturkey.Logger.debug("created " + (i+1) + " column");
//	}
//	grid.appendChild(columns);
//	var rows = document.createElement("rows");
//	//first row (headers)
//	var row = document.createElement("row");
//	row.setAttribute("class", "gridRow");
//	var label = document.createElement("label");//first empty header
//	row.appendChild(label);
//	for (var i=0; i<projectConsumerList.length; i++){
//		label = document.createElement("label");
//		label.setAttribute("class", "headerLabel");
//		label.setAttribute("value", projectConsumerList[i]);
//		row.appendChild(label);
//	}
//	//other rows
//	rows.appendChild(row);
//	for (var i=0; i<projectList.length; i++){
//		row = document.createElement("row");
//		row.setAttribute("class", "gridRow");
//		label = document.createElement("label");//first cell (project name)
//		label.setAttribute("value", projectList[i]);
//		label.setAttribute("class", "headerLabel");
//		row.appendChild(label);
//		//
//		for (var j=0; j<projectConsumerList.length; j++){
//			var innerGrid = document.createElement("grid");
//			var innerCols = document.createElement("columns");
//			var innerCol = document.createElement("column");
//			innerCol.setAttribute("class", "firstInnerCol");
//			innerCols.appendChild(innerCol);
//			innerCol = document.createElement("column");
//			innerCols.appendChild(innerCol);
//			innerGrid.appendChild(innerCols);
//			var innerRows = document.createElement("rows");
//			//first row (ACL)
//			var innerRow = document.createElement("row");
//			innerRow.setAttribute("class", "firstInnerRow");
//			label = document.createElement("label");
//			label.setAttribute("value", "RW");
//			innerRow.appendChild(label);
//			var textbox = document.createElement("textbox");
//			textbox.setAttribute("value", "RW");
//			textbox.setAttribute("class", "aclTextbox");
//			textbox.setAttribute("disabled", "true");
//			innerRow.appendChild(textbox);
//			innerRows.appendChild(innerRow);
//			//second row (lock level)
//			innerRow = document.createElement("row");
//			label = document.createElement("label");
//			label.setAttribute("value", "NO");
//			innerRow.appendChild(label);
//			textbox = document.createElement("textbox");
//			textbox.setAttribute("value", "NO");
//			textbox.setAttribute("class", "lockTextbox");
//			textbox.setAttribute("disabled", "true");
//			innerRow.appendChild(textbox);
//			innerRows.appendChild(innerRow);
//			innerGrid.appendChild(innerRows);
//			
//			row.appendChild(innerGrid);
//		}
//		rows.appendChild(row);
//	}
//	grid.appendChild(rows);
//	
//}
