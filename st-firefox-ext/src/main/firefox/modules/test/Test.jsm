/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is SemanticTurkey.
 * 
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 * 
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART) Current
 * information about SemanticTurkey can be obtained at
 * http://semanticturkey.uniroma2.it
 * 
 */
 

let EXPORTED_SYMBOLS = ["Test"];
Components.utils.import("resource://stmodules/Preferences.jsm");
Components.utils.import("resource://stmodules/Logger.jsm"); 
Components.utils.import("resource://stmodules/Context.jsm");

function Test(){};

function fakeRequest(){
	Logger.debug('[Test.jsm] fakeRequest, context: '+this.context.getContextValuesAsString());
}

function fakeRequest2(){
	Logger.debug("[Test.jsm] fakeRequest, context: "+
			"ctx_project="+this.context.getProject()+
			"&ctx_wGraph="+this.context.getWGpragh()+
			"&"+this.context.getContextValuesAsString());
	//Logger.debug('[Test.jsm] fakeRequest, context: '+this.context.getContextValuesAsString());
}

Test.prototype.getAPI = function(specifiedContext){
	var newObj = new Test();
	newObj.context = specifiedContext;
	return newObj;
}
Test.prototype.fakeRequest = fakeRequest;
Test.prototype.fakeRequest2 = fakeRequest2;
Test.prototype.context = new Context();  // set the default context
Test.constructor = Test;
Test.__proto__ = Test.prototype;


