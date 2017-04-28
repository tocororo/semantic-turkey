package it.uniroma2.art.semanticturkey.properties;

import java.util.Collection;

public class STPropertiesChecker {

	private STProperties props;
	private String errorMsg;

	private STPropertiesChecker(STProperties conf) {
		this.props = conf;
	}

	public static STPropertiesChecker getModelConfigurationChecker(STProperties conf) {
		return new STPropertiesChecker(conf);
	}

	/**
	 * Tells if the handed {@link STProperties} are valid. The concept of validity is currently limited to
	 * checking that the values for all required properties have been set. Further constraints could be added
	 * in the future.<br/>
	 * 
	 * @return <code>true</code> if the handed property set is valid.
	 */
	public boolean isValid() {
		Collection<String> pars = props.getProperties();
		try {
			for (String p : pars) {
				if (props.isRequiredProperty(p) && (props.getPropertyValue(p) == null)) {
					setErrorMessage("property: " + p + " has not been set");
					return false;
				}
			}
		} catch (PropertyNotFoundException e) {
			// really could never happen if the property implementation is self consistent!!!
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
