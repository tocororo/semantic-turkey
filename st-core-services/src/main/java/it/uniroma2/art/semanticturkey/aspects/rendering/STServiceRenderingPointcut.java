package it.uniroma2.art.semanticturkey.aspects.rendering;

import it.uniroma2.art.semanticturkey.services.STService;
import it.uniroma2.art.semanticturkey.services.annotations.AutoRendering;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.RootClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;

public class STServiceRenderingPointcut implements Pointcut {

	@Override
	public ClassFilter getClassFilter() {
		return new RootClassFilter(STService.class);
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return new AnnotationMethodMatcher(AutoRendering.class);
	}
	
}
