package it.uniroma2.art.semanticturkey.mvc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import it.uniroma2.art.semanticturkey.json.PairSerializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JBNodeDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JIRIDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JLiteralDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JResourceDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueSerializer;

/**
 * A {@link BeanPostProcessor} that reconfigures the {@link RequestMappingHandlerAdapter} bean created by the
 * tag {@code <mvc:annotation-driver>}. Currently, it performs the following:
 * <ul>
 * <li>registers a {@link HandlerMethodArgumentResolver} that uses Jackson's
 * {@link com.fasterxml.jackson.databind.ObjectMapper}, with priority with respect to default ones</li>
 * <li>registers an instance of MessageCon
 * </ul>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Component
public class RequestMappingHandlerAdapterPostProcessor implements BeanPostProcessor {

	private ObjectMapper objectMapper;

	public RequestMappingHandlerAdapterPostProcessor() {
		SimpleModule customTypeHandlers = new SimpleModule();
		customTypeHandlers.addDeserializer(Value.class, new RDF4JValueDeserializer());
		customTypeHandlers.addDeserializer(Resource.class, new RDF4JResourceDeserializer());
		customTypeHandlers.addDeserializer(BNode.class, new RDF4JBNodeDeserializer());
		customTypeHandlers.addDeserializer(IRI.class, new RDF4JIRIDeserializer());
		customTypeHandlers.addDeserializer(Literal.class, new RDF4JLiteralDeserializer());
		customTypeHandlers.addSerializer(Value.class, new RDF4JValueSerializer());
		customTypeHandlers.addSerializer(new PairSerializer());

		ObjectMapper newObjectMapper = new ObjectMapper();
		newObjectMapper.registerModule(customTypeHandlers);
		newObjectMapper.registerModule(new Jdk8Module());
		this.objectMapper = newObjectMapper;

	}

	@Autowired
	private ConfigurableBeanFactory beanFactory;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RequestMappingHandlerAdapter) {
			RequestMappingHandlerAdapter requestMappingHandlerAdapter = (RequestMappingHandlerAdapter) bean;

			MappingJackson2HttpMessageConverter jacksonMessageConverter = new MappingJackson2HttpMessageConverter();
			jacksonMessageConverter.setObjectMapper(objectMapper);

			List<HttpMessageConverter<?>> enrichedMessageConverterList = new ArrayList<>(
					requestMappingHandlerAdapter.getMessageConverters().size() + 1);
			enrichedMessageConverterList.add(jacksonMessageConverter);
			enrichedMessageConverterList.addAll(requestMappingHandlerAdapter.getMessageConverters());
			requestMappingHandlerAdapter.setMessageConverters(enrichedMessageConverterList);
			return bean;
		} else {
			return bean;
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if (bean instanceof RequestMappingHandlerAdapter) {
			RequestMappingHandlerAdapter requestMappingHandlerAdapter = (RequestMappingHandlerAdapter) bean;

			List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
			resolvers.add(new JacksonMethodArgumentResolver(beanFactory, false, objectMapper));
			resolvers.addAll((requestMappingHandlerAdapter.getArgumentResolvers()).getResolvers());

			requestMappingHandlerAdapter.setArgumentResolvers(resolvers);
			return bean;
		} else {
			return bean;
		}
	}

}
