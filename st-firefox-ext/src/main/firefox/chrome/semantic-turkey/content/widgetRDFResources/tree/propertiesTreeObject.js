
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);
//Components.utils.import("resource://stservices/SERVICE_Individual.jsm", art_semanticturkey);

if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

art_semanticturkey.propertiesTreeObject = function(boxForProeprtiesInput, rootPropertiesArrayInput, 
		createContextMenuInput, functionForRDFNodeDBLCLickOnPropertyInput){
	var boxForProperties = boxForClassesInput;
	var createContextMenu = createContextMenuInput;
	
	var rootPropertiesArray = rootPropertiesArrayInput;
	
	var dblclickOnPropertyFunction;
	if(typeof functionForRDFNodeDBLCLickOnPropertyInput != 'undefined')
		dblclickOnPropertyFunction = functionForRDFNodeDBLCLickOnPropertyInput;
	
	
	this.init = function(){
		boxForProperties.treecolLabel = "Properties";
		boxForProperties.setAttribute("class","tree-properties-rdfnode-widget");
		boxForProperties.clientTop;  // Force a style flush, so that we ensure our binding is attached.
		boxForProperties.setClassesTreeObj(this);
		if(typeof dblclickOnClassFunction != 'undefined'){
			//boxForClasses.addEventListener("dblclickOnTree",dblclickOnClassFunction, true);
			boxForProperties.addDblClickFunction(dblclickOnClassFunction);
		}
		
		//add the root (remember to "discover if it has child or not")
		//boxForClasses.addRoot(rootClass, true);
		for(var i=0; i<rootPropertiesArray.length; ++i)
			boxForProperties.addRootProperty(rootPropertiesArray[i]);
		
		if(typeof createContextMenu != 'undefined' && 
				(createContextMenu==true || createContextMenu == "true")){
			boxForProperties.createStandardContextMenu();
		}
	}
	
	
	this.addInstanceTree = function(instanceTree){
		boxForClasses.addInstanceTree(instanceTree);
	}
	
	this.getSelectedClass = function(){
		return boxForClasses.getSelectedRDFNode();
	}
	
	this.getSelectedInstance = function(){
		var selInstance;
		if(typeof boxForInstances != 'undefined')
			selInstance = boxForInstances.getSelectedRDFNode();
		return selInstance;
	}
	
	this.getClassesTreeBox = function(){
		return boxForClasses;
	}
	
	this.addToContextMenu = function(menutimeAnonId, label, associatedFunction, imageSrc){
		boxForClasses.addToContextMenu(menutimeAnonId, label, associatedFunction, imageSrc);
	}

	this.init();
	
}