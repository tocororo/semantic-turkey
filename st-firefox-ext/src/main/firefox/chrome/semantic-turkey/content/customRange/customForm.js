if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stservices/SERVICE_XMLSchema.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_CustomRanges.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/stEvtMgr.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Sanitizer.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm");

/*
 * For a better understanding of this code, here is an explanation of how the UI is built.
 * For every formEntry in the getRange request, a row is added to the three-column grid.
 * Every row of the grid is composed as follow:
 * 	- label: Specified the value to insert. Its value represents the userPrompt feature written 
 * 	 	in the PEARL (if the CRE is "node" type, the label is simply "value")
 * 	- hbox: This box allows the user to input value and in turn its composed by some elements: 
 * 		- a textbox for the input (independent from the type of the formEntry (uri or literal)
 * 		- some other elements: they could be menulist for language input, widget for 
 * 		special datatype input (datepicker, timepicker,...), a button to show a convertion preview...
 * 	N.B. every input that matches a userPrompt is marked with a class "userPrompt" and an attribute
 * 	"userPrompt" that contains the userPrompt/ feature name. 
 * 	- label/checkbox: This 3rd element of the row is a label (*) if the formEntry is mandatory,
 * 		otherwise is a checkbox that allows to choose if fill or not the field.
 * 
 * For further information check the createXXXInput methods.
 */

var inputFields = [];

//function to handle change of lang in case of multiple language menulists
var eventHandler = {
		eventHappened : function(eventId, eventObject) {
			if (eventId == "languageMenuChanged") {
				var menus = document.getElementsByTagName("menulist");
				for (i=0; i<menus.length; i++) {
					var menu = menus[i];
					if (menu.getAttribute("userPrompt") == eventObject.userPrompt) {
						if (menu.selectedItem.value != eventObject.lang) {
							for (var j=0; j<menu.itemCount; j++) {
								if (menu.getItemAtIndex(j).value == eventObject.lang) {
									menu.selectedIndex = j;
									break;
								}
							}
						}
					}
				}
			} else if (eventId == "textboxInputChanged") {
				var textboxes = document.getElementsByTagName("textbox");
				for (i=0; i<textboxes.length; i++) {
					var textbox = textboxes[i];
					if (textbox.getAttribute("userPrompt") == eventObject.userPrompt) {
						if (textbox.value != eventObject.value) {
							textbox.value = eventObject.value;
						}
					}
				}
			}
		}
}

window.onload = function() {
	
	document.getElementById("okBtn").addEventListener("command", buttonOkListener, false);
	
	var crEntryId = window.arguments[0].crEntryId;
	var respXml = art_semanticturkey.STRequests.CustomRanges.getCustomRangeEntryForm(crEntryId);
	var formXml = respXml.getElementsByTagName("form")[0];

	//collect input fields
	var formEntriesXml = formXml.getElementsByTagName("formEntry");
	if (formEntriesXml.length > 0){ //there are formEntry
		for (var i=0; i<formEntriesXml.length; i++){
			inputFields.push(createInputField(formEntriesXml[i]));
		}
	} else { //there aren't formEntry, then there is exception in form tag
		/* if the pearl has some error, the form tag will contain the thrown exception in a "exception"
		 * attribute and there will be no "formEntry" tags. Here perform the check */
		art_semanticturkey.Alert.alert("Error in pearl code of the custom range with id '" +
				crEntryXml.getAttribute("id") + "'", formXml.getAttribute("exception"));
		window.close();
	}
	
	//build form UI with input fields
	var gridrows = document.getElementById("gridrows");
	for (var i=0; i<inputFields.length; i++) {
		var inputField = inputFields[i];
		if (!inputField.dependency) {
			var row = document.createElement("row");
			row.setAttribute("align", "center");
			//1st child: userPromptLabel
			var label = document.createElement("label");
			label.setAttribute("value", inputField.userPrompt);
			row.appendChild(label);
			//2nd child: input box
			row.appendChild(inputField.inputElement); //TODO attention
			//3nd child: mandatory control
			row.appendChild(createMandatoryField(inputField.mandatory));
			
			gridrows.appendChild(row);
		}
	}
	window.sizeToContent();
	
	art_semanticturkey.evtMgr.registerForEvent("languageMenuChanged", eventHandler);
	art_semanticturkey.evtMgr.registerForEvent("textboxInputChanged", eventHandler);
}

