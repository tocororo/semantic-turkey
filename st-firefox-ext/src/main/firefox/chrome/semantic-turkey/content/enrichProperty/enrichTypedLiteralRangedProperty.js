/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is SemanticTurkey.
 * 
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 * 
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART) Current
 * information about SemanticTurkey can be obtained at
 * http://semanticturkey.uniroma2.it
 * 
 */
/**
 * setPanel
 * 
 * @param
 */
 var langsPrefsEntry="extensions.semturkey.annotprops.langs";
 var defaultLangPref="extensions.semturkey.annotprops.defaultlang";
 
if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_XMLSchema.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

 window.onload = function() {
	document.getElementById("createProperty").addEventListener("click",art_semanticturkey.onAccept,true);
	document.getElementById("cancel").addEventListener("click",art_semanticturkey.onCancel,true);
	document.getElementById("newValue").addEventListener("command",
			art_semanticturkey.onAccept, true);
	document.getElementById("newValue").focus();	
	art_semanticturkey.setPanel();
};
 
art_semanticturkey.setPanel= function() {
	var boxrows = document.getElementById("boxrows");
	var defaultRangeType=window.arguments[0].rangeType;
	if (typeof defaultRangeType == 'undefined'){ //undefined range. Let the user decide it through a menu
		var rangeLbl = document.createElement("label");
		rangeLbl.setAttribute("id", "lblvalue");
		rangeLbl.setAttribute("value", "Insert Range Type:");
		var row1 = document.createElement("row");
		row1.appendChild(rangeLbl);
		var row2 = document.createElement("row");
		var rangeMenuList = document.createElement("menulist");
		rangeMenuList.setAttribute("id", "rangeMenu");
		var rangeMenupopup = document.createElement("menupopup");
		var rangeMenuitem = document.createElement("menuitem");
		var rangList = new art_semanticturkey.dataRangeList();
		var listLength = rangList.getLength();
		for(var i=0; i<listLength; ++i){
			rangeMenuitem.setAttribute('label', rangList.getElement(i));
			rangeMenuitem.setAttribute('id', rangList.getElement(i));
			rangeMenupopup.appendChild(rangeMenuitem);
			rangeMenuitem = document.createElement("menuitem");
		}
		rangeMenuList.appendChild(rangeMenupopup);
		rangeMenuList.addEventListener("command", rangeMenuListener, false);
		row2.appendChild(rangeMenuList);
		boxrows.appendChild(row1);
		boxrows.appendChild(row2);
	}else if(defaultRangeType == "dataRange"){ //[Tiziano] when this block is executed? is remnant of old code? can be removed?
		document.getElementById("valueLabel").setAttribute("hidden",true);
		document.getElementById("valueRow").setAttribute("hidden",true);
		var rangeLbl = document.createElement("label");
		rangeLbl.setAttribute("id", "lblvalue");
		rangeLbl.setAttribute("value", "Insert Range Value:");
		var row1 = document.createElement("row");
		row1.appendChild(rangeLbl);
		var dataRangesValueList = window.arguments[0].dataRangesValueList;
		var row2 = document.createElement("row");
		var rangeMenuList = document.createElement("menulist");
		rangeMenuList.setAttribute("id", "rangeMenu");
		var rangeMenupopup = document.createElement("menupopup");
		var rangeMenuitem = document.createElement("menuitem");
		var listLength = dataRangesValueList.length;
		for(var i=0; i<listLength; ++i){
			rangeMenuitem.setAttribute('label', (dataRangesValueList[i]).show);
			rangeMenuitem.setAttribute('id',(dataRangesValueList[i]).show);
			rangeMenuitem.setAttribute('rangeType',(dataRangesValueList[i]).rangeType);
			rangeMenuitem.setAttribute('type',(dataRangesValueList[i]).type);
			rangeMenupopup.appendChild(rangeMenuitem);
			rangeMenuitem = document.createElement("menuitem");
		}
		rangeMenuList.appendChild(rangeMenupopup);
		row2.appendChild(rangeMenuList);
		boxrows.appendChild(row1);
		boxrows.appendChild(row2);
	} else { //Typed literal with range defined, check some particular cases:
		if (defaultRangeType == "http://www.w3.org/2001/XMLSchema#dateTime") {
			setDatetimeInputInterface();
		} else if (defaultRangeType == "http://www.w3.org/2001/XMLSchema#date"){
			setDateInputInterface();
		} else if (defaultRangeType == "http://www.w3.org/2001/XMLSchema#time"){
			setTimeInputInterface();
		} else if (defaultRangeType == "http://www.w3.org/2001/XMLSchema#duration"){
			setDurationInputInterface();
		} else if (defaultRangeType == "http://www.w3.org/2001/XMLSchema#integer"){
			setIntegerInputInterface();
		} else if (defaultRangeType == "http://www.w3.org/2001/XMLSchema#float"){
			setFloatInputInterface();
		}
	}
	
	if (window.arguments[0].property == "rdfs:comment") {
		var propValue = document.getElementById("newValue");
		propValue.setAttribute("multiline", "true");
		propValue.setAttribute("wrap", "on");
		propValue.setAttribute("cols", "1");
		propValue.setAttribute("rows", "3");
	}	
};

