
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);
//Components.utils.import("resource://stservices/SERVICE_Individual.jsm", art_semanticturkey);

if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

art_semanticturkey.individualsTreeObject = function(boxForInstancesInput, createContextMenuInput,
		functionForRDFNodeDBLCLickOnInstanceInput){
	var boxForInstances = boxForInstancesInput;
	var createContextMenu = createContextMenuInput;

	var dblclickOnInstaneFunction;
	if(typeof functionForRDFNodeDBLCLickOnInstanceInput != 'undefined' &&
			functionForRDFNodeDBLCLickOnInstanceInput != null)
		dblclickOnInstaneFunction = functionForRDFNodeDBLCLickOnInstanceInput;
	
	this.init = function(){
		boxForInstances.treecolLabel = "Instances";
		boxForInstances.setAttribute("class","tree-individuals-rdfnode-widget");
		boxForInstances.clientTop;  // Force a style flush, so that we ensure our binding is attached.
		boxForInstances.setIndividualTreeObj(this);
		if(typeof dblclickOnInstaneFunction != 'undefined'){
			//boxForClasses.addEventListener("dblclickOnTree",dblclickOnClassFunction, true);
			boxForInstances.addDblClickFunction(dblclickOnInstaneFunction);
		}
		
		if(typeof createContextMenu != 'undefined' && 
				(createContextMenu==true || createContextMenu == "true")){
			boxForInstances.createStandardContextMenu();
		}
	}
	
	
	
	this.getSelectedInstance = function(){
		return boxForInstances.getSelectedRDFNode();
	}
	
	this.getIndividualsTreeBox = function(){
		return boxForInstances;
	}
	
	this.setIndividualsForClass = function(classRDFNode){
		boxForInstances.setIndividualsForClass(classRDFNode);
	}
	
	this.init();
	
}