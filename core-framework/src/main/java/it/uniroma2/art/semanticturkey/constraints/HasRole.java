package it.uniroma2.art.semanticturkey.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.validators.RoleValidator;

/**
 * Requires that a resource has a given role
 * 
 * @author Manuel Fiorelli
 */
@Documented
@Constraint(validatedBy = RoleValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HasRole {
	String message() default "Error: resource " + MsgInterpolationVariables.invalidParamValuePlaceHolder
			+ " does not have the provided 'role'";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	RDFResourceRole value();

	boolean allowNarrowerRoles() default true;
}