art_semanticturkey.onAccept= function() {
	var range = window.arguments[0].rangeType;
	var type = "typedLiteral";
	if(range == "dataRange"){
		var rangeMenuList=document.getElementById("rangeMenu");
		var rangeMenuitem =rangeMenuList.selectedItem;
		range = rangeMenuitem.getAttribute('rangeType');
		type =rangeMenuitem.getAttribute('type');
		var propValue = rangeMenuitem.getAttribute('id');
	}else if (typeof range == 'undefined'){
		var rangeMenuList=document.getElementById("rangeMenu");
		var rangeMenuitem =rangeMenuList.selectedItem;
		range = rangeMenuitem.getAttribute('id');
		var propValue = document.getElementById("newValue").value;
	}else{
		var propValue = document.getElementById("newValue").value;
	}
	try{
			art_semanticturkey.STRequests.Property.createAndAddPropValue(
					window.arguments[0].subject,
					window.arguments[0].predicate,
					propValue,
					range,
					type
			);
		close();
	}
	catch (e) {
		alert(e.name + ": " + e.message);
	}
};

art_semanticturkey.onCancel= function() {
	window.arguments[0].oncancel = true;
	window.close();
};

/**
 * Listener to the menulist available only if the typed literal has no range.
 */
var rangeMenuListener = function(){
	restoreStaticInterface();
	var boxrows = document.getElementById("boxrows");
	var selectedItem = this.selectedItem.label;
	if (selectedItem == "http://www.w3.org/2001/XMLSchema#date"){		
		setDateInputInterface();
	} else if (selectedItem == "http://www.w3.org/2001/XMLSchema#dateTime"){
		setDatetimeInputInterface();
	} else if (selectedItem == "http://www.w3.org/2001/XMLSchema#time"){
		setTimeInputInterface();
	} else if (selectedItem == "http://www.w3.org/2001/XMLSchema#duration"){
		setDurationInputInterface();
	} else if (selectedItem == "http://www.w3.org/2001/XMLSchema#float"){
		setFloatInputInterface();
	} else if (selectedItem == "http://www.w3.org/2001/XMLSchema#integer"){
		setIntegerInputInterface();
	}
}

/**
 * If the range of a typed literal is determined dynamically (through the menu), the dialog should
 * have a basic interface composed by the input textbox and the menulist.
 * When a range is chosen, the UI could change and a further box is added.
 * This method restores the initial interface.
 */
var restoreStaticInterface = function(){
	var inputBox = document.getElementById("inputBox");
	if (inputBox != null)
		inputBox.remove();
	var newValueTxt = document.getElementById("newValue");
	newValueTxt.removeAttribute("type");
	newValueTxt.readOnly = false;
	newValueTxt.value = "";
}

/**
 * Change the input textbox to allow only integer input
 */
var setIntegerInputInterface = function() {
	var newValueTxt = document.getElementById("newValue");
	newValueTxt.setAttribute("type", "number");
	newValueTxt.setAttribute("hidespinbuttons", "true");
}

/**
 * Change the input textbox to allow only float input
 */
var setFloatInputInterface = function() {
	var newValueTxt = document.getElementById("newValue");
	newValueTxt.setAttribute("type", "number");
	newValueTxt.setAttribute("decimalplaces", "Infinity");
	newValueTxt.setAttribute("hidespinbuttons", "true");
}

/**
 * Add dynamically a row containing a datepicker and a timepicker (used in case of datetime range)
 */
var setDatetimeInputInterface = function() {
	var mainBox = document.getElementById("mainBox");
	var inputBox = document.createElement("hbox");
	inputBox.setAttribute("id", "inputBox");
	var label = document.createElement("label");
	label.setAttribute("value", "Datetime:");
	var groupbox = document.createElement("groupbox");
	groupbox.setAttribute("align", "center");
	groupbox.setAttribute("orient", "horizontal");
	var datepicker = document.createElement("datepicker");
	datepicker.setAttribute("type", "popup");
	datepicker.setAttribute("id", "datepicker");
	var timepicker = document.createElement("timepicker");
	timepicker.setAttribute("id", "timepicker");
	groupbox.appendChild(label);
	groupbox.appendChild(datepicker);
	groupbox.appendChild(timepicker);
	inputBox.appendChild(groupbox);
	mainBox.appendChild(inputBox);
	datepicker.addEventListener("change", datetimePickerListener, false);
	timepicker.addEventListener("change", datetimePickerListener, false);
	//disabled the input textbox (datetime editable only by the pickers) and set the value
	var newValueTxt = document.getElementById("newValue");
	newValueTxt.readOnly = true;
	newValueTxt.value = getDatetime();
}

