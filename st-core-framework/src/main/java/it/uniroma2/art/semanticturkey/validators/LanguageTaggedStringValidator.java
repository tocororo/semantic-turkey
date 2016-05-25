package it.uniroma2.art.semanticturkey.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.rdf4j.model.Literal;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;

public class LanguageTaggedStringValidator implements
ConstraintValidator<LanguageTaggedString, Literal>{

	@Override
	public void initialize(LanguageTaggedString constraintAnnotation) {
		// Nothing to do
	}

	@Override
	public boolean isValid(Literal value, ConstraintValidatorContext context) {
		return value.getLanguage().isPresent();
	}

}
