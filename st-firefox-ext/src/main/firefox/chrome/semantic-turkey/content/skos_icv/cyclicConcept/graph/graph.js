window.onload = function() {
	
	var edges = window.arguments[0].edges;

	var g = new Graph();
	g.edgeFactory.template.style.directed = true;
	
	for (var i=0; i<edges.length; i++){
		var e = edges[i];
		g.addEdge(e[0], e[1]);
	}
		 
	var layouter = new Graph.Layout.Spring(g);
	layouter.layout();
	 
	var renderer = new Graph.Renderer.Raphael('canvas', g, 400, 300);
	renderer.draw();

}