window.onunload = function() {
	art_semanticturkey.evtMgr.deregisterForEvent("languageMenuChanged", eventHandler);
	art_semanticturkey.evtMgr.deregisterForEvent("textboxInputChanged", eventHandler);
}

/**
 * Creates an input field for a form entry
 */
createInputField = function(formEntryXml) {
	/*
	 * inputField is a structure with the following properties
	 * 	- userPrompt: String representing the userPrompt related to the field (it will be also the label)
	 * 	- inputElement: hbox containing the element to allow the user to input a value
	 *	- mandatory: boolean that tells if the field is mandatory or not
	 *	- dependency: boolean that tells if an input field is a dependency for another one
	 */
	var inputField = {};
	inputField.userPrompt = formEntryXml.getAttribute("userPrompt");
	inputField.mandatory = formEntryXml.getAttribute("mandatory") == "true";
	inputField.dependency = false;
	inputField.inputElement = createInputElement(formEntryXml);
	return inputField;
}

/**
 * Creates the input element of a formEntry, namely the inputElement (a box) of a inputField object
 */
createInputElement = function(formEntryXml){
	var type = formEntryXml.getAttribute("type");
	if (type == "literal"){
		var datatype = formEntryXml.getAttribute("datatype");
		if (datatype != null){
			if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#date".toLowerCase()){
				return createDateInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#dateTime".toLowerCase()){
				return createDatetimeInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#time".toLowerCase()){
				return createTimeInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#float".toLowerCase()){
				return createFloatInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#integer".toLowerCase()){
				return createIntegerInput(formEntryXml);
			} else if (datatype.toLowerCase() == "http://www.w3.org/2001/XMLSchema#boolean".toLowerCase()){
				return createBooleanInput(formEntryXml);
			} else { //for every other case
				return createGenericLiteralInput(formEntryXml);
			}
		} else { //for plain literal
			return createGenericLiteralInput(formEntryXml);
		}
	} else if (type == "uri") {
		return createUriInput(formEntryXml);
	}
}

/**
 * Creates an input element for a generic literal: plain literal, plain literal with language tag and 
 * typed literal with a datatype for which a proper widget is not provided (ex. datapicker for xsd:date)
 * @param formEntryXml
 * @returns
 */
createGenericLiteralInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	mainBox.setAttribute("align", "center");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	textbox.addEventListener("input", textboxChangedListener, false);
	mainBox.appendChild(textbox);
	//eventual datatype
	var datatype = formEntryXml.getAttribute("datatype");
	if (datatype != null){
		textbox.setAttribute("tooltiptext", datatype);
	}
	//eventual lang
	var lang = formEntryXml.getAttribute("lang");
	if (lang != null){
		var langLabel = document.createElement("label");
		langLabel.setAttribute("value", "@"+lang);
		mainBox.appendChild(langLabel);
	}
	//special case converter: coda:langString should be enriched with a menu to select language
	var converterXml = formEntryXml.getElementsByTagName("converter")[0];
	if (converterXml != null){
		if (converterXml.getAttribute("uri") == "http://art.uniroma2.it/coda/contracts/langString") {
			var langUp = converterXml.getElementsByTagName("arg")[0].getAttribute("userPrompt");
			//create menulist to select language
			var langMenu = document.createElement("menulist");
			langMenu.setAttribute("class", "userPrompt");
			langMenu.setAttribute("userPrompt", langUp);
			var langMenupopup = document.createElement("menupopup");
			langMenu.appendChild(langMenupopup);
			var prefs = Components.classes["@mozilla.org/preferences-service;1"]
				.getService(Components.interfaces.nsIPrefBranch);
			var defaultLang = prefs.getCharPref("extensions.semturkey.annotprops.defaultlang");
			var langList = prefs.getCharPref("extensions.semturkey.annotprops.langs").split(",").sort();
			//populate the language menu
			for (var i=0; i<langList.length; i++) {
				var menuitem = document.createElement("menuitem");
				menuitem.setAttribute("value", langList[i]);
				menuitem.setAttribute("label", langList[i]);
				langMenupopup.appendChild(menuitem);
				if (langList[i] == defaultLang) {
					menuitem.setAttribute("selected", true);
				}
			}
			mainBox.appendChild(langMenu);
			
			//when change language fire event to change eventual menulist for the same userPrompt
			langMenu.addEventListener("select", function() {
				art_semanticturkey.evtMgr.fireEvent("languageMenuChanged",
						{userPrompt: langUp, lang: this.selectedItem.value});
			}, false);
			
			//set the inputField for the language (already created in inputFields) as dependency
			for (var i=0; i<inputFields.length; i++) {
				if (inputFields[i].userPrompt == langUp) {
					inputFields[i].dependency = true;
					break;
				}
			}
		}
	}
	return mainBox;
}

createUriInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	textbox.addEventListener("input", textboxChangedListener, false);
	art_semanticturkey.Sanitizer.makeAutosanitizing(textbox);
	mainBox.appendChild(textbox);
	//if there's a converter add a popup to see a conversion preview
	var converterXml = formEntryXml.getElementsByTagName("converter")[0];
	if (converterXml != null){
		var converter = converterXml.getAttribute("uri");
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
	return mainBox;
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
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	textbox.setAttribute("value", getDatetime());
	textbox.addEventListener("input", textboxChangedListener, false);
	mainBox.appendChild(textbox);
	var datepicker = document.createElement("datepicker");
	var timepicker = document.createElement("timepicker");
	datepicker.addEventListener("command", function(){
		textbox.setAttribute("value", getDatetime(datepicker, timepicker));
	}, false);
	timepicker.addEventListener("command", function(){
		textbox.setAttribute("value", getDatetime(datepicker, timepicker));
	}, false);
	mainBox.appendChild(datepicker);
	mainBox.appendChild(timepicker);
	return mainBox;
}

createDateInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	textbox.setAttribute("value", getDate());
	textbox.addEventListener("input", textboxChangedListener, false);
	mainBox.appendChild(textbox);
	var datepicker = document.createElement("datepicker");
	datepicker.addEventListener("change", function(){
		textbox.setAttribute("value", getDate(datepicker));
	}, false);
	mainBox.appendChild(datepicker);	
	return mainBox;
}

createTimeInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	textbox.setAttribute("value", getTime());
	textbox.addEventListener("input", textboxChangedListener, false);
	mainBox.appendChild(textbox);
	var timepicker = document.createElement("timepicker");
	timepicker.addEventListener("command", function(){
		textbox.setAttribute("value", getTime(timepicker));
	}, false);
	mainBox.appendChild(timepicker);
	return mainBox;
}

createFloatInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("type", "number");
	textbox.setAttribute("decimalplaces", "Infinity");
	textbox.setAttribute("hidespinbuttons", "true");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	textbox.addEventListener("input", textboxChangedListener, false);
	mainBox.appendChild(textbox);
	return mainBox;
}

createIntegerInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("type", "number");
	textbox.setAttribute("hidespinbuttons", "true");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	textbox.addEventListener("input", textboxChangedListener, false);
	mainBox.appendChild(textbox);
	return mainBox;
}

createBooleanInput = function(formEntryXml){
	var mainBox = document.createElement("hbox");
	var textbox = document.createElement("textbox");
	textbox.setAttribute("class", "userPrompt");
	textbox.setAttribute("userPrompt", formEntryXml.getAttribute("userPrompt"));
	textbox.setAttribute("flex", "1");
	textbox.setAttribute("readonly", "true");
	textbox.setAttribute("height", "25");
	textbox.setAttribute("tooltiptext", formEntryXml.getAttribute("datatype"));
	textbox.setAttribute("value", "true");
	textbox.addEventListener("input", textboxChangedListener, false);
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
	return mainBox;
}

