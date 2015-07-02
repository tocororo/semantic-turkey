EXPORTED_SYMBOLS = ["Sanitizer"];

var sourceChar = " ";
var targetChar = "_";

Sanitizer = {
		
	/**
	 * Make a textbox auto-sanitize its content
	 * @param textbox
	 */
	makeAutosanitizing : function(textbox){
		textbox.addEventListener("keypress", onKeypressListener, false);
		textbox.addEventListener("paste", onPasteListener, false);
	},

	/**
	 * Sanitize the input text replacing the white space with underscore. The optional parameter
	 * <code>options</code> allows to customize the process. Available options are: <code>collapseSpaces</code>,
	 * <code>trimText</code>, <code>encodeText</code>.
	 * @param text
	 * @param options
	 * @returns
	 */
	sanitize : function(text, options){
		var wrappedOptions = Object.create(options);
		
		var collapseSpaces = wrappedOptions.collapseSpaces || false;
		var trimText = wrappedOptions.trimText || false;
		var encodeText = wrappedOptions.encodeText || false;

		if (collapseSpaces) {
			text = text.replace(/\s+/gm, " ");
		}
		
		if (trimText) {
			text = text.trim();
		}
		
		var text = text.replace(new RegExp(sourceChar, 'g'), targetChar);//'g' = global match (find all matches, doesn't stop after 1st match)
		
		if (encodeText) {
			text = encodeURIComponent(text);
		}
		
		return text;
	}
}

onPasteListener = function(event){
	var txt = this;
	var txtContent = txt.value;
	var start = txt.selectionStart;
	var end = txt.selectionEnd;
	
	var textToPaste = event.clipboardData.getData("text/plain");
	var transformedText = Sanitizer.sanitize(textToPaste);
	
	// Set the new textbox content
	var contentBeforeSpace = txtContent.slice(0, start);
	var contentAfterSpace = txtContent.slice(end);
    txt.value = contentBeforeSpace + transformedText + contentAfterSpace;
	// Move the cursor
    txt.selectionStart = txt.selectionEnd = start + transformedText.length;
    
	event.preventDefault();//prevent the default keypress listener
},

onKeypressListener = function(event){
	var txt = this;
	var txtContent = txt.value;
	var charPressed = String.fromCharCode(event.which);
	
	if (charPressed == sourceChar){
		var transformedChar = Sanitizer.sanitize(charPressed);
		var start = txt.selectionStart;
		var end = txt.selectionEnd;
		// Set the new textbox content
		var contentBeforeSpace = txtContent.slice(0, start);
		var contentAfterSpace = txtContent.slice(end);
	    txt.value = contentBeforeSpace + transformedChar + contentAfterSpace;
	    // Move the cursor
	    txt.selectionStart = txt.selectionEnd = start + 1;
	    event.preventDefault();//prevent the default keypress listener
	}
}