package it.uniroma2.art.semanticturkey.mvc;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JBNodeDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JIRIDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JLiteralDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JResourceDeserializer;
import it.uniroma2.art.semanticturkey.properties.yaml.RDF4JValueDeserializer;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;

/**
 * A {@link HandlerMethodArgumentResolver} that uses Jackson's
 * {@link com.fasterxml.jackson.databind.ObjectMapper}, with priority with respect to default ones
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class JacksonMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private ObjectMapper objectMapper;
	private RequestParamMethodArgumentResolver delegate;

	public JacksonMethodArgumentResolver(ConfigurableBeanFactory beanFactory, boolean useDefaultResolution) {
		// super(beanFactory, useDefaultResolution);
		SimpleModule stdDeserializers = new SimpleModule();
		stdDeserializers.addDeserializer(Value.class, new RDF4JValueDeserializer());
		stdDeserializers.addDeserializer(Resource.class, new RDF4JResourceDeserializer());
		stdDeserializers.addDeserializer(BNode.class, new RDF4JBNodeDeserializer());
		stdDeserializers.addDeserializer(IRI.class, new RDF4JIRIDeserializer());
		stdDeserializers.addDeserializer(Literal.class, new RDF4JLiteralDeserializer());
		this.objectMapper = new ObjectMapper();
		objectMapper.registerModule(stdDeserializers);

		this.delegate = new RequestParamMethodArgumentResolver(beanFactory, false);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		/*
		 * A parameter is supported if it satisfied all the conditions below: 1. it is annotated with
		 * RequestParam 2. it is annotated with JsonSerialized
		 */
		return parameter.hasParameterAnnotation(RequestParam.class)
				&& parameter.hasParameterAnnotation(JsonSerialized.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		// String webArg = webRequest.getParameter(parameter.getParameterName());
		// if (webArg == null) {
		// return null;
		// }
		// System.out.println("Binder factory: " + binderFactory);
		// System.out.println("converting!!");
		//
		// return objectMapper.readValue(new StringReader(webArg),
		// objectMapper.getTypeFactory().constructType(parameter.getGenericParameterType()));

		return this.delegate.resolveArgument(parameter, mavContainer, webRequest, new WebDataBinderFactory() {

			@Override
			public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
					throws Exception {
				return new JacksonServletRequestDataBinder(target, objectName, objectMapper);
			}
		});
	}

	private static class JacksonServletRequestDataBinder extends ServletRequestDataBinder {

		private ObjectMapper objectMapper;

		public JacksonServletRequestDataBinder(Object target, String objectName, ObjectMapper objectMapper) {
			super(target, objectName);
			this.objectMapper = objectMapper;
		}

		public JacksonServletRequestDataBinder(Object target, ObjectMapper objectMapper) {
			super(target);
			this.objectMapper = objectMapper;
		}

		@Override
		public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
			return convertIfNecessaryInternal(value, requiredType);
		}

		@Override
		public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
				throws TypeMismatchException {
			return convertIfNecessaryInternal(value,
					MoreObjects.firstNonNull(field.getGenericType(), requiredType));
		}

		@Override
		public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
				throws TypeMismatchException {
			return convertIfNecessaryInternal(value,
					MoreObjects.firstNonNull(methodParam.getGenericParameterType(), requiredType));
		}

		public <T> T convertIfNecessaryInternal(Object value, Type requiredType)
				throws TypeMismatchException {
			if (value == null)
				return null;
			if (requiredType == null)
				throw new TypeMismatchException(value, (Class<?>) requiredType);

			try {
				return objectMapper.readValue(new StringReader((String) value),
						objectMapper.getTypeFactory().constructType(requiredType));
			} catch (IOException e) {
				throw new TypeMismatchException(value, TypeUtils.getRawType(requiredType, null));
			}
		}

	}
}
