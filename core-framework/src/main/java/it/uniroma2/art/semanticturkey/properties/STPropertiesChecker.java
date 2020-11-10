package it.uniroma2.art.semanticturkey.properties;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

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
				Object v = props.getPropertyValue(p);
				if (props.isRequiredProperty(p)) {
					if (isNullish(v)) {
						setErrorMessage("property: " + p + " has not been set");
						return false;
					} else {
						if (v instanceof STProperties) {
							if (isNullish(v)) {
								setErrorMessage("property: " + p + " is an incomplete nested structure");
								return false;
							}
						} else if (v instanceof Collection<?>) {
							Collection<?> coll = (Collection<?>) v;
						
							if (coll.stream().anyMatch(STPropertiesChecker::isNullish)) {
								setErrorMessage(
										"property: " + p + " is a collection containing an incomplete item");
								return false;
							}
						}
					}
				}
			}
		} catch (PropertyNotFoundException e) {
			// really could never happen if the property implementation is self consistent!!!
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static boolean isNullish(Object v) {
		// @formatter:off
		return v == null || 
				(v instanceof String && StringUtils.isAllBlank((String) v)) ||
				(v instanceof STProperties && !STPropertiesChecker.getModelConfigurationChecker((STProperties)v).isValid()) ||
				(v instanceof Collection<?> && (
						((Collection<?>)v).isEmpty() || ((Collection<?>)v).stream().anyMatch(STPropertiesChecker::isNullish)));
		// @formatter:on
	}

	private void setErrorMessage(String msg) {
		errorMsg = msg;
	}

	public String getErrorMessage() {
		return errorMsg;
	}

}
