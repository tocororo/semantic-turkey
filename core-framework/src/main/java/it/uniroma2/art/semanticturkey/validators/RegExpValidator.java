package it.uniroma2.art.semanticturkey.validators;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Pattern.Flag;

import it.uniroma2.art.semanticturkey.constraints.HasDatatype;
import it.uniroma2.art.semanticturkey.constraints.RegExp;

/**
 * Validates {@link HasDatatype} constraints.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RegExpValidator implements ConstraintValidator<RegExp, CharSequence> {

	private Pattern pattern;

	@Override
	public void initialize(RegExp constraintAnnotation) {
		int flags = 0;
		for (Flag flag : constraintAnnotation.flags()) {
			flags |= flag.getValue();
		}

		try {
			pattern = java.util.regex.Pattern.compile(constraintAnnotation.regexp(), flags);
		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException("Invalid regular expression", e);
		}
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if (value == null)
			return true;

		return pattern.matcher(value).matches();
	}

}
