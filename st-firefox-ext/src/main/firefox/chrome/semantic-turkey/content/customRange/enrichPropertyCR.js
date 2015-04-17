if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Property.jsm", art_semanticturkey);

/**
 * This function checks the range of the given predicate. If the predicate has only classic range,
 * launches the legacy enrichProperty, otherwise, if is available some CustomRange, let the user
 * choose what range to use.
 * @param subject
 * @param predicate
 */
enrichProperty = function(subject, predicate){
	var xmlResp = art_semanticturkey.STRequests.Property.getRange(predicate, "false");
	
	var customRangeXml = xmlResp.getElementsByTagName("customRanges")[0];
	var rangesXml = xmlResp.getElementsByTagName("ranges")[0];//classic ranges
	
	var parameters = new Object();
	//if the getRange response has both customRange and ranges section
	if (typeof customRangeXml != "undefined" && typeof rangesXml != "undefined"){
		parameters.xmlResp = xmlResp;
		parameters.selectedRange = null; //param for returned value
		window.openDialog("chrome://semantic-turkey/content/customRange/rangeSelectDialog.xul",
				"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
		//in case the dialog returned a value
		if (parameters.selectedRange != null){
			//check if the selected range is the classic range
			if (rangesXml.getAttribute("rngType") == parameters.selectedRange){
				classicRangesLaunch(rangesXml);
			} else { //or look in the custom ranges
				var crEntriesXml = customRangeXml.getElementsByTagName("crEntry");
				for (var i=0; i<crEntriesXml.length; i++){
					if (crEntriesXml[i].getAttribute("name") == parameters.selectedRange){
						customRangeLaunch(crEntriesXml[i]);
					}
				}
			}
		} else {
			return;
		}
	} else if (typeof customRangeXml != "undefined"){//if the getRange response has only customRange section
		var crEntriesXml = customRangeXml.getElementsByTagName("crEntry");
		if (crEntriesXml.length > 1){ //multiple crEntry
			parameters.xmlResp = xmlResp;
			parameters.selectedRange = null; //param for returned value
			window.openDialog("chrome://semantic-turkey/content/customRange/rangeSelectDialog.xul",
					"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
			//in case the dialog returned a value look for the custom range selected
			if (parameters.selectedRange != null){
				for (var i=0; i<crEntriesXml.length; i++){
					if (crEntriesXml[i].getAttribute("name") == parameters.selectedRange){
						customRangeLaunch(crEntriesXml[i]);
					}
				}
			} else {
				return;
			}
		} else { //single crEntry
			customRangeLaunch(crEntriesXml[0]);
		}
	} else if (typeof rangesXml != "undefined"){//if the getRange response has only ranges section
		classicRangesLaunch(rangesXml);
	}
}


customRangeLaunch = function(crEntryXml, subject, predicate){
	var parameters = {};
	parameters.crEntryXml = crEntryXml;
	parameters.subject = subject;
	parameters.predicate = predicate;
	window.openDialog("chrome://semantic-turkey/content/customRange/customForm.xul",
			"_blank", "chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
}

classicRangesLaunch = function(rangesXml, subject, predicate){
	var parameters = {};
	parameters.subject = subject;
	parameters.predicate = predicate;
	parameters.winTitle = "Add Property Value";
	parameters.action = "createAndAddPropValue";
	parameters.parentWindow = window;
	parameters.oncancel = false;
	
	if (rangesXml.getAttribute("rngType").indexOf("resource") != -1) {
		window.openDialog(
				"chrome://semantic-turkey/content/enrichProperty/enrichProperty.xul",
				"_blank", "modal=yes,resizable,centerscreen", parameters);

	} else if (rangesXml.getAttribute("rngType").indexOf("plainLiteral") != -1) {
		window.openDialog(
				"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
				"_blank", "modal=yes,resizable,centerscreen", parameters);
	} else if (rangesXml.getAttribute("rngType").indexOf("typedLiteral") != -1) {
		var rangeList = rangesXml.childNodes;
		for (var i = 0; i < rangeList.length; ++i) {
			if (typeof (rangeList[i].tagName) != 'undefined') {
				parameters.rangeType = rangeList[i].textContent;
			}
		}
		window.openDialog(
				"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
				"_blank", "modal=yes,resizable,centerscreen", parameters);
	} else if (rangesXml.getAttribute("rngType").indexOf("literal") != -1) {
		var rangeList = rangesXml.childNodes;
		var role = null;
		if (rangeList.length > 0) {
			for (var i = 0; i < rangeList.length; ++i) {
				if (typeof (rangeList[i].tagName) != 'undefined') {
					var dataRangeBNodeID = rangeList[i].textContent;
					var role = rangeList[i].getAttribute("role");
					var nodeType = rangeList[i].tagName;
				}
			}
			if (role.indexOf("dataRange") != -1) {
				var responseXML = art_semanticturkey.STRequests.Property.parseDataRange(
						dataRangeBNodeID, nodeType);

				var dataElement = responseXML.getElementsByTagName("data")[0];
				var dataRangesList = dataElement.childNodes;
				var dataRangesValueList = new Array();
				var k = 0;
				for (var i = 0; i < dataRangesList.length; ++i) {
					if (typeof (dataRangesList[i].tagName) != 'undefined') {
						var dataRangeValue = new Object();
						dataRangeValue.type = dataRangesList[i].tagName;
						dataRangeValue.rangeType = dataRangesList[i].getAttribute("type");
						dataRangeValue.show = dataRangesList[i].getAttribute("show");
						dataRangesValueList[k] = dataRangeValue;
						k++;
					}
				}
				parameters.rangeType = "dataRange";
				parameters.dataRangesValueList = dataRangesValueList;
				window.openDialog(
						"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
						"_blank", "modal=yes,resizable,centerscreen", parameters);
			}
		} else {
			var literalsParameters = new Object();
			literalsParameters.isLiteral = "literal";
			window.openDialog(
					"chrome://semantic-turkey/content/enrichProperty/isLiteral.xul",
					"_blank", "modal=yes,resizable,centerscreen", literalsParameters);
			if (literalsParameters.isLiteral == "plainLiteral") {
				window.openDialog(
						"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
						"_blank", "modal=yes,resizable,centerscreen", parameters);
			} else if (literalsParameters.isLiteral == "typedLiteral") {
				window.openDialog(
						"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
						"_blank", "modal=yes,resizable,centerscreen", parameters);
			}
		}
	} else if (rangesXml.getAttribute("rngType").indexOf("undetermined") != -1) {
		var literalsParameters = new Object();
		literalsParameters.isLiteral = "undetermined";
		window.openDialog(
				"chrome://semantic-turkey/content/enrichProperty/isLiteral.xul",
				"_blank", "modal=yes,resizable,centerscreen", literalsParameters);
		if (literalsParameters.isLiteral == "plainLiteral") {
			window.openDialog(
					"chrome://semantic-turkey/content/enrichProperty/enrichPlainLiteralRangedProperty.xul",
					"_blank", "modal=yes,resizable,centerscreen", parameters);
		} else if (literalsParameters.isLiteral == "typedLiteral") {
			var rangeList = rangesXml.childNodes;
			for (var i = 0; i < rangeList.length; ++i) {
				if (typeof (rangeList[i].tagName) != 'undefined') {
					parameters.rangeType = rangeList[i].textContent;
				}
			}
			window.openDialog(
					"chrome://semantic-turkey/content/enrichProperty/enrichTypedLiteralRangedProperty.xul",
					"_blank", "modal=yes,resizable,centerscreen", parameters);
		} else if (literalsParameters.isLiteral == "resource") {
			window.openDialog(
					"chrome://semantic-turkey/content/enrichProperty/enrichProperty.xul",
					"_blank", "modal=yes,resizable,centerscreen", parameters);
		}
	} else if (rangesXml.getAttribute("rngType").indexOf("inconsistent") != -1) {
		alert("Error range of " + propertyQName + " property is inconsistent");
	}
}