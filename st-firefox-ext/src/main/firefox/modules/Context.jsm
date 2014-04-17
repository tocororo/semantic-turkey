Components.utils.import("resource://stmodules/Logger.jsm");

EXPORTED_SYMBOLS = [ "Context"];

function Context() {}

function getContextValuesForHTTPGetAsArray(){
	var contexAsArray = new Array();
	var i=0;
	for (var name in this.valuesArray) {
		contexAsArray[i++] =name+"="+this.valuesArray[name];
	}
	return contexAsArray;
}

function getContextValuesAsString(){
	var contexString = "";
	var i=0;
	for (var name in this.valuesArray) {
		contexString +=name+"="+this.valuesArray[name]+";";
	}
	return contexString;
}


function createNewArrayForContext(){
	this.valuesArray = new Array();
}

function addValue(name, value){
	this.valuesArray[name] = value;
}

function removeValue(name){
	delete this.valuesArray[name];
}

function getValue(name){
	return this.valuesArray[name];
}

//TODO test it
function clearValues(){
	for (var name in this.valuesArray) {
		delete this.valuesArray[name];
	}
}

Context.prototype.getContextValuesForHTTPGetAsArray = getContextValuesForHTTPGetAsArray;
Context.prototype.getContextValuesAsString = getContextValuesAsString;
Context.prototype.createNewArrayForContext = createNewArrayForContext;
Context.prototype.addValue = addValue;
Context.prototype.removeValue = removeValue;
Context.prototype.getValue = getValue;
Context.prototype.clearValues = clearValues;

Context.prototype.valuesArray = new Array();

Context.constructor = Context;

Context.__proto__ = Context.prototype;