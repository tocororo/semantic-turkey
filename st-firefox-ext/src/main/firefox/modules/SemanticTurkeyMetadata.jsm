
let EXPORTED_SYMBOLS = ["SemanticTurkeyMetadata"];

var SemanticTurkeyMetadata = {};

SemanticTurkeyMetadata.getClientVersion = function(){
	
	// this code is not completely robust, it assumes that the version is written at least in an "standard" way
	// that is any of: <major> or <major>.<minor> or <major>.<minor>.<revision>
	
	var rawVersion = SemanticTurkeyMetadata.getRawClientVersion();
	
	var strippedRawVersion = stripQualifier(rawVersion);
	
	var res = strippedRawVersion.split(".");
	
    if (res.length==1) {
      return strippedRawVersion + ".0.0";
    }
    
    else if (res.length==2) {
      return strippedRawVersion + ".0";
    }
    
    return strippedRawVersion;
};

SemanticTurkeyMetadata.getRawClientVersion = function(){
	return "${project.version}";
};


function stripQualifier(version){
	var pos = version.indexOf("-");
	if(pos == -1)
		return version;
	return version.substring(0, pos);
	var temp = version
}