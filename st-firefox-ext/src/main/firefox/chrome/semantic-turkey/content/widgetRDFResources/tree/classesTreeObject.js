
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);
//Components.utils.import("resource://stservices/SERVICE_Individual.jsm", art_semanticturkey);

if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

art_semanticturkey.classesTreeObject = function(boxForClassesInput, rootClassArrayInput, 
		createClassesContextMenuInput, functionForRDFNodeDBLCLickOnClassInput, boxForInstancesInput,
		createIndividualsContextMenuInput, functionForRDFNodeDBLCLickOnInstanceInput){
	var boxForClasses = boxForClassesInput;
	var boxForInstances = boxForInstancesInput; 
	var createClassesContextMenu = createClassesContextMenuInput;
	var createIndividualsContextMenu = createIndividualsContextMenuInput;
	
	var rootClassArray = rootClassArrayInput;
	
	var dblclickOnClassFunction;
	if(typeof functionForRDFNodeDBLCLickOnClassInput != 'undefined')
		dblclickOnClassFunction = functionForRDFNodeDBLCLickOnClassInput;

	var dblclickOnInstaneFunction;
	if(typeof functionForRDFNodeDBLCLickOnInstanceInput != 'undefined')
		dblclickOnInstaneFunction = functionForRDFNodeDBLCLickOnInstanceInput;

	
	
	this.init = function(){
		boxForClasses.treecolLabel = "Classes";
		boxForClasses.setAttribute("class","tree-classes-rdfnode-widget");
		boxForClasses.clientTop;  // Force a style flush, so that we ensure our binding is attached.
		boxForClasses.setClassesTreeObj(this);
		if(typeof dblclickOnClassFunction != 'undefined'){
			//boxForClasses.addEventListener("dblclickOnTree",dblclickOnClassFunction, true);
			boxForClasses.addDblClickFunction(dblclickOnClassFunction);
		}
		
		if(typeof boxForInstances != 'undefined'){
			//create an instances tree inside the boxForInstances
			var individualTreeObject = new art_semanticturkey.individualsTreeObject(boxForInstances, 
					createIndividualsContextMenu, dblclickOnInstaneFunction);
			boxForClasses.addInstanceTree(boxForInstances);
		}
		
		//add the root (remember to "discover if it has child or not")
		//boxForClasses.addRoot(rootClass, true);
		for(var i=0; i<rootClassArray.length; ++i)
			boxForClasses.addRootClass(rootClassArray[i]);
		
		if(typeof createClassesContextMenu != 'undefined' && 
				(createClassesContextMenu==true || createClassesContextMenu == "true")){
			boxForClasses.createStandardContextMenu();
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