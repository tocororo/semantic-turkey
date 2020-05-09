package it.uniroma2.art.semanticturkey.services.aspects;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import it.uniroma2.art.semanticturkey.aop.STServiceOperationPointcut;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * A convenient {@link BeanPostProcessor} implementation that advises eligible beans (i.e. Semantic Turkey
 * services) in order to manage resource-level change metadata.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@SuppressWarnings("serial")
public class ResourceLevelChangeMetadataPostProcessor extends AbstractAdvisingBeanPostProcessor
		implements ApplicationContextAware, InitializingBean {
	private ApplicationContext applicationContext;

	public ResourceLevelChangeMetadataPostProcessor() {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Pointcut pointcut = new ComposablePointcut(new STServiceOperationPointcut())
				.intersection(new AnnotationMethodMatcher(Write.class));
		ResourceLevelChangeMetadataInterceptor advice = applicationContext.getAutowireCapableBeanFactory()
				.createBean(ResourceLevelChangeMetadataInterceptor.class);
		this.advisor = new DefaultPointcutAdvisor(pointcut, advice);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
