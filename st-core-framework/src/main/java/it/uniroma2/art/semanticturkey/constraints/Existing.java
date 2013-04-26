
package it.uniroma2.art.semanticturkey.constraints;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import it.uniroma2.art.semanticturkey.validators.ExistingValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
 
import javax.validation.Constraint;
import javax.validation.Payload;

 
@Documented
@Constraint(validatedBy = ExistingValidator.class)
@Target( { METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Existing {
 String message() default "Error: resource " + MsgInterpolationVariables.invalidParamValuePlaceHolder + " does not exist in the current dataset";
 
 Class<?>[] groups() default {};
 
 Class<? extends Payload>[] payload() default {};
}