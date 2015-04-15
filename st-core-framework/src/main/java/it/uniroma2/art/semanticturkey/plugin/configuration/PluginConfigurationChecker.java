 /*
  * The contents of this file are subject to the Mozilla Public License
  * Version 1.1 (the "License");  you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  * http//www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
  * the specific language governing rights and limitations under the License.
  *
  * The Original Code is ART OWL API.
  *
  * The Initial Developer of the Original Code is University of Roma Tor Vergata.
  * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
  * All Rights Reserved.
  *
  * The ART OWL API were developed by the Artificial Intelligence Research Group
  * (art.uniroma2.it) at the University of Roma Tor Vergata
  * Current information about the ART OWL API can be obtained at 
  * http://art.uniroma2.it/owlart
  *
  */

package it.uniroma2.art.semanticturkey.plugin.configuration;

import java.util.Collection;

public class PluginConfigurationChecker {

	private PluginConfiguration conf;
	private String errorMsg;
	
	private PluginConfigurationChecker(PluginConfiguration conf) {
		this.conf = conf;
	}
	
	public static PluginConfigurationChecker createPluginConfigurationChecker(PluginConfiguration conf) {
		return new PluginConfigurationChecker(conf);
	}

	/**
	 * Tells if the present configuration (at its current status) is valid. The concept of validity is limited
	 * (in the current specification), to checking that all required parameters have been specified. Further
	 * constraints could be added in the future.<br/>
	 * 
	 * @return <code>true</code> if the current configuration is valid.
	 */
	public boolean isValid() {
		Collection<String> pars = conf.getConfigurationParameters();
		try {
			for (String p : pars) {
				if (conf.isRequiredParameter(p) && (conf.getParameterValue(p) == null)) {
					setErrorMessage("parameter: " + p + " has not been set");
					return false;
				}
			}
		} catch (ConfParameterNotFoundException e) {
			// really could never happen if the conf implementation is self consistent!!!
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void setErrorMessage(String msg) {
		errorMsg = msg;
	}
	
	
	public String getErrorMessage() {
		return errorMsg; 
	}
	
}
