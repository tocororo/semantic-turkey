package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.constraints.HasDatatype;

/**
 * Validates {@link HasDatatype} constraints.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DatatypeValidator implements ConstraintValidator<HasDatatype, Literal> {

	private IRI requiredDatatype;

	@Override
	public void initialize(HasDatatype constraintAnnotation) {
		this.requiredDatatype = SimpleValueFactory.getInstance().createIRI(constraintAnnotation.value());
	}

	@Override
	public boolean isValid(Literal value, ConstraintValidatorContext context) {
		return requiredDatatype.equals(value.getDatatype());
	}

}
