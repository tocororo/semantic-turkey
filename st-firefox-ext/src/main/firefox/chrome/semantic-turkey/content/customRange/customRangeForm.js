if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stservices/SERVICE_XMLSchema.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

window.onload = function() {
	document.getElementById("okBtn").addEventListener("command", buttonOkListener, false);
	var crEntryXml = window.arguments[0].crEntryXml;
	var formXml = crEntryXml.getElementsByTagName("form")[0];
	/* if the pearl has some error, the form tag will contain the thrown exception in a "exception"
	 * attribute and there will be no "formEntry" tags. Here perform the check */
	var formEntriesXml = formXml.getElementsByTagName("formEntry");
	if (formEntriesXml.length > 0){ //there are formEntry
		for (var i=0; i<formEntriesXml.length; i++){
			addFieldToForm(formEntriesXml[i]);
		}
	} else { //there aren't formEntry, then there is exception in form tag
		art_semanticturkey.Alert.alert("Error in pearl code of the custom range with id '" +
				crEntryXml.getAttribute("id") + "'", formXml.getAttribute("exception"));
		window.close();
	}
	window.sizeToContent();
}

addFieldToForm = function(formEntryXml){
	var type = formEntryXml.getAttribute("type");
	if (type == "literal"){
		var datatype = formEntryXml.getAttribute("datatype");
		if (datatype != null){
			if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#date".toLowerCase()){
				createDateInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#dateTime".toLowerCase()){
				createDatetimeInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#time".toLowerCase()){
				createTimeInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#float".toLowerCase()){
				createFloatInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#integer".toLowerCase()){
				createIntegerInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#boolean".toLowerCase()){
				createBooleanInput(formEntryXml);
			} else { //for every other case
				createGenericInput(formEntryXml);
			}
		} else {
			createGenericInput(formEntryXml);
		}
	} else { //type == "uri"
		createGenericInput(formEntryXml);
	}
}

createGenericInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	var datatype = formEntryXml.getAttribute("datatype");
	if (datatype != null){
		textbox.setAttribute("tooltiptext", datatype);
	}
	mainBox.appendChild(textbox);
	var mandatory = (formEntryXml.getAttribute("mandatory") === "true");
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
}

createDatetimeInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	mainBox.appendChild(textbox);
	var datepicker = document.createElement("datepicker");
	var timepicker = document.createElement("timepicker");
	mainBox.appendChild(datepicker);
	mainBox.appendChild(timepicker);
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
	textbox.setAttribute("value", getDatetime(datepicker, timepicker));
}

createDateInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	mainBox.appendChild(textbox);
	var datepicker = document.createElement("datepicker");
	datepicker.addEventListener("command", function(){
		textbox.setAttribute("value", getDate(datepicker));
	}, false);
	mainBox.appendChild(datepicker);	
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
	textbox.setAttribute("value", getDate(datepicker));
}

createTimeInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	mainBox.appendChild(textbox);
	var timepicker = document.createElement("timepicker");
	timepicker.addEventListener("command", function(){
		textbox.setAttribute("value", getTime(timepicker));
	}, false);
	mainBox.appendChild(timepicker);
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
	textbox.setAttribute("value", getTime(timepicker));
}

createFloatInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("type", "number");
	textbox.setAttribute("decimalplaces", "Infinity");
	textbox.setAttribute("hidespinbuttons", "true");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	mainBox.appendChild(textbox);
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
}

createIntegerInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("type", "number");
	textbox.setAttribute("hidespinbuttons", "true");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	mainBox.appendChild(textbox);
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
}

createBooleanInput = function(formEntryXml){
	var gridrows = document.getElementById("gridrows");
	var row = document.createElement("row");
	row.setAttribute("align", "center");
	//1st child: userPromptLabel
	row.appendChild(createLabelForEntry(formEntryXml));
	//2nd child: input box
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	mainBox.appendChild(textbox);
	var radiogroup = document.createElement("radiogroup");
	radiogroup.setAttribute("orient", "horizontal");
	var radio = document.createElement("radio");
	radio.setAttribute("label", "true");
	radio.setAttribute("selected", "true");
	radiogroup.appendChild(radio);
	radio = document.createElement("radio");
	radio.setAttribute("label", "false");
	radiogroup.appendChild(radio);
	radiogroup.addEventListener("command", function(){
		textbox.setAttribute("value", radiogroup.selectedItem.label);
	}, false);
	mainBox.appendChild(radiogroup);
	row.appendChild(mainBox);
	//3rd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
	textbox.setAttribute("value", radiogroup.selectedItem.label);
}

