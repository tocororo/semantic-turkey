Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/ARTResources.jsm");

Components.utils.import("resource://gre/modules/FileUtils.jsm");


EXPORTED_SYMBOLS = [ "STResUtils" ];

var STResUtils = new Object();


function getImageSrcOrNull(rdfRes, operation){
	var src = getImageSrc(rdfRes, operation);
	//check if the src refers to an existing file, or retunr null if it does not refer to an existing file
	var exist = checkExistenceOfPath(getAbsolutePathForImageFileName(src));
	
	if(exist){
		return getRelativePathForImageName(src);
	} else{
		return null;
	}
	
}

function getImageSrc(rdfRes, operation) {
	// check the information contained inside the rdfRes, to decide the right fileName
	var fileName="";

	if (rdfRes instanceof ARTURIResource || rdfRes instanceof ARTBNode) {
		var role = rdfRes.getRole();
		var explicit = rdfRes.explicit;
		if (typeof operation == "undefined") {
			if (role == "concept") {
				if (explicit == "false" || explicit == false)
					fileName = "skosConcept_imported.png";
				else
					fileName = "skosConcept20x20.png";
			} else if (role == "individual") {
				if (explicit == "false" || explicit == false)
					fileName = "individual_noexpl.png";
				else
					fileName = "individual20x20.png";
			} else if (role == "cls") {
				if (explicit == "false" || explicit == false)
					fileName = "class_imported.png";
				else
					fileName = "class20x20.png";
			} else if (role == "objectProperty") {
				if (explicit == "false" || explicit == false)
					fileName = "propObject_imported.png";
				else
					fileName = "propObject20x20.png";
			} else if (role == "datatypeProperty") {
				if (explicit == "false" || explicit == false)
					fileName = "propDatatype_imported.png";
				else
					fileName = "propDatatype20x20.png";				
			} else if (role == "annotationProperty") {
				if (explicit == "false" || explicit == false)
					fileName = "propAnnotation_imported.png";
				else
					fileName = "propAnnotation20x20.png";				
			} else if (role == "ontologyProperty") {
				if (explicit == "false" || explicit == false)
					fileName = "propOntology_imported.png";
				else
					fileName = "propOntology20x20.png";				
			} else if (role == "property") {
				if (explicit == "false" || explicit == false)
					fileName = "prop_imported.png";
				else
					fileName = "prop20x20.png";
			}
		} else if(operation == "remove"){
			if (role == "concept") {
				fileName = "skosConcept20x20.png";
			} else if (role == "conceptScheme") {
				fileName = "skosScheme20x20.png";
			} else if (role == "individual") {
				fileName = "individual20x20.png";
			} else if (role == "cls") {
				fileName = "class20x20.png";
			}
		} else if(operation == "add"){
			if (role == "concept") {
				fileName = "skosC_create.png";
			} else if (role == "individual") {
				fileName = "individual_add.png";
			} else if (role == "cls") {
				fileName = "class_create.png";
			} else if (role == "property") {
				fileName = "prop_create.png";
			} else if (role == "conceptScheme") {
				fileName = "skosScheme_create.png"
			}
		}
	} else if (rdfRes instanceof ARTLiteral) {
		var lang = rdfRes.getLang();
		if(typeof lang != "undefined" && lang != null && lang != "" ){
			fileName = "flags/"+lang+".gif";
		}

	} 

	return fileName;
};


function getRelativePathForImageName(fileName){
	var path;
	
	path = "chrome://semantic-turkey/skin/images/"+fileName;
	
	return path;
}

function getAbsolutePathForImageFileName(fileName){
	var path;
	
	var file = FileUtils.getFile("ProfD", []);
	
	path = file.path +"\\extensions\\semanticturkey@art.uniroma2.it\\chrome\\semantic-turkey\\skin\\" +
			"classic\\images\\"+
	fileName.replace(new RegExp("/", 'g'), "\\");
	
	return path;
}


function checkExistenceOfPath(path){
	var exist = false;
	
	var nsifile = new FileUtils.File(path);
	exist = nsifile.exists();
	
	return exist;
}

STResUtils.getImageSrcOrNull = getImageSrcOrNull;
STResUtils.getImageSrc = getImageSrc;
STResUtils.getRelativePathForImageName = getRelativePathForImageName;
STResUtils.getAbsolutePathForImageFileName = getAbsolutePathForImageFileName;
STResUtils.checkExistenceOfPath = checkExistenceOfPath;
