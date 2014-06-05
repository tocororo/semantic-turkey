
if (typeof art_semanticturkey == 'undefined')
	var art_semanticturkey = {};

Components.utils.import("resource://stmodules/test/Test.jsm", art_semanticturkey);
Components.utils.import("resource://stmodules/Logger.jsm", art_semanticturkey);


art_semanticturkey.onloadSTHttpMgr = function() {
	document.getElementById("testButton8").addEventListener("command",
			art_semanticturkey.executeFakeGETRequest, true);
	document.getElementById("testButton9").addEventListener("command",
			art_semanticturkey.executeFakePOSTRequest, true);
}


art_semanticturkey.executeFakeGETRequest = function(){
	var groupId = document.getElementById("groupIdGET").value;
	var artifactId = document.getElementById("artifactIdGET").value;
	var service = document.getElementById("serviceIdGET").value;
	var request = document.getElementById("requestIdGET").value;
	var param1 = document.getElementById("param1IdGET").value;
	var param2 = document.getElementById("param2IdGET").value;
	
	//alert("groupId = "+groupId+"\nartifactId = "+artifactId+"\nservice = "+service+"\nrequest = " + 
	//		requet+"\nparam1Id = "+param1+ "\nparam2Id = "+param2);
	
	art_semanticturkey.Test.fakeRequest3(groupId, artifactId, service, request, param1, param2);
}

art_semanticturkey.executeFakePOSTRequest = function(){
	var groupId = document.getElementById("groupIdPOST").value;
	var artifactId = document.getElementById("artifactIdPOST").value;
	var service = document.getElementById("serviceIdPOST").value;
	var request = document.getElementById("requestIdPOST").value;
	var param1 = document.getElementById("param1IdPOST").value;
	var param2 = document.getElementById("param2IdPOST").value;
	
	//alert("groupId = "+groupId+"\nartifactId = "+artifactId+"\nservice = "+service+"\nrequest = " + 
	//		requet+"\nparam1Id = "+param1+ "\nparam2Id = "+param2);
	
	art_semanticturkey.Test.fakeRequest4(groupId, artifactId, service, request, param1, param2);
}



window.addEventListener("load",
		art_semanticturkey.onloadSTHttpMgr, true);