/**
 * Creates and returns a label Element for the given form entry
 * @param formEntryXml
 * @returns
 */
createLabelForEntry = function(formEntryXml){
	var userPrompt = formEntryXml.getAttribute("userPrompt");
	var label = document.createElement("label");
	label.setAttribute("value", userPrompt);
	return label;
}

/**
 * Adds an Element to the given row. The Element could be a simple label (*) if the entry is 
 * mandatory, or a checkbox if the entry is optional
 * @param row
 * @param formEntryXml
 */
addMandatoryField = function(row, formEntryXml){
	var mandatory = (formEntryXml.getAttribute("mandatory") === "true");
	if (mandatory){
		var mandLbl = document.createElement("label");
		mandLbl.setAttribute("value", "*");
		row.appendChild(mandLbl);
	} else {
		var check = document.createElement("checkbox");
		check.setAttribute("checked", "true");
		check.setAttribute("tooltiptext", "optional field, check to insert");
		row.appendChild(check);
		check.addEventListener("command", optionalChecboxListener, false);
	}
}

getDate = function(dp){
	var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDate(dp.year, dp.month, dp.date);
	var date = xmlResp.getElementsByTagName("date")[0].textContent;
	return date;
}

getTime = function(tp){
	var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatTime(tp.hour, tp.minute, tp.second);
	var time = xmlResp.getElementsByTagName("time")[0].textContent;
	return time;
}

getDatetime = function(dp, tp){
	var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDateTime(
			dp.year, dp.month, dp.date, tp.hour, tp.minute, tp.second);
	var dt = xmlResp.getElementsByTagName("dateTime")[0].textContent;
	return dt;
}

optionalChecboxListener = function(){
	var row = this.parentNode;
	var inputBox = row.childNodes[1];
	var inputBoxChild = inputBox.childNodes;
	for (var i=0; i<inputBoxChild.length; i++){
		if (this.checked){
			inputBoxChild[i].disabled = false;
		} else {
			inputBoxChild[i].disabled = true;
		}
	}
}

/**
 * for every row of the grid, get the second element (2nd column) of the row, that is a box and from
 * this get its content.
 */
buttonOkListener = function(){
	var gridrows = document.getElementById("gridrows");
	var rows = gridrows.childNodes;
	var map = [];
	for (var i=0; i<rows.length; i++){
		var row = rows[i];
		var userPrompt = row.childNodes[0].value;//1st child is the label (userPrompt)
		//check through the 3d child of the row if the field is optional
		var optionalField = row.childNodes[2];
		if (optionalField.tagName == "checkbox" && optionalField.checked){//the field is optional and checked
			var inputValue = row.childNodes[1].childNodes[0].value;
			if (inputValue == ""){
				art_semanticturkey.Alert.alert("Field '" + userPrompt + "' cannot be empty, please provide a value");
				return;
			}
			map.push({key: userPrompt, value: inputValue});
		} else if (optionalField.tagName == "label"){ //the field is mandatory
			var inputValue = row.childNodes[1].childNodes[0].value;
			if (inputValue == ""){
				art_semanticturkey.Alert.alert("Field '" + userPrompt + "' cannot be empty, please provide a value");
				return;
			}
			map.push({key: userPrompt, value: inputValue});
		}
	}
	for (var i=0; i<map.length; i++){
		art_semanticturkey.Logger.debug("key: " + map[i].key + ", value: " + map[i].value);
	}
	var crEntryId = window.arguments[0].crEntryXml.getAttribute("id");
	try {
		art_semanticturkey.STRequests.CustomRanges.runCoda(crEntryId, map);
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
	window.close();
}