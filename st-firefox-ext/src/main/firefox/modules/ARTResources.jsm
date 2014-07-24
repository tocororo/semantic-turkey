
EXPORTED_SYMBOLS = ["ARTNode", "ARTLiteral", "ARTURIResource", "ARTBNode"];

/********** ARTNode *******************/

ARTNode = function(){
		
	this.isLiteral = function(){
		return false;
	};
	
	this.isResource = function(){
		return false;
	};
	
	this.isURIResource = function(){
		return false;
	};
	
	this.isBNode = function(){
		return false;
	};
	
	this.getNominalValue = function() {
		return "";
	};
	
	this.toNT = function() {
		return "";
	};
};

/********** ARTLiteral *******************/

ARTLiteral = function(label, datatype, lang, isTypedLiteral){
	var label = label;
	var datatype = datatype;
	var lang = lang;
	var isTypedLiteral = isTypedLiteral;
	
	this.getLabel = function(){
		return label;	
	};
	
	this.getDatatype = function(){
		return datatype;	
	};
	
	this.getLang = function(){
		return lang;	
	};
	
	this.isLiteral = function(){
		return true;
	};
	
	this.isTypedLiteral = function(){
		return isTypedLiteral;
	};
	
	this.getNominalValue = function() {
		return label;
	};
	
	this.toNT = function() {
		var nt = label.quote();
		
		if (lang != null) {
			nt += "@" + lang;
		} else if (datatype != null) {
			nt += "^^" + datatype; // TODO: check!!!
		}
		
		return nt;
	};

};

ARTLiteral.prototype = new ARTNode();
ARTLiteral.constructor = ARTLiteral;

/********** ARTURIResource *******************/

ARTURIResource = function(show, role, uri){
	var show = show;
	var role = role;
	var uri = uri;
	
	this.isResource = function(){
		return true;
	};
	
	this.isURIResource = function(){
		return true;
	};
	
	this.getShow = function(){
		return show;	
	};
	
	this.getRole = function(){
		return role;	
	};
	
	this.getURI = function(){
		return uri;	
	};
	
	this.getNominalValue = function() {
		return uri;
	};
	
	this.toNT = function() {
		return "<" + uri + ">";
	};
};

ARTURIResource.prototype = new ARTNode();
ARTURIResource.constructor = ARTURIResource;


/********** ARTBNode *******************/

ARTBNode = function(id){
	var id = id;
	
	this.isResource = function(){
		return true;
	};
	
	this.isBNode = function(){
		return true;
	};
	
	this.getId = function(){
		return id;	
	};
	
	this.getNominalValue = function() {
		return "_:" + id;
	};
	
	this.toNT = function() {
		return this.getNominalValue();
	};

};

ARTBNode.prototype = new ARTNode();
ARTBNode.constructor = ARTBNode;
