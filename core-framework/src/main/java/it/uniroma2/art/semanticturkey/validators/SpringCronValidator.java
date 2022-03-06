package it.uniroma2.art.semanticturkey.validators;

import it.uniroma2.art.semanticturkey.constraints.HasDatatype;
import it.uniroma2.art.semanticturkey.constraints.SpringCron;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validates {@link it.uniroma2.art.semanticturkey.constraints.SpringCron} constraint.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SpringCronValidator implements ConstraintValidator<SpringCron, String> {

	@Override
	public void initialize(SpringCron constraintAnnotation) {
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		try {
			new CronSequenceGenerator(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}
