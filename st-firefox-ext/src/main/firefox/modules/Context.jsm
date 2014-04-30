Components.utils.import("resource://stmodules/Logger.jsm");

EXPORTED_SYMBOLS = [ "Context"];

function Context() {
	//var project;
	//var wGraph;
	//var rGraph;
}

function getContextValuesForHTTPGetAsArray(){
	var contexAsArray = new Array();
	var i=0;
	
	//first of all take the context, wgraph e rgraph. If they are not present, 
	// try using these values from the default context
	// (it does not check if this is the default context or not)
	
	var project = this.getProject();
	if(typeof project == 'undefined' || project == "")
		project = Context.getProject();
	if(typeof project != 'undefined' && project != "")
		contexAsArray[i++] ="ctx_project="+project;
	
	var wGraph = this.getWGpragh();
	if(typeof wGraph == 'undefined' || wGraph == "")
		wGraph = Context.getWGpragh();
	if(typeof wGraph != 'undefined' && wGraph != "")
		contexAsArray[i++] ="ctx_wgraph="+wGraph;
	
	var rGraphs = this.getRGraphs();
	var rGraphsAsString="";
	if(typeof rGraphs == 'undefined' || rGraphs.length == 0)
		rGraphs = Context.getRGraphs();
	if(typeof rGraphs != 'undefined' && rGraphs.length != 0){
		for (var i=0; i<rGraphs.length ; ++i) {
			rGraphsAsString+=rGraphs[i]
		}
		contexAsArray[i++] ="ctx_rgraph="+rGraphsAsString;
	}
	
	//now use all the other values from the associative array
	for (var name in this.valuesArray) {
		contexAsArray[i++] ="ctx_"+name+"="+this.valuesArray[name];
	}
	return contexAsArray;
}

function getContextValuesAsString(separator){
	//use getContextValuesForHTTPGetAsArray, take all the elements of the returned array and put 
	// them in a string
	if(typeof separator  == 'undefined' || separator == "")
		separator = "&";
	var contexString = "";
	var contexAsArray = this.getContextValuesForHTTPGetAsArray();
	for (var i=0; i<contexAsArray.length ; ++i) {
		if(i != 0)
			contexString +=separator;
		contexString +=contexAsArray[i];
	}
	return contexString;
	
	/*var i=0;
	for (var name in this.valuesArray) {
		contexString +=name+"="+this.valuesArray[name]+";";
	}
	return contexString;*/
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

function getProject(){
	return this.project;
}

function setProject(projectName){
	this.project = projectName;
}

function getWGpragh(){
	return this.wGraph;
}

function setWGraph(wGraphName){
	this.wGraph = wGraphName;
}

function getRGraphs(){
	return this.rGraphs;
}

function setRGraphs(rGraphsName){
	this.rGraphs = rGraphsName;
}

//TODO test it
function clearValues(){
	this.project = "";
	this.wGraph = "";
	for (var name in this.valuesArray) {
		delete this.valuesArray[name];
	}
	for (var name in this.rGraphs) {
		delete this.rGraphs[name];
	}
}

Context.prototype.getContextValuesForHTTPGetAsArray = getContextValuesForHTTPGetAsArray;
Context.prototype.getContextValuesAsString = getContextValuesAsString;
Context.prototype.createNewArrayForContext = createNewArrayForContext;
Context.prototype.addValue = addValue;
Context.prototype.removeValue = removeValue;
Context.prototype.getValue = getValue;
Context.prototype.clearValues = clearValues;
Context.prototype.getProject = getProject;
Context.prototype.setProject = setProject;
Context.prototype.getWGpragh = getWGpragh;
Context.prototype.setWGraph = setWGraph;
Context.prototype.getRGraphs = getRGraphs;
Context.prototype.setRGraphs = setRGraphs;

//the data inside the context
Context.prototype.valuesArray = new Array();
Context.prototype.project; //used to just remember that there is this variable
Context.prototype.wGraph;  //used to just remember that there is this variable
Context.prototype.rGraphs = new Array();  



Context.constructor = Context;


Context.__proto__ = Context.prototype;