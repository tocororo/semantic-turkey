if (typeof art_semanticturkey == 'undefined') var art_semanticturkey = {};

Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ARTResources.jsm", art_semanticturkey);

//Components.utils.import("resource://stservices/SERVICE_Cls.jsm", art_semanticturkey);


window.onload = function(){
	
	/*art_semanticturkey.Logger.debug("TEST dentro JS - inizio");
	
	
	var hboxForTree1 = document.getElementById("hboxForTree1");
	hboxForTree1.treecolLabel = "myTree1";
	//hboxForTree1.typeChildWidget = "RDFchild-tree-widget";
	hboxForTree1.setAttribute("class","tree-widget-rdfnode");
	hboxForTree1.clientTop;  // Force a style flush, so that we ensure our binding is attached.
	
	//alert("test");
	//while(typeof hboxForTree1.addRoot == 'undefined'){
		//art_semanticturkey.wait();
		//art_semanticturkey.Logger.debug("&&&&&&&&&&dentro il loop del wait");
	//}
	
	var rdfNode1 = new art_semanticturkey.ARTURIResource("paperone", "concept", "http://test#paperone");
	var rdfNode2 = new art_semanticturkey.ARTURIResource("paperino", "concept", "http://test#paperino");
	var rdfNode3 = new art_semanticturkey.ARTLiteral("Casa", "", "it", false);
	var rdfNode4 = new art_semanticturkey.ARTLiteral("dog", "http://test.it#int", "", false);
	var rdfNode5 = new art_semanticturkey.ARTLiteral("cane", "", "it", false);
	var rdfNode6 = new art_semanticturkey.ARTURIResource("topolino", "concept", "http://test#topolino");
	var rdfNode7 = new art_semanticturkey.ARTLiteral("eliminare", "", "it", false);
	
	
	hboxForTree1.addRoot(rdfNode1);
	
	hboxForTree1.addRoot(rdfNode2, false);
	
	hboxForTree1.addRoot(rdfNode3, false);
	
	hboxForTree1.addRoot(rdfNode4, false);
	
	
	//hboxForTree1.addChild(rdfNode2, rdfNode3, false);
	
	
	hboxForTree1.addChild(rdfNode1, rdfNode5, false);
	hboxForTree1.addChild(rdfNode1, rdfNode6, false);
	
	hboxForTree1.addChild(rdfNode5, rdfNode6, false);
	
	
	hboxForTree1.addChild(rdfNode3, rdfNode6, false);
	hboxForTree1.addChild(rdfNode6, rdfNode7, false);
	
	hboxForTree1.setRowsNumber(10);
	
	
	
	var buttonGetSelRDFNodeFromTree = document.getElementById("getSelRDFNodeFromTree");
	buttonGetSelRDFNodeFromTree.addEventListener("click",
			art_semanticturkey.getSelRDFNodeFromTree, true);
	
	var buttonDelRDFNodeFromTree = document.getElementById("getDelRDFNodeFromTree");
	buttonDelRDFNodeFromTree.addEventListener("click",
			art_semanticturkey.getDelRDFNodeFromTree, true);
	
	var buttonDelRDFNodeFromTree = document.getElementById("changeRDFNodeFromTree");
	buttonDelRDFNodeFromTree.addEventListener("click",
			art_semanticturkey.changeRDFNodeFromTree, true);
	
	hboxForTree1.addEventListener("dblclickOnTree",
			art_semanticturkey.dblclickOnTree, true);
	
	
	art_semanticturkey.Logger.debug("TEST dentro JS - fine");
	*/
	
	/*****************************************/
	/** TEST FOR CLASS AND INSTANCES TREES ***/
	/*****************************************/
	
	// one class tree and one instance tree
	
	var classesBox1 = document.getElementById("boxForClasses1");
	var instancesBox1 = document.getElementById("boxForInstances1");
	
	var rootClassArray1=art_semanticturkey.STRequests.Cls.getClassesInfoAsRootsForTree(true, 
				"http://www.w3.org/2002/07/owl#Thing");
	
	var treeClasses1 = new art_semanticturkey.classesTreeObject(classesBox1, rootClassArray1, true,
			art_semanticturkey.dblclickOnTreeToChangeIndividualsTree, instancesBox1, true, 
			art_semanticturkey.dblclickOnTree);
	treeClasses1.addToContextMenu("testId", "test label1");
	
	// one class tree and three instances tree
	var classesBox2 = document.getElementById("boxForClasses2");
	var instancesBox2_1 = document.getElementById("boxForInstances2-1");
	var instancesBox2_2 = document.getElementById("boxForInstances2-2");
	var instancesBox2_3 = document.getElementById("boxForInstances2-3");
	
	var rootClassArray2=art_semanticturkey.STRequests.Cls.getClassesInfoAsRootsForTree(true, 
				"http://www.w3.org/2002/07/owl#Thing");
	
	var treeClasses2 = new art_semanticturkey.classesTreeObject(classesBox2, rootClassArray2, true,
			art_semanticturkey.dblclickOnTreeToChangeIndividualsTree, instancesBox2_1, false, 
			art_semanticturkey.dblclickOnTree);
	var treeInstances2_2 = new art_semanticturkey.individualsTreeObject(instancesBox2_2, true, 
			art_semanticturkey.dblclickOnTree);
	var treeInstances2_3 = new art_semanticturkey.individualsTreeObject(instancesBox2_3, true, 
			art_semanticturkey.dblclickOnTree);
	
	
	//Add a listener on the class tree so the instance tree can detect the event
	//treeClasses2.addInstanceTree(treeInstances2_2.getIndividualsTreeBox());
	treeClasses2.getClassesTreeBox().addEventListener("infoOnRDFNodeEvent", 
			treeInstances2_2.getIndividualsTreeBox().infoOnRDFNodeEvent, true);
	
	//Try to add a new option in the context menu
	treeClasses2.addToContextMenu("testId", "test label2");
	
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

art_semanticturkey.dblclickOnTreeToChangeIndividualsTree = function(event){
	var treeitem = event.detail.ti;
	var rdfNode = event.detail.rdfNode;
	
	var instancesBox2_3 = document.getElementById("boxForInstances2-3");
	instancesBox2_3.getIndividualTreeObj().setIndividualsForClass(rdfNode);
	
}