/**
 * Creates an element to indicate if field is mandatory or optional.
 * The Element could be a simple label (*) if the entry is mandatory,
 * or a checkbox if the entry is optional
 */
createMandatoryField = function(mandatory){
	if (mandatory){
		var mandLbl = document.createElement("label");
		mandLbl.setAttribute("value", "*");
		return mandLbl;
	} else {
		var check = document.createElement("checkbox");
		check.setAttribute("checked", "true");
		check.setAttribute("tooltiptext", "optional field, check to insert");
		check.addEventListener("command", optionalChecboxListener, false);
		return check;
	}
}

getDate = function(dp){
	var xmlResp;
	if (dp != undefined) {
		xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDate(dp.year, dp.month+1, dp.date);
	} else {
		var d = new Date();
		xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDate(d.getFullYear(), d.getMonth()+1, d.getDate());
	}
	return xmlResp.getElementsByTagName("date")[0].textContent;
}

getTime = function(tp){
	var xmlResp;
	if (tp != undefined) {
		xmlResp = art_semanticturkey.STRequests.XMLSchema.formatTime(tp.hour, tp.minute, tp.second);
	} else {
		var d = new Date();
		xmlResp = art_semanticturkey.STRequests.XMLSchema.formatTime(d.getHours(), d.getMinutes(), d.getSeconds());
	}
	return xmlResp.getElementsByTagName("time")[0].textContent;
}

getDatetime = function(dp, tp){
	var xmlResp;
	if (dp != undefined && tp != undefined) {
		xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDateTime(
				dp.year, dp.month+1, dp.date, tp.hour, tp.minute, tp.second);
	} else {
		var d = new Date();
		xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDateTime(
				d.getFullYear(), d.getMonth()+1, d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds());
	}
	return xmlResp.getElementsByTagName("dateTime")[0].textContent;
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

textboxChangedListener = function() {
	var eventObject = {
			userPrompt: this.getAttribute("userPrompt"),
			value: this.value
	}
	art_semanticturkey.evtMgr.fireEvent("textboxInputChanged", eventObject);
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
		//check through the 3d child of the row if the field is optional
		var optionalField = row.childNodes[2];
		// if the row is mandatory (optional field is a label "*") or if the row is optional and checked
		if ((optionalField.tagName == "label") || (optionalField.tagName == "checkbox" && optionalField.checked)){
			
			var userPromptElems = row.getElementsByClassName("userPrompt");
			
			for (var j=0; j<userPromptElems.length; j++) {
				var userPrompt = userPromptElems[j].getAttribute("userPrompt");
				var inputValue;
				if (userPromptElems[j].tagName == "textbox") {
					inputValue = userPromptElems[j].value;
					if (inputValue == ""){//if the field is not filled
						art_semanticturkey.Alert.alert("Field '" + userPrompt + "' cannot be empty, please provide a value");
						return;
					}
				} else if (userPromptElems[j].tagName == "menulist") {
					inputValue = userPromptElems[j].selectedItem.value;
				}
				//add the key-value pair in map only if is not already present
				var found = false;
				for (var k=0; k<map.length; k++) {
					if (map[k].key == userPrompt) {
						found = true;
						break;
					}
				}
				if (!found) {
					map.push({key: userPrompt, value: inputValue});
				}
			}
		}
	}
	for (var i=0; i<map.length; i++){
		Logger.debug("key: " + map[i].key + ", value: " + map[i].value);
	}
	try {
		var subject = window.arguments[0].subject;
		var predicate = window.arguments[0].predicate;
		var crEntryId = window.arguments[0].crEntryId;
		art_semanticturkey.STRequests.CustomRanges.runCoda(subject, predicate, crEntryId, map);
	} catch (e){
		art_semanticturkey.Alert.alert(e);
	}
	window.arguments[0].completed = true;
	window.close();
}
