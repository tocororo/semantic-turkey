if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stservices/SERVICE_XMLSchema.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);

/*
 * For a better understanding of this code, here is an explanation of how the UI is built.
 * For every formEntry in the getRange request, a row is added to the three-column grid.
 * Every row of the grid is composed as follow:
 * 	- label: Specified the value to insert. Its value represents the userPrompt feature written 
 * 	 	in the PEARL (if the CRE is "node" type, the label is simply "value")
 * 	- hbox: This box allows the user to input value and in turn its composed by some elements: 
 * 		- Textbox: Independent from the type of the formEntry (uri or literal) the first child of 
 * 			the box is a textbox that allows to input some value or that shows some validated
 * 			value (converted or generated through some picker).
 * 		- InputElements: This elements vary based on the type and the datatype of the formEntry,
 * 			but basically they are elements that allows different type of input. For example, 
 * 			if the entry is type=uri there will be a button that allows to input a value and convert it;
 * 			if type=literal and datatype=xsd:date there will be a datepicker and so on.
 * 	- label/checkbox: This 3rd element of the row is a label (*) if the formEntry is mandatory,
 * 		otherwise is a checkbox that allows to choose if fill or not the field.
 * 
 * For further information check the createXXXInput methods.
 */

window.onload = function() {
	
	document.getElementById("okBtn").addEventListener("command", buttonOkListener, false);
	
	var crEntryXml = window.arguments[0].crEntryXml;
	var type = crEntryXml.getAttribute("type");
	
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
		} else { //for plain literal
			createGenericInput(formEntryXml);
		}
	} else if (type == "uri") {
		createUriInput(formEntryXml);
//		createGenericInput(formEntryXml);
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
	mainBox.setAttribute("align", "center");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	mainBox.appendChild(textbox);
	var datatype = formEntryXml.getAttribute("datatype");
	var lang = formEntryXml.getAttribute("lang");
	if (datatype != null){
		textbox.setAttribute("tooltiptext", datatype);
	}
	if (lang != null){
		var langLabel = document.createElement("label");
		langLabel.setAttribute("value", "@"+lang);
		mainBox.appendChild(langLabel);
	}
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
}

createUriInput = function(formEntryXml){
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
	mainBox.appendChild(textbox);
	
	var converter = formEntryXml.getAttribute("converter");
	if (converter != null){
		var testConvertBtn = document.createElement("toolbarbutton");
		testConvertBtn.setAttribute("image", "chrome://semantic-turkey/skin/images/tick.png");
		testConvertBtn.setAttribute("type", "menu");
		testConvertBtn.setAttribute("tooltiptext", "Conversion preview (generated URI)");
		testConvertBtn.setAttribute("disabled", "true");
		textbox.addEventListener("input", function() {
			if (this.value == "") testConvertBtn.setAttribute("disabled", "true");
			else testConvertBtn.setAttribute("disabled", "false");
		}, false);
		var menupopup = document.createElement("menupopup");
		menupopup.setAttribute("converter", converter);
		var menuTextbox = document.createElement("textbox");
		menuTextbox.setAttribute("readonly", "true");
		menuTextbox.setAttribute("width", "250");
		menupopup.appendChild(menuTextbox);
		//listener: when showing the popup, convert the text and show a preview of the conversion
		menupopup.addEventListener("popupshowing", function(){
			var converter = this.getAttribute("converter");
			menuTextbox.value = convert(converter, textbox.value);
		}, false);
		testConvertBtn.appendChild(menupopup);
		mainBox.appendChild(testConvertBtn);
	}
	row.appendChild(mainBox);
	//3nd child: mandatory control
	addMandatoryField(row, formEntryXml);
	
	gridrows.appendChild(row);
}

convert = function(converter, value){
	try{
		var xmlResp = art_semanticturkey.STRequests.CustomRanges.executeURIConverter(converter, value);
		return xmlResp.getElementsByTagName("value")[0].textContent;
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
	}
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
		// if the row is mandatory (optional field is a label "*") or if the row is optional and checked
		if ((optionalField.tagName == "label") || (optionalField.tagName == "checkbox" && optionalField.checked)){
			var inputTextbox = row.childNodes[1].childNodes[0];
			var inputValue = inputTextbox.value;
			if (inputValue == ""){//if the field is not filled
				art_semanticturkey.Alert.alert("Field '" + userPrompt + "' cannot be empty, please provide a value");
				return;
			}
			map.push({key: userPrompt, value: inputValue});
		}
	}
	for (var i=0; i<map.length; i++){
		art_semanticturkey.Logger.debug("key: " + map[i].key + ", value: " + map[i].value);
	}
	try {
		var subject = window.arguments[0].subject;
		var predicate = window.arguments[0].predicate;
		var crEntryId = window.arguments[0].crEntryXml.getAttribute("id");
		art_semanticturkey.STRequests.CustomRanges.runCoda(subject, predicate, crEntryId, map);
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
	window.close();
}