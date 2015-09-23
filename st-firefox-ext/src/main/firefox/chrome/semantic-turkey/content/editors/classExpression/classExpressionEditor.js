if (typeof art_semanticturkey == "undefined") var art_semanticturkey = {};
if (typeof art_semanticturkey.classExpressionEditor == "undefined") art_semanticturkey.classExpressionEditor = {};

Components.utils.import("resource://stservices/SERVICE_Manchester.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Alert.jsm", art_semanticturkey);

art_semanticturkey.classExpressionEditor.init = function() {
	var args = (arguments && arguments[0]) || {};
	var defaultExpression = args.expression || "";
	
	var classExpressionTextbox = document.getElementById("classExpressionTextbox");
	
	classExpressionTextbox.setAttribute("value", defaultExpression);
	classExpressionTextbox.addEventListener("input", art_semanticturkey.classExpressionEditor.inputHandler);
	
	document.addEventListener("dialogaccept", art_semanticturkey.classExpressionEditor.dialogAcceptHandlder, true);
};

art_semanticturkey.classExpressionEditor.dialogAcceptHandlder = function(event) {
	var expression = document.getElementById("classExpressionTextbox").value;
	
	try {
		var replyResponse = art_semanticturkey.STRequests.Manchester.checkExpression(expression);
		if (replyResponse.isFail()) {
			throw new Error(replyResponse.getMsg());
		}		
	} catch (e) {
		art_semanticturkey.Alert.alert(e);
		event.preventDefault();
		return;
	}

	if (window.arguments && window.arguments[0]) {
		window.arguments[0].expression = expression;
	}
};

art_semanticturkey.classExpressionEditor.inputHandler = function(event) {
	var defaultExpression = event.target.getAttribute("value");
	var currentExpression = event.target.value;
	
	if (currentExpression == defaultExpression) {
		document.documentElement.setAttribute("buttondisabledaccept", "true");
	} else {
		document.documentElement.setAttribute("buttondisabledaccept", "false");		
	}
};

window.addEventListener("load", art_semanticturkey.classExpressionEditor.init, false);