/**
 * Add dynamically a row containing a datepicker (used in case of date range)
 */
var setDateInputInterface = function() {
	var mainBox = document.getElementById("mainBox");
	var inputBox = document.createElement("hbox");
	inputBox.setAttribute("id", "inputBox");
	var groupbox = document.createElement("groupbox");
	groupbox.setAttribute("orient", "horizontal");
	groupbox.setAttribute("align", "center");
	var label = document.createElement("label");
	label.setAttribute("value", "Date:");
	var datepicker = document.createElement("datepicker");
	datepicker.setAttribute("type", "popup");
	datepicker.setAttribute("id", "datepicker");
	groupbox.appendChild(label);
	groupbox.appendChild(datepicker);
	inputBox.appendChild(groupbox);
	mainBox.appendChild(inputBox);
	datepicker.addEventListener("change", datePickerListener, false);
	//disabled the input textbox (date editable only by the picker) and set the value
	var newValueTxt = document.getElementById("newValue");
	newValueTxt.readOnly = true;
	newValueTxt.value = getDate();
}

/**
 * Add dynamically a row containing a timepicker (used in case of time range)
 */
var setTimeInputInterface = function() {
	var mainBox = document.getElementById("mainBox");
	var inputBox = document.createElement("hbox");
	inputBox.setAttribute("id", "inputBox");
	var groupbox = document.createElement("groupbox");
	groupbox.setAttribute("orient", "horizontal");
	groupbox.setAttribute("align", "center");
	var label = document.createElement("label");
	label.setAttribute("value", "Time:");
	var timepicker = document.createElement("timepicker");
	timepicker.setAttribute("id", "timepicker");
	groupbox.appendChild(label);
	groupbox.appendChild(timepicker);
	inputBox.appendChild(groupbox);
	mainBox.appendChild(inputBox);
	timepicker.addEventListener("change", timePickerListener, false);
	//disabled the input textbox (time editable only by the picker) and set the value
	var newValueTxt = document.getElementById("newValue"); 
	newValueTxt.readOnly = true;
	newValueTxt.value = getTime();
}

/**
 * Add dynamically a row containing input textbox for duration (used in case of duration range)
 */
var setDurationInputInterface = function() {
	var mainBox = document.getElementById("mainBox");
	var inputBox = document.createElement("hbox");
	inputBox.setAttribute("id", "inputBox");
	var groupbox = document.createElement("groupbox");
	groupbox.setAttribute("orient", "horizontal");
	groupbox.setAttribute("align", "center");
	var minusCb = document.createElement("checkbox");
	minusCb.setAttribute("id", "minusCb");
	minusCb.setAttribute("tooltiptext", "negative");
	minusCb.setAttribute("label", "-");
	var yearTxt = document.createElement("textbox");
	yearTxt.setAttribute("id", "yearTxt");
	yearTxt.setAttribute("type", "number");
	yearTxt.setAttribute("hidespinbuttons", "true");
	yearTxt.setAttribute("tooltiptext", "year");
	yearTxt.setAttribute("width", "25");
	var monthTxt = document.createElement("textbox");
	monthTxt.setAttribute("id", "monthTxt");
	monthTxt.setAttribute("type", "number");
	monthTxt.setAttribute("hidespinbuttons", "true");
	monthTxt.setAttribute("tooltiptext", "month");
	monthTxt.setAttribute("width", "25");
	var dayTxt = document.createElement("textbox");
	dayTxt.setAttribute("id", "dayTxt");
	dayTxt.setAttribute("type", "number");
	dayTxt.setAttribute("hidespinbuttons", "true");
	dayTxt.setAttribute("tooltiptext", "day");
	dayTxt.setAttribute("width", "25");
	var hourTxt = document.createElement("textbox");
	hourTxt.setAttribute("id", "hourTxt");
	hourTxt.setAttribute("type", "number");
	hourTxt.setAttribute("hidespinbuttons", "true");
	hourTxt.setAttribute("tooltiptext", "hour");
	hourTxt.setAttribute("width", "25");
	var minuteTxt = document.createElement("textbox");
	minuteTxt.setAttribute("id", "minuteTxt");
	minuteTxt.setAttribute("type", "number");
	minuteTxt.setAttribute("hidespinbuttons", "true");
	minuteTxt.setAttribute("tooltiptext", "minute");
	minuteTxt.setAttribute("width", "25");
	var secondTxt = document.createElement("textbox");
	secondTxt.setAttribute("id", "secondTxt");
	secondTxt.setAttribute("type", "number");
	secondTxt.setAttribute("hidespinbuttons", "true");
	secondTxt.setAttribute("tooltiptext", "second");
	secondTxt.setAttribute("width", "25");
	var okBtn = document.createElement("button");
	okBtn.setAttribute("label", "OK");
	okBtn.addEventListener("command", durationFormatListener, false);
	groupbox.appendChild(minusCb);
	groupbox.appendChild(yearTxt);
	groupbox.appendChild(monthTxt);
	groupbox.appendChild(dayTxt);
	groupbox.appendChild(hourTxt);
	groupbox.appendChild(minuteTxt);
	groupbox.appendChild(secondTxt);
	groupbox.appendChild(okBtn);
	inputBox.appendChild(groupbox);
	mainBox.appendChild(inputBox);
	//disabled the input textbox (duration editable only by the input textboxes) and set the value
	var newValueTxt = document.getElementById("newValue"); 
	newValueTxt.readOnly = true;
	newValueTxt.value = getDuration();
}

