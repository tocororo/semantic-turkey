package it.uniroma2.art.semanticturkey.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Utility class related to the AOP {@link MethodInvocation}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class MethodInvocationUtilities {
	/**
	 * Returns the value of the first parameter marked with the given annotation
	 * 
	 * @param invocation
	 * @param annotationType
	 * @param expectedParameterType
	 * @return
	 */
	public static <T> Optional<T> getValueOfFirstAnnotatedParameter(MethodInvocation invocation,
			Class<? extends Annotation> annotationType, Class<T> expectedParameterType) {
		Parameter[] params = invocation.getMethod().getParameters();
		for (int i = 0; i < params.length; i++) {
			Parameter par = params[i];
			if (par.isAnnotationPresent(annotationType)) {
				return Optional.of(expectedParameterType.cast(invocation.getArguments()[i]));
			}
		}

		return Optional.empty();
	}

	/**
	 * Returns first parameter marked with the given annotation. The return value is a pair consisting of the
	 * parameter value and the annotation.
	 * 
	 * @param invocation
	 * @param annotationType
	 * @param expectedParameterType
	 * @return
	 */
	public static <T, S extends Annotation> Optional<ImmutablePair<T, S>> getFirstAnnotatedArgument(
			MethodInvocation invocation, Class<S> annotationType, Class<T> expectedParameterType) {
		Parameter[] params = invocation.getMethod().getParameters();
		for (int i = 0; i < params.length; i++) {
			Parameter par = params[i];
			S annotation = par.getAnnotation(annotationType);
			if (annotation != null) {
				return Optional.of(new ImmutablePair<>(
						expectedParameterType.cast(invocation.getArguments()[i]), annotation));
			}
		}

		return Optional.empty();
	}
}
