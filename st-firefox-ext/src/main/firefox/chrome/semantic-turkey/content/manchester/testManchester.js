if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);

Components.utils.import("resource://stservices/SERVICE_Manchester.jsm", art_semanticturkey);
Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);


window.onload = function(){
	
	
	/*****************************************/
	/** TEST FOR CLASS AND INSTANCES TREES ***/
	/*****************************************/
	
	// one class tree and one instance tree
	
	var classesBox1 = document.getElementById("boxForClasses1");
	var instancesBox1 = document.getElementById("boxForInstances1");
	
	var rootClassArray1=art_semanticturkey.STRequests.Cls.getClassesInfoAsRootsForTree(true, 
				"http://www.w3.org/2002/07/owl#Thing");
	
	var treeClasses1 = new art_semanticturkey.classesTreeObject(classesBox1, rootClassArray1, true,
			art_semanticturkey.dblclickOnTreeToEditShowManchesterExpressions, instancesBox1, true, 
			art_semanticturkey.dblclickOnTree);
	treeClasses1.addToContextMenu("testId", "test label1");
	
	var newExpressionTextBox = document.getElementById("newExpression");
	
	
	document.getElementById("addNewExpressionButton").addEventListener("click",
			art_semanticturkey.checkAndAddNewExpressiononAccept, true);
	document.getElementById("newExpressionTextBox").addEventListener("command",
			art_semanticturkey.checkAndAddNewExpressiononAccept, true);
	
}

art_semanticturkey.testEvent = function(event){
	event.preventDefault();
    event.stopPropagation();
	alert("event test : event = "+event);
}

art_semanticturkey.getSelRDFNodeFromTree = function(){
	var hboxForTree1 = document.getElementById("hboxForTree1");
	var selRDFNode = hboxForTree1.getSelectedRDFNode();
	if(typeof selRDFNode == 'undefined'){
		alert("selRDFNode is undefined");
		return;
	}
	var idRDFNode = hboxForTree1.getIdFromRDFNode(selRDFNode);
	alert("idRDFNode = "+idRDFNode);
}


art_semanticturkey.getDelRDFNodeFromTree = function(){
	var checkboxParent = document.getElementById("getParentCheckBox");
	var isCheckedParent = false;
	if(checkboxParent.getAttribute("checked") == "true")
		isCheckedParent = true;
	
	var checkboxChild = document.getElementById("remWithChild");
	var isCheckedChild = false;
	if(checkboxChild.getAttribute("checked") == "true")
		isCheckedChild = true;
	
	var hboxForTree1 = document.getElementById("hboxForTree1");
	
	var selRDFNode = hboxForTree1.getSelectedRDFNode();
	if(typeof selRDFNode == 'undefined'){
		alert("selRDFNode is undefined");
		return;
	}
	var idDelRDFNode = hboxForTree1.getIdFromRDFNode(selRDFNode);
	
	var parentOfSelRDFNode = hboxForTree1.getParentRDFNodeFromSelectedRDFNode();
	var parentId;
	if(typeof parentOfSelRDFNode == 'undefined'){
		parentId = "parentOfSelRDFNode is undefined";
	} else{
		parentId = hboxForTree1.getIdFromRDFNode(parentOfSelRDFNode);
	}
	
	
	//alert("Deleting idRDFNode = "+idDelRDFNode +"\nchecked = "+isCheckedParent+"\nparentId = "+parentId+
	//		"\nwithChild = "+isCheckedChild);
	
	if(isCheckedParent==true && typeof parentOfSelRDFNode != 'undefined')	
		hboxForTree1.removeChild(selRDFNode, isCheckedChild, parentOfSelRDFNode);
	else
		hboxForTree1.removeChild(selRDFNode, isCheckedChild);
	
	
	
}

art_semanticturkey.changeRDFNodeFromTree = function(){
	var hboxForTree1 = document.getElementById("hboxForTree1");
	var showValue = document.getElementById("newRDFnodeShow").value;
	var uriValue = document.getElementById("newRDFnodeURI").value;
	
	alert("showValue = "+showValue+" uriValue = "+uriValue);
	
	var selRDFNode = hboxForTree1.getSelectedRDFNode();
	if(typeof selRDFNode == 'undefined'){
		alert("selRDFNode is undefined");
		return;
	}
	
	var newNode = new art_semanticturkey.ARTURIResource(showValue, "concept", uriValue);
	
	hboxForTree1.changeChild(selRDFNode, newNode);
}

art_semanticturkey.dblclickOnTree = function(event){
	var treeitem = event.detail.ti;
	var rdfNode = event.detail.rdfNode;
	
	var show;
	if(rdfNode.getShow != undefined){ // ARTURIResource
		show = rdfNode.getShow();
	} else if (rdfNode.getId != undefined){ // ARTBNode
		show = newRoot.getId(); 
	} else if(rdfNode.getLabel != undefined){
		show = rdfNode.getLabel();
		if(rdfNode.getLang != undefined && rdfNode.getLang() != ""){
			show += "@"+rdfNode.getLang();
		}
	} else{ // it should never enter here
		return;
	}
	
	alert("dblclick on rdfNode = "+show);
}

art_semanticturkey.dblclickOnTreeToEditShowManchesterExpressions = function(event){
	var treeitem = event.detail.ti;
	var rdfNode = event.detail.rdfNode;
	
	var labelShowExpressions = document.getElementById("labelShowExpressions");
	labelShowExpressions.value = "show existing Manchester Expressions for "+rdfNode.getShow();
	
	var labelShowExpressions = document.getElementById("labelAddExpressions");
	labelShowExpressions.value = "Add a new Manchester Expression for "+rdfNode.getShow();
	
	
}


art_semanticturkey.checkAndAddNewExpressiononAccept = function(event){
	//first check the expression
	expr = document.getElementById("newExpressionTextBox").value;
	alert("checking expression: "+expr);
	art_semanticturkey.STRequests.Manchester.checkExpression(expr);
	
	//then add the expression
}
