Components.utils.import("resource://stmodules/Logger.jsm");

EXPORTED_SYMBOLS = [ "Context"];

const HTTP_PARAM_PREFIX = "ctx_";
const ARRAY_SEPARATOR = ",";

function Context() {
	//var project;
	//var wGraph;
	//var rGraph;
}

function getContextValuesForHTTPGetAsArray(){
	var contexAsArray = new Array();
	var pos=0;
	var separatorRGraphs=","; // to separate the elements of the array rGraphs 
	
	//first of all take the context, wgraph e rgraph. If they are not present, 
	// try using these values from the default context
	// (it does not check if this is the default context or not)
	
	var project = this.getProject();
	if(typeof project == 'undefined' || project == ""){
		project = Context.getProject();
	}
	if(typeof project != 'undefined' && project != ""){
		contexAsArray[pos++] = HTTP_PARAM_PREFIX + "project=" + project;
	}
	
	var wGraph = this.getWGpragh();
	if(typeof wGraph == 'undefined' || wGraph == ""){
		wGraph = Context.getWGpragh();
	}
	if(typeof wGraph != 'undefined' && wGraph != ""){
		contexAsArray[pos++] = HTTP_PARAM_PREFIX + "wgraph=" + wGraph;
	}
	
	var rGraphs = this.getRGraphs();
	var rGraphsAsString="";
	if(typeof rGraphs == 'undefined' || rGraphs.length == 0){
		rGraphs = Context.getRGraphs();
	}
	if(typeof rGraphs != 'undefined' && rGraphs.length != 0){
		for (var i=0; i<rGraphs.length ; ++i) {
			if(i!=0){
				rGraphsAsString+=separatorRGraphs;
			}
			rGraphsAsString+=rGraphs[i]
		}
		contexAsArray[pos++] = HTTP_PARAM_PREFIX + "rgraph=" + rGraphsAsString;
	}
	
	//now use all the other values from the associative array
	for (var name in this.valuesArray) {
		contexAsArray[pos++] = HTTP_PARAM_PREFIX + name + "=" + this.valuesArray[name];
	}
	return contexAsArray;
}

function getContextValuesAsString(separator){
	//use getContextValuesForHTTPGetAsArray, take all the elements of the returned array and put 
	// them in a string
	if(typeof separator  == 'undefined' || separator == ""){
		separator = "&";
	}
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

function copy(inputContext){
	if(typeof inputContext == 'undefined' || inputContext == null){
		return; // the input context is not defined, so just return
	}
	
	//iterate over the values stored in the inputContext
	
	//first of all take the context, wgraph e rgraph. If they are not present, 
	// try using these values from the default context
	// (it does not check if this is the default context or not)
	
	var project = inputContext.getProject();
	if(typeof project != 'undefined' && project != "") {
		this.setProject(project);
	}
	
	var wGraph = inputContext.getWGpragh();
	if(typeof wGraph != 'undefined' && wGraph != "") {
		this.setWGraph(wGraph);
	}

	var rGraphs = inputContext.getRGraphs();
	if(typeof rGraphs != 'undefined' && rGraphs.length != 0){
		this.setRGraphs(rGraphs);
	}
	
	//now copy all the other values from the inputContext
	for (var name in inputContext.valuesArray) {
		this.addValue(name, inputContext.valuesArray[name])
	}
}

function clone(){
	var clonedContext = new Context();
	clonedContext.createNewArrayForContext();
	
	//iterate over the values stored in this context
	
	//first of all take the context, wgraph e rgraph. If they are not present, 
	// try using these values from the default context
	// (it does not check if this is the default context or not)
	
	var project = this.getProject();
	if(typeof project != 'undefined' && project != "") {
		clonedContext.setProject(project);
	}
	
	var wGraph = this.getWGpragh();
	if(typeof wGraph != 'undefined' && wGraph != "") {
		clonedContext.setWGraph(wGraph);
	}

	var rGraphs = this.getRGraphs();
	if(typeof rGraphs != 'undefined' && rGraphs.length != 0){
		clonedContext.setRGraphs(rGraphs);
	}
	
	//now copy all the other values from this context
	for (var name in this.valuesArray) {
		clonedContext.addValue(name, this.valuesArray[name])
	}
	
	return clonedContext;
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

function setRGraphs(rGraphsArray){
	this.rGraphs = rGraphsArray;
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
Context.prototype.copy = copy;
Context.prototype.clone = clone;
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