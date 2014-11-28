if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};
Components.utils.import("resource://stservices/SERVICE_SKOS_ICV.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/ResourceViewLauncher.jsm", art_semanticturkey);

window.onload = function() {
	art_semanticturkey.init();
}

var cycles;

art_semanticturkey.init = function(){
	var listbox = document.getElementById("listbox");
	try {
		var xmlResp = art_semanticturkey.STRequests.SKOS_ICV.listCyclicConcepts();
		var data = xmlResp.getElementsByTagName("data")[0];
		var records = data.getElementsByTagName("record");
		
		/* cycles is an array of concept-graph pairs, where concept is the concept cause of the cycle (that
		 * concept that has a parent that is also a (transitive child) and graph is an array of edges (pairs
		 * node1-node2). NOTE: if more graph are cause by the same concept, they will belong to the same
		 * cycles[] entry.
		 */
		cycles = new Array();
		for (var i=0; i<records.length; i++){
			var record = records[i];
			var concept = record.getAttribute("topCyclicConcept");
			var node1 = record.getAttribute("node1");
			var node2 = record.getAttribute("node2");
			
			//search into cycles if there's already a cycle cause by concept
			var idxC = -1;
			for (var j=0; j<cycles.length; j++){
				var cycle = cycles[j];
				if (cycle[0] == concept){
					idxC = j;
					break;
				}
			}
			if (idxC != -1){ //cycle for concept already in cycles[]
				var c = cycles[idxC];
				c[1].push([node1, node2]);//c[1] is graph associated to concept (c[0])
			} else {//cycle for concept not already in cycles[]
				var edge = new Array();
				edge.push([node1, node2]);
				cycles.push([concept, edge]);//add pair concept-edge to cycles
			}
		}
		
		for (var i=0; i<cycles.length; i++){
			var cycle = cycles[i];
			var concept = cycle[0];
			var graph = cycle[1];
			art_semanticturkey.Logger.debug("concept: " + concept);
			art_semanticturkey.Logger.debug("graph ("+graph.length+"): ");
			for (var j=0; j<graph.length; j++){
				var edge = graph[j];
				art_semanticturkey.Logger.debug(edge[0]+" - "+edge[1]);
			}
		}
			
		//init UI
		for (var i=0; i<cycles.length; i++){
			var cycle = cycles[i];
			var concept = cycle[0];
			for (var j=0; j<graph.length; j++){
				var edge = graph[j];
			}
		
			var listitem = document.createElement("listitem");
			listitem.setAttribute("allowevents", "true");
			
			var cell = document.createElement("listcell");
		    cell.setAttribute("label", concept);
		    cell.addEventListener("dblclick", art_semanticturkey.conceptDblClickListener, false);
		    listitem.appendChild(cell);
		    
		    var btnEdit = document.createElement("button");
		    btnEdit.setAttribute("label", "Edit concept");
		    btnEdit.setAttribute("flex", "1");
		    btnEdit.addEventListener("command", art_semanticturkey.fixButtonClickListener, false);
		    listitem.appendChild(btnEdit);
		    
		    var btnGraph = document.createElement("button")
		    btnGraph.setAttribute("label", "Graph");
		    btnGraph.setAttribute("flex", "1");
		    btnGraph.addEventListener("command", art_semanticturkey.graphButtonClickListener, false);
		    //associate the graph caused by concept to the button through an attribute 
		    listitem.appendChild(btnGraph);
			
			listbox.appendChild(listitem)
		}
	} catch (e){
		alert(e.message);
	}
}

art_semanticturkey.fixButtonClickListener = function() {
	var btn = this;
	var listitem = btn.parentNode;
	var concept = listitem.children[0].getAttribute("label");
	var parameters = new Object();
	parameters.sourceType = "concept";
	parameters.sourceElement = concept;
	parameters.sourceElementName = concept;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);
}

/**
 * Listener to the concept, when double clicked it opens the editor panel
 */
art_semanticturkey.conceptDblClickListener = function() {
	var concept = this.getAttribute("label");//this in an actionListener represents the target of the listener
	var parameters = new Object();
	parameters.sourceType = "concept";
	parameters.sourceElement = concept;
	parameters.sourceElementName = concept;
	parameters.parentWindow = window;
	parameters.isFirstEditor = true;
	art_semanticturkey.ResourceViewLauncher.openResourceView(parameters);

}

art_semanticturkey.graphButtonClickListener = function(){
	var btn = this;
	var listitem = btn.parentNode;
	var concept = listitem.children[0].getAttribute("label");
	var graph;
	for (var i=0; i<cycles.length; i++){//retrieve cycle associated to concept
		var cycle = cycles[i];
		if (cycle[0] == concept){
			graph = cycle[1];
			break;
		}
	}
	var edges = new Array();
	for (var i=0; i<graph.length; i++){
		edges.push(graph[i]);
	}
	var parameters = new Object();
	parameters.edges = edges;
	window.openDialog("chrome://semantic-turkey/content/skos_icv/cyclicConcept/graph/graph.html", "_blank",
			"chrome,dependent,dialog,modal=yes,resizable,centerscreen", parameters);
}