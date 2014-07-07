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

let EXPORTED_SYMBOLS = [ "STInfo" ];

STInfo = new function() {
	
	this.getGroupId = function(){
		return "it.uniroma2.art.semanticturkey";
	}
	
	this.getArtifactId = function(){
		return "st-core-services";
	}
	
	this.getSystemProjectName = function(){
		return "SYSTEM";
	}
	
	this.getAccessRW = function(){
		return "RW";
	}
	
	this.getAccessR = function(){
		return "R";
	}
	
	this.getLockW= function(){
		return "W";
	}
	
	this.getLockR= function(){
		return "R";
	}
	
	this.getLockNO= function(){
		return "NO";
	}
}