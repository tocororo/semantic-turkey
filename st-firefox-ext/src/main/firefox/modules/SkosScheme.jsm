Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/Preferences.jsm");
Components.utils.import("resource://stmodules/stEvtMgr.jsm");

EXPORTED_SYMBOLS = ["SkosScheme"];

var PREFERENCE_BRANCH = "extensions.semturkey.skos.selected_scheme.";

SkosScheme = {
		
		getSelectedScheme : function(projectName) {
			return Preferences.get(getPreferenceName(projectName));
		},

		//to invoke when the user change Scheme
		setSelectedScheme : function(projectName, scheme) {
			Preferences.set(getPreferenceName(projectName), scheme);
			
			evtMgr.fireEvent("projectPropertySet", {
				getPropName : function(){return "skos.selected_scheme";}, 
				getPropValue : function(){return scheme;}, 
				getContext : function(){return undefined;}});
		},
		
		//to invoke when user unchecks the scheme and works in noScheme mode
		setNoSelectedScheme : function(projectName) {
			Preferences.set(getPreferenceName(projectName), "*");
			
			evtMgr.fireEvent("projectPropertySet", {
				getPropName : function(){return "skos.selected_scheme";}, 
				getPropValue : function(){return "*";}, 
				getContext : function(){return undefined;}});
		},
		
		//to invoke when the project is deleted and thus the scheme has no more reason to exist
		removeSelectedScheme : function(projectName) {
			Preferences.reset(getPreferenceName(projectName));
		},
		
		//to invoke after listProjects, this method remove eventual selectedScheme pref for which
		//the related project doesn't exist no more (e.g. deleted simply by removing its folder)
		cleanDanglingSelectedSchemes : function(projectNameList){
			var prefs = Components.classes["@mozilla.org/preferences-service;1"]
        		.getService(Components.interfaces.nsIPrefService);
			var branch = prefs.getBranch(PREFERENCE_BRANCH);
			var children = branch.getChildList("", {});
			for (var i=0; i<children.length; i++){
				var exist = false;
				var projName = children[i];
				//check if projName is an existing project name
				for (var j=0; j<projectNameList.length; j++){
					if (projName == projectNameList[j]){
						exist = true;
						break;
					}
				}
				//if not remove the preference
				if (!exist) {
					Preferences.reset(getPreferenceName(projName));
				}
			}
		}
		
}

getPreferenceName = function(projectName) {
	return PREFERENCE_BRANCH + projectName;
}