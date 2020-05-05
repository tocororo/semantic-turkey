package it.uniroma2.art.semanticturkey.user;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import it.uniroma2.art.semanticturkey.aop.STServiceOperationPointcut;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

@SuppressWarnings("serial")
public class SchemesOwnershipPostProcessor extends AbstractAdvisingBeanPostProcessor
		implements ApplicationContextAware, InitializingBean {
	
	private ApplicationContext applicationContext;
	
	public SchemesOwnershipPostProcessor() {}

	@Override
	public void afterPropertiesSet() throws Exception {
		Pointcut pointcut = new ComposablePointcut(new STServiceOperationPointcut())
				.intersection(new AnnotationMethodMatcher(Write.class));
		SchemesOwnershipCheckerInterceptor advice = applicationContext.getAutowireCapableBeanFactory()
				.createBean(SchemesOwnershipCheckerInterceptor.class);
		this.advisor = new DefaultPointcutAdvisor(pointcut, advice);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
