package it.uniroma2.art.semanticturkey.services.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * It indicates that a parameter of an ST service may be omitted in an HTTP request to that service. As in
 * Java all parameters must be assigned with arguments in a method invocation, the value of an omitted
 * parameter is:
 * <ul>
 * <li>if {@link #defaultValue()} is equal to the empty string, the value obtained converting it to the annotated
 * parameter type</li>
 * <li>otherwise, the default value for that type according to the rules for the default initialization of
 * Java fields (<a
 * href="http://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html">http://docs.oracle
 * .com/javase/tutorial/java/nutsandbolts/datatypes.html</a>}</li>
 * </ul>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional {

	String defaultValue() default "";

}
