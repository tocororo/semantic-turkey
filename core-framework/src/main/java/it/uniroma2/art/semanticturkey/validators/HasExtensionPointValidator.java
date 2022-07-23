package it.uniroma2.art.semanticturkey.validators;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validators associated with the constraint {@link HasExtensionPoint}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HasExtensionPointValidator implements ConstraintValidator<HasExtensionPoint, Configuration> {

	@Autowired
	private ExtensionPointManager exptMgr;

	private Class<? extends Extension> requiredExtensionPoint;

	@Override
	public void initialize(HasExtensionPoint value) {
		this.requiredExtensionPoint = value.value();
	}

	@Override
	public boolean isValid(Configuration value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		try {
			exptMgr.buildPluginSpecification(requiredExtensionPoint, value);
		} catch(IllegalArgumentException e) {
			return false;
		}

		return true;
	}
}
