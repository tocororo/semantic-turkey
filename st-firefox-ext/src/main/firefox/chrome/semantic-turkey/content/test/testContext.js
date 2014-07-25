
if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/test/Test.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Context.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);


art_semanticturkey.onloadTextContext = function() {
	document.getElementById("testButton1").addEventListener("command",
			art_semanticturkey.getDefaultContext, true);
	

	document.getElementById("testButton2").addEventListener("command",
			art_semanticturkey.setNewContext1, true);
	document.getElementById("testButton3").addEventListener("command",
			art_semanticturkey.getNewContext1, true);

	
	document.getElementById("testButton1Copied").addEventListener("command",
			art_semanticturkey.setNewContextCopied, true);
	document.getElementById("testButton2Copied").addEventListener("command",
			art_semanticturkey.getNewContextCopied, true);
	
	document.getElementById("testButton1Cloned").addEventListener("command",
			art_semanticturkey.setNewContextCloned, true);
	document.getElementById("testButton2Cloned").addEventListener("command",
			art_semanticturkey.getNewContextCloned, true);
	
	
	document.getElementById("testButton1Array").addEventListener("command",
			art_semanticturkey.setNewContextArray, true);
	document.getElementById("testButton2Array").addEventListener("command",
			art_semanticturkey.getNewContextArray, true);
	
}

art_semanticturkey.getDefaultContext = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	
	//for a test, try to change the dafault context
	//art_semanticturkey.Logger.debug("PRE defaultContext.getProject() = "+defaultContext.getProject());
	//defaultContext.setProject("progettoProva");
	//defaultContext.setWGraph("grafoProva");
	//defaultContext.addValue("pippo","paperino");
	//art_semanticturkey.Logger.debug("POST defaultContext.getProject() = "+defaultContext.getProject());
	// end of the test
	
	art_semanticturkey.writeContext(defaultContext, defaultContext);
	
	art_semanticturkey.Test.fakeRequest();
	
}



art_semanticturkey.setNewContext1 = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var specifiedContext = new art_semanticturkey.Context();
	specifiedContext.createNewArrayForContext();
	var nameForContext1 = document.getElementById("nameForContext1").value;
	var valueForContext1 = document.getElementById("valueForContext1").value;
	specifiedContext.addValue(nameForContext1, valueForContext1 );
	
	art_semanticturkey.newServiceInstance1 = art_semanticturkey.Test.getAPI(specifiedContext);
	art_semanticturkey.writeContext(defaultContext, specifiedContext);
	
	art_semanticturkey.newServiceInstance1.fakeRequest();
}

art_semanticturkey.getNewContext1 = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var specifiedContext = art_semanticturkey.newServiceInstance1.context;
	art_semanticturkey.writeContext(defaultContext, specifiedContext);
	
	art_semanticturkey.newServiceInstance1.fakeRequest();
}



//COPY TEST
art_semanticturkey.setNewContextCopied = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var specifiedContext = new art_semanticturkey.Context();
	specifiedContext.createNewArrayForContext();
	//specifiedContext.copyContext(art_semanticturkey.newServiceInstance1.context);
	specifiedContext.copy(art_semanticturkey.newServiceInstance1.context);
	var nameForContext2 = document.getElementById("nameForContextCopied").value;
	var valueForContext2 = document.getElementById("valueForContextCopied").value;
	specifiedContext.addValue(nameForContext2, valueForContext2 );
	
	art_semanticturkey.newServiceInstance2 = art_semanticturkey.Test.getAPI(specifiedContext);
	art_semanticturkey.writeContext(defaultContext, specifiedContext);
	
	art_semanticturkey.newServiceInstance2.fakeRequest();
}

art_semanticturkey.getNewContextCopied = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var specifiedContext = art_semanticturkey.newServiceInstance2.context;
	
	art_semanticturkey.writeContext(defaultContext, specifiedContext);
	
	art_semanticturkey.newServiceInstance2.fakeRequest();
}

//CLONE TEST
art_semanticturkey.setNewContextCloned = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var clonedContext = art_semanticturkey.newServiceInstance1.context.clone();
	var nameForContext2 = document.getElementById("nameForContextCloned").value;
	var valueForContext2 = document.getElementById("valueForContextCloned").value;
	clonedContext.addValue(nameForContext2, valueForContext2 );
	
	art_semanticturkey.newServiceInstance2 = art_semanticturkey.Test.getAPI(clonedContext);
	art_semanticturkey.writeContext(defaultContext, clonedContext);
	
	art_semanticturkey.newServiceInstance2.fakeRequest();
}

art_semanticturkey.getNewContextCloned = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var clonedContext = art_semanticturkey.newServiceInstance2.context;
	
	art_semanticturkey.writeContext(defaultContext, clonedContext);
	
	art_semanticturkey.newServiceInstance2.fakeRequest();
}

//ARRAY TEST
art_semanticturkey.setNewContextArray = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var specifiedContext = new art_semanticturkey.Context();
	specifiedContext.createNewArrayForContext();
	var nameForProject3 = document.getElementById("nameForProjectArray").value;
	//take the value for the wGraph
	var valueForWGraph3 = document.getElementById("valueForWGraphArray").value;
	specifiedContext.setProject(nameForProject3);
	specifiedContext.setWGraph(valueForWGraph3);
	specifiedContext.addValue("donald","duck");
	//take the value(s) for the rGraphs
	var rGraphsArray = new Array();
	var count = 0;
	for(var i=1; i<4; ++i){
		var valueForRGraph31 = document.getElementById("valueForRGraphArray"+i).value;
		if(valueForRGraph31 != "")
			rGraphsArray[count++] = valueForRGraph31;
	}
	specifiedContext.setRGraphs(rGraphsArray);
	
	art_semanticturkey.newServiceInstance3 = art_semanticturkey.Test.getAPI(specifiedContext);
	art_semanticturkey.writeContext(defaultContext, specifiedContext);
	
	art_semanticturkey.newServiceInstance3.fakeRequest2();
}

art_semanticturkey.getNewContextArray = function(){
	var defaultContext = art_semanticturkey.Test.context;
	
	var specifiedContext = art_semanticturkey.newServiceInstance3.context;
	
	art_semanticturkey.writeContext(defaultContext, specifiedContext);
	
	art_semanticturkey.newServiceInstance3.fakeRequest2();
}


/***************************************************************/



art_semanticturkey.writeContext = function(defaultContext, usedContext){
	art_semanticturkey.writeDefaultContext(defaultContext.getContextValuesAsString());
	art_semanticturkey.writeUsedContext(usedContext.getContextValuesAsString());
}

art_semanticturkey.writeDefaultContext = function(valueString){
	document.getElementById("defaulContextTextBox").value = valueString;
}

art_semanticturkey.writeUsedContext = function(valueString){
	document.getElementById("usedContextTextBox").value = valueString;
}



window.addEventListener("load",
		art_semanticturkey.onloadTextContext, true);
