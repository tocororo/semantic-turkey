
let EXPORTED_SYMBOLS = ["SemanticTurkeyMetadata"];

var SemanticTurkeyMetadata = {};

SemanticTurkeyMetadata.getClientVersion = function(){
	return "${project.version}";
}