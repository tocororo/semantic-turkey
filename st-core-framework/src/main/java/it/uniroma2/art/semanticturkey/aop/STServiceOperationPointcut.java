package it.uniroma2.art.semanticturkey.aop;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.RootClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * A {@link Pointcut} implementation matching Semantic Turkey service operations.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STServiceOperationPointcut implements Pointcut {

	@Override
	public ClassFilter getClassFilter() {
		return new RootClassFilter(STServiceAdapter.class);
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return new AnnotationMethodMatcher(STServiceOperation.class);
	}

}