//listener for date time picker (only for typed literal with range datetime)
var datetimePickerListener = function(){
	document.getElementById("newValue").value = getDatetime();
}

//listener for date picker (only for typed literal with range date)
var datePickerListener = function(){
	document.getElementById("newValue").value = getDate();
}

//listener for date picker (only for typed literal with range time)
var timePickerListener = function(){
	document.getElementById("newValue").value = getTime();
}

//listener for date picker (only for typed literal with range time)
var durationFormatListener = function(){
	document.getElementById("newValue").value = getDuration();
}

/**
 * return the datetime in yyyy-MM-ddThh:mm:ss+offset format indicated by the datepicker and timepicker
 * (useful only for typed literal with range datetime)
 */ 
var getDatetime = function(){
	//format performed by client
//	return getDate() + "T" + getTime();
	/*Comment the following code block and decommend the previous to format client-side and exclude the offset*/
	//format performed by the server (it include offset to time).
	var dp = document.getElementById("datepicker");
	var tp = document.getElementById("timepicker");
	try {
		var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDateTime(
				dp.year, dp.month, dp.date, tp.hour, tp.minute, tp.second);
		var data = xmlResp.getElementsByTagName("data")[0];
		return data.getElementsByTagName("dateTime")[0].textContent;
	} catch (e){
		art_semanticturkey.Alert.alert(e);
		return;
	}
}

/**
 * return the date in yyyy-MM-dd format indicated by the datepicker
 * (useful only for typed literal with range datetime and date)
 */ 
var getDate = function(){
	//format performed by the client
//	var date = document.getElementById("datepicker").value;
//	//workaround to format date with 0 leading year (if year is 2 or 3 digits)
//	while (date.length < 10)
//		date = "0" + date;
//	return date;
	/*Comment the following code block and decommend the previous to format client-side*/
	//format performed by the server
	var dp = document.getElementById("datepicker");
	try {
		var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDate(dp.year, dp.month, dp.date);
		var data = xmlResp.getElementsByTagName("data")[0];
		return data.getElementsByTagName("date")[0].textContent;
	} catch (e){
		art_semanticturkey.Alert.alert(e);
		return;
	}
}

/**
 * return the time in hh:mm:ss format indicated by the timepicker
 * (useful only for typed literal with range datetime and time)
 */ 
var getTime = function(){
	//format performed by the client	
//	var time = document.getElementById("timepicker").value;
//	//workaround to format time with 0 leading hour (if hour is 1-digits)
//	if (time.length < 8)
//		time = "0" + time;
//	return time;
	/*Comment the following code block and decommend the previous to format client-side*/
	//format performed by the server
	var tp = document.getElementById("timepicker");
	try {
		var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatTime(tp.hour, tp.minute, tp.second);
		var data = xmlResp.getElementsByTagName("data")[0];
		return data.getElementsByTagName("time")[0].textContent;
	} catch (e){
		art_semanticturkey.Alert.alert(e);
		return;
	}
}

/**
 * Return the formatted duration based on the user input
 * (useful only for typed literal with range duration)
 */
var getDuration = function(){
	var isPositive = !(document.getElementById("minusCb").checked);
	var year = document.getElementById("yearTxt").value;
	var month = document.getElementById("monthTxt").value;
	var day = document.getElementById("dayTxt").value;
	var hour = document.getElementById("hourTxt").value;
	var minute = document.getElementById("minuteTxt").value;
	var second = document.getElementById("secondTxt").value;
	try {
		var xmlResp = art_semanticturkey.STRequests.XMLSchema.formatDuration(isPositive, year, month, day, hour, minute, second);
		var data = xmlResp.getElementsByTagName("data")[0];
		return data.getElementsByTagName("duration")[0].textContent;
	} catch (e){
		art_semanticturkey.Alert.alert(e);
		return;
	}
}
