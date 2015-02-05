EXPORTED_SYMBOLS = ["sanitizeInput"];
	
sanitizeInput = function(textbox){
	textbox.addEventListener("keypress", onKeypressListener, false);
	textbox.addEventListener("paste", onPasteListener, false);
}		

onPasteListener = function(event){
	var txt = this;
	var txtContent = txt.value;
	var start = txt.selectionStart;
	var end = txt.selectionEnd;
	
	var textToPaste = event.clipboardData.getData("text/plain");
	var transformedText = textToPaste.replace(/ /g, "_");
	
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
	var transformedChar;
	var changed = false;
	if (charPressed == " ") {
		transformedChar = "_";
		changed = true;
	}
	if (changed) {
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