package it.uniroma2.art.semanticturkey.mvc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * A {@link BeanPostProcessor} that reconfigures the {@link RequestMappingHandlerAdapter} bean created by the
 * tag {@code <mvc:annotation-driver>}. Currently, it performs the following:
 * <ul>
 * <li>registers a {@link HandlerMethodArgumentResolver} that uses Jackson's
 * {@link com.fasterxml.jackson.databind.ObjectMapper}, with priority with respect to default ones</li>
 * </ul>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Component
public class RequestMappingHandlerAdapterPostProcessor implements BeanPostProcessor {

	@Autowired
	private ConfigurableBeanFactory beanFactory;
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RequestMappingHandlerAdapter) {
			RequestMappingHandlerAdapter requestMappingHandlerAdapter = (RequestMappingHandlerAdapter) bean;

			List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
			resolvers.add(new JacksonMethodArgumentResolver(beanFactory, false));
			resolvers.addAll((requestMappingHandlerAdapter.getArgumentResolvers()).getResolvers());

			requestMappingHandlerAdapter.setArgumentResolvers(resolvers);

			return bean;
		} else {
			return bean;
		}
	}

}
