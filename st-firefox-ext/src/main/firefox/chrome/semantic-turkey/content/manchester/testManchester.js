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
	
	
	document.getElementById("addNewSubClassExpressionButton").addEventListener("click",
			art_semanticturkey.checkAndAddNewSubClassExpressiononAccept, true);
	document.getElementById("addNewEquivalentExpressionButton").addEventListener("click",
			art_semanticturkey.checkAndAddNewEquivalentExpressiononAccept, true);
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
	//var treeitem = event.detail.ti;
	var rdfNode = event.detail.rdfNode;
	
	art_semanticturkey.refreshRestriction(rdfNode);
	
	
}

art_semanticturkey.refreshRestriction = function(rdfNode){
	var labelShowExpressions = document.getElementById("labelShowExpressions");
	labelShowExpressions.value = "show existing Manchester Expressions for "+rdfNode.getShow();
	labelShowExpressions.selClass = rdfNode.getURI();
	
	
	var labelShowExpressions = document.getElementById("labelAddExpressions");
	labelShowExpressions.value = "Add a new Manchester Expression for "+rdfNode.getShow();
	labelShowExpressions.selClass = rdfNode.getURI();
	labelShowExpressions.rdfNode = rdfNode;
	
	var vboxForExpr = document.getElementById("vboxForExpressions");
	//remove all the info about the previous Class
	art_semanticturkey.removeAllChildOfElement(vboxForExpr)
	
	//get all the possible expressions associated to this class
	var response = art_semanticturkey.STRequests.Manchester.getAllDLExpression(rdfNode.getURI());
	var collectionElem = response.getElementsByTagName("collection")[0];
	//get all the equivalent class
	var eqClassElements = collectionElem.getElementsByTagName("equivalentClass");
	if(eqClassElements.length>0){
		var labelEqui = document.createElement("label");
		labelEqui.setAttribute("value", "Equivalent to:");
		vboxForExpr.appendChild(labelEqui);
	}
	for(var i=0; i<eqClassElements.length; ++i){
		var eqClassElem = eqClassElements[i];
		var expr = eqClassElem.getAttribute("expression");
		var tempHbox = document.createElement("hbox");
		var removeButton = document.createElement("button");
		removeButton.setAttribute("label", "remove restriction");
		removeButton.classUri = rdfNode.getURI();
		removeButton.expr = expr;
		removeButton.exprType = "http://www.w3.org/2002/07/owl#equivalentClass";
		removeButton.bnode = "_:"+eqClassElem.getAttribute("bnode");
		removeButton.rdfNode = rdfNode;
		removeButton.addEventListener("click", 
				art_semanticturkey.removeExpression, true);
		tempHbox.appendChild(removeButton);
		var showExprTextbox = document.createElement("textbox");
		showExprTextbox.setAttribute("value", expr);
		showExprTextbox.setAttribute("readonly", "true");
		showExprTextbox.setAttribute("flex", "1");
		tempHbox.appendChild(showExprTextbox);
		
		vboxForExpr.appendChild(tempHbox);
	}
	
	//get all the subClass
	var subClassElements = collectionElem.getElementsByTagName("subClass");
	if(subClassElements.length>0){
		var labelEqui = document.createElement("label");
		labelEqui.setAttribute("value", "SubClass of:");
		vboxForExpr.appendChild(labelEqui);
	}
	for(var i=0; i<subClassElements.length; ++i){
		var subClassElem = subClassElements[i];
		var expr = subClassElem.getAttribute("expression");
		var tempHbox = document.createElement("hbox");
		var removeButton = document.createElement("button");
		removeButton.setAttribute("label", "remove restriction");
		removeButton.classUri = rdfNode.getURI();
		removeButton.expr = expr;
		removeButton.exprType = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
		removeButton.bnode = "_:"+subClassElem.getAttribute("bnode");
		removeButton.rdfNode = rdfNode;
		removeButton.addEventListener("click", 
				art_semanticturkey.removeExpression, true);
		tempHbox.appendChild(removeButton);
		var showExprTextbox = document.createElement("textbox");
		showExprTextbox.setAttribute("value", expr);
		showExprTextbox.setAttribute("readonly", "true");
		showExprTextbox.setAttribute("flex", "1");
		tempHbox.appendChild(showExprTextbox);
		
		vboxForExpr.appendChild(tempHbox);
	}
}


art_semanticturkey.removeAllChildOfElement = function(elem){
	while(elem.childElementCount>0){
		elem.removeChild(elem.firstChild);
	}
}

art_semanticturkey.removeExpression = function(event){
	var button = event.currentTarget;
	var response = art_semanticturkey.STRequests.Manchester.removeExpression(button.classUri, button.exprType, 
			button.bnode);
	if(response.isOk()){
		art_semanticturkey.refreshRestriction(button.rdfNode);
	}
	
}

art_semanticturkey.checkAndAddNewSubClassExpressiononAccept = function(event){
	art_semanticturkey.checkAndAddNewExpressiononAccept(false);
}

art_semanticturkey.checkAndAddNewEquivalentExpressiononAccept = function(event){
	art_semanticturkey.checkAndAddNewExpressiononAccept(true);
}

art_semanticturkey.checkAndAddNewExpressiononAccept = function(isEquivalent){
	
	classUri = document.getElementById("labelAddExpressions").selClass;
	
	if(typeof classUri == 'undefined'){
		alert("plese select a class from the class tree by performing a double click on the desired class");
		return;
	}
	
	//first check the expression
	expr = document.getElementById("newExpressionTextBox").value;
	var response = art_semanticturkey.STRequests.Manchester.checkExpression(expr);
	if(response.isFail()){
		var text = response.getElementsByTagName("reply")[0].textContent;
		alert(text);
		//since the expression contains at least one syntactic error, stop here
		return;
	}
	//then add the expression
	try{
		if(isEquivalent){
			var exprType ="http://www.w3.org/2002/07/owl#equivalentClass";
			response = art_semanticturkey.STRequests.Manchester.createRestriction(classUri, exprType, expr);
		} else{
			var exprType ="http://www.w3.org/2000/01/rdf-schema#subClassOf";
			response = art_semanticturkey.STRequests.Manchester.createRestriction(classUri, exprType, expr);
		}
		
		
		if(response.isOk()){
			art_semanticturkey.refreshRestriction(document.getElementById("labelAddExpressions").rdfNode);
		}
	}
	catch(err) {
	    alert("there was a problem when dealing with expression: "+expr);
	}
	
}
