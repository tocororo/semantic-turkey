package it.uniroma2.art.semanticturkey.mvc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.springframework.aop.support.AopUtils;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.json.PairSerializer;
import it.uniroma2.art.semanticturkey.json.TripleSerializer;
import it.uniroma2.art.semanticturkey.json.TupleQueryResultSerializer;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesSerializer;
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

	public RequestMappingHandlerAdapterPostProcessor() {
	}

	public static ObjectMapper createObjectMapper() {
		return createObjectMapper(null);
	}

	public static ObjectMapper createObjectMapper(ExtensionPointManager exptManager) {
		SimpleModule customTypeHandlers = new SimpleModule();
		customTypeHandlers.addDeserializer(Value.class, new RDF4JValueDeserializer());
		customTypeHandlers.addDeserializer(Resource.class, new RDF4JResourceDeserializer());
		customTypeHandlers.addDeserializer(BNode.class, new RDF4JBNodeDeserializer());
		customTypeHandlers.addDeserializer(IRI.class, new RDF4JIRIDeserializer());
		customTypeHandlers.addDeserializer(Literal.class, new RDF4JLiteralDeserializer());
		customTypeHandlers.addSerializer(Value.class, new RDF4JValueSerializer());
		customTypeHandlers.addSerializer(new PairSerializer());
		customTypeHandlers.addSerializer(new TripleSerializer());
		customTypeHandlers.addSerializer(new TupleQueryResultSerializer());
		customTypeHandlers.addSerializer(STProperties.class, new STPropertiesSerializer(null, exptManager));
		ObjectMapper newObjectMapper = new ObjectMapper();
		newObjectMapper.registerModule(customTypeHandlers);
		newObjectMapper.registerModule(new Jdk8Module());
		newObjectMapper.registerModule(new JavaTimeModule());

		return newObjectMapper;
	}

	@Autowired
	private ConfigurableBeanFactory beanFactory;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RequestMappingHandlerAdapter) {
			RequestMappingHandlerAdapter requestMappingHandlerAdapter = (RequestMappingHandlerAdapter) bean;

			MappingJackson2HttpMessageConverter jacksonMessageConverter = new MappingJackson2HttpMessageConverter();
			jacksonMessageConverter
					.setObjectMapper(createObjectMapper(beanFactory.getBean(ExtensionPointManager.class)));

			List<HttpMessageConverter<?>> originalMessageConverters = requestMappingHandlerAdapter
					.getMessageConverters();
			List<HttpMessageConverter<?>> enrichedMessageConverterList = new ArrayList<>(
					originalMessageConverters.size() + 1);

			boolean replaced = false;
			for (HttpMessageConverter<?> msgConv : originalMessageConverters) {
				if (AopUtils.getTargetClass(msgConv).equals(MappingJackson2HttpMessageConverter.class)) {
					if (!replaced) {
						enrichedMessageConverterList.add(jacksonMessageConverter);
						replaced = true;
					}
				} else {
					enrichedMessageConverterList.add(msgConv);
				}
			}

			if (!replaced) {
				enrichedMessageConverterList.add(jacksonMessageConverter);
			}

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
			resolvers.add(new JacksonMethodArgumentResolver(beanFactory, false,
					createObjectMapper(beanFactory.getBean(ExtensionPointManager.class))));
			resolvers.addAll((requestMappingHandlerAdapter.getArgumentResolvers()).getResolvers());

			requestMappingHandlerAdapter.setArgumentResolvers(resolvers);
			return bean;
		} else {
			return bean;
		}
	}

}
