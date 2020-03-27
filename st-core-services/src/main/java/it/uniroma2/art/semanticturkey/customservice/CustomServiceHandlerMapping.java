package it.uniroma2.art.semanticturkey.customservice;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinition;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.config.customservice.OperationDefintion;
import it.uniroma2.art.semanticturkey.config.customservice.ParameterDefinition;
import it.uniroma2.art.semanticturkey.config.customservice.TypeDescription;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.ExtensionSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.Response;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

/**
 * A {@link HandlerMapping} that dispatches incoming requests to <em>custom services</a> (see
 * {@link CustomServiceDefinitionStore}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomServiceHandlerMapping extends AbstractHandlerMapping implements Ordered {

	public static final String CUSTOM_SERVICES_URL_PREFIX = "it.uniroma2.art.semanticturkey/st-custom-services/";

	@Autowired
	private ExtensionPointManager exptManager;

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private ConfigurableListableBeanFactory context;

	private ConcurrentHashMap<String, Object> customServiceHandlers = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() throws NoSuchConfigurationManager {
		registerStoredCustomServices();
	}

	protected void registerStoredCustomServices() throws NoSuchConfigurationManager {
		CustomServiceDefinitionStore cs = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());

		for (String customServiceCfgID : cs.getSystemConfigurationIdentifiers()) {
			try {
				CustomServiceDefinition customServiceCfg = cs.getSystemConfiguration(customServiceCfgID);
				registerCustomService(customServiceCfg);
			} catch (Exception e) {
				e.printStackTrace(); // store the exception somewhere to enable inspection
			}
		}

	}

	private void registerCustomService(CustomServiceDefinition customServiceCfg)
			throws InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, WrongPropertiesException, STPropertyAccessException,
			InvalidConfigurationException {
		// mimicking ordinary ST service, we have to build i) a Spring MVC controller, and ii) an @STService
		// object. The controller binds the request parameters and delegates the actual implementation to the
		// servuce

		Map<String, OperationDefintion> operationDefinitions = Optional
				.ofNullable(customServiceCfg.operations).orElse(Collections.emptyMap());

		// build the service class
		Builder<Object> serviceClassBuilder = new ByteBuddy().subclass(Object.class);
		serviceClassBuilder = serviceClassBuilder
				.annotateType(AnnotationDescription.Builder.ofType(STService.class).build())
				.defineField("stServiceContext", STServiceContext.class, Modifier.PUBLIC)
				.annotateField(AnnotationDescription.Builder.ofType(Autowired.class).build());

		for (Entry<String, OperationDefintion> operationEntry : operationDefinitions.entrySet()) {

			OperationDefintion operationDefinition = operationEntry.getValue();

			ExtensionSpecification operationImplConfig = operationDefinition.implementation;
			CustomServiceBackend customServiceBackend = exptManager.instantiateExtension(
					CustomServiceBackend.class, new PluginSpecification(operationImplConfig.getExtensionID(),
							null, null, operationImplConfig.getConfig()));
			InvocationHandler invocationHandler = customServiceBackend
					.createInvocationHandler(operationDefinition);
			boolean isWrite = customServiceBackend.isWrite();

			TypeDefinition returnTypeDefinition = generateTypeDefinitionFromSchema(
					operationEntry.getValue().returns);

			Initial<Object> methodBuilder = serviceClassBuilder.defineMethod(operationEntry.getKey(),
					returnTypeDefinition, Modifier.PUBLIC);

			Annotatable<Object> parameterBuilder = null;

			for (ParameterDefinition parameterDefinition : operationDefinition.parameters) {
				parameterBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder).withParameter(
						generateTypeDefinitionFromSchema(parameterDefinition.type), parameterDefinition.name);
			}
			serviceClassBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
					.intercept(InvocationHandlerAdapter.of(invocationHandler))
					.annotateMethod(AnnotationDescription.Builder.ofType(STServiceOperation.class)
							.define("method", isWrite ? RequestMethod.POST : RequestMethod.GET).build(),
							AnnotationDescription.Builder.ofType(isWrite ? Read.class : Write.class).build());
		}

		Class<?> serviceClass = serviceClassBuilder.make().load(getClass().getClassLoader()).getLoaded();

		// build a service object using the Spring context to enable dependency injection (e.g. of the service
		// context), aspects, etc.
		Object service = context.createBean(serviceClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

		// build the controller class
		Builder<Object> controllerClassBuilder = new ByteBuddy().subclass(Object.class);
		controllerClassBuilder = controllerClassBuilder
				.annotateType(AnnotationDescription.Builder.ofType(Controller.class).build());

		for (Entry<String, OperationDefintion> operationEntry : operationDefinitions.entrySet()) {

			OperationDefintion operationDefinition = operationEntry.getValue();
			ExtensionSpecification operationImplConfig = operationDefinition.implementation;
			CustomServiceBackend customServiceBackend = exptManager.instantiateExtension(
					CustomServiceBackend.class, new PluginSpecification(operationImplConfig.getExtensionID(),
							null, null, operationImplConfig.getConfig()));
			boolean isWrite = customServiceBackend.isWrite();

			TypeDefinition returnTypeDefinition = generateTypeDefinitionFromSchema(
					operationEntry.getValue().returns);

			Initial<Object> methodBuilder = controllerClassBuilder
					.defineMethod(operationEntry.getKey(),
							net.bytebuddy.description.type.TypeDescription.Generic.Builder.parameterizedType(
									net.bytebuddy.description.type.TypeDescription.Generic.Builder
											.rawType(Response.class).build().asErasure(),
									returnTypeDefinition).build(),
							Modifier.PUBLIC);

			Annotatable<Object> parameterBuilder = null;

			for (ParameterDefinition parameterDefinition : operationDefinition.parameters) {
				parameterBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
						.withParameter(generateTypeDefinitionFromSchema(parameterDefinition.type),
								parameterDefinition.name)
						.annotateParameter(AnnotationDescription.Builder.ofType(RequestParam.class)
								.define("value", parameterDefinition.name)
								.define("required", parameterDefinition.required).build());
			}

			controllerClassBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
					.intercept(InvocationHandlerAdapter.of(new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Method m = Arrays.stream(service.getClass().getMethods())
									.filter(method2 -> method2.getName().equals(operationEntry.getKey()))
									.findAny().get();
							Object out = m.invoke(service, args);
							return new Response<>(out);
						}
					}))
					.annotateMethod(AnnotationDescription.Builder.ofType(RequestMapping.class)
							.defineArray("value",
									CUSTOM_SERVICES_URL_PREFIX + customServiceCfg.name + "/"
											+ operationEntry.getKey())
							.defineEnumerationArray("method",
									org.springframework.web.bind.annotation.RequestMethod.class,
									isWrite ? org.springframework.web.bind.annotation.RequestMethod.POST
											: org.springframework.web.bind.annotation.RequestMethod.GET)
							.defineArray("produces", "application/json").build())
					.annotateMethod(AnnotationDescription.Builder.ofType(ResponseBody.class).build());

		}

		Class<? extends Object> controllerClass = controllerClassBuilder.make()
				.load(getClass().getClassLoader()).getLoaded();

		// build a controller object
		Object controller = controllerClass.newInstance();

		customServiceHandlers.put(customServiceCfg.name, controller);
	}

	protected TypeDefinition generateTypeDefinitionFromSchema(TypeDescription typeDescription)
			throws SchemaException {
		if ("AnnotatedValue".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder
					.parameterizedType(AnnotatedValue.class, Value.class).build();
		} else if ("java.lang.String".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.STRING;
		} else if ("boolean".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder.rawType(Boolean.class)
					.build();
		} else if ("List".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder
					.parameterizedType(TypeDefinition.Sort.describe(List.class).asErasure(),
							typeDescription.getTypeArguments().stream()
									.map(this::generateTypeDefinitionFromSchema).collect(Collectors.toList()))
					.build();
		} else if ("IRI".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder.rawType(IRI.class).build();
		} else if ("Literal".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder.rawType(Literal.class)
					.build();
		} else if ("BNode".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder.rawType(BNode.class)
					.build();
		} else if ("RDFValue".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.Generic.Builder.rawType(Value.class)
					.build();
		} else {
			throw new SchemaException("Unknown type '" + typeDescription.getName());
		}
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		Pattern pattern = Pattern.compile("^" + Pattern.quote(request.getContextPath()) + "/"
				+ Pattern.quote(CUSTOM_SERVICES_URL_PREFIX) + "(?<serviceName>.*)/(?<operationName>.*)$");
		Matcher m = pattern.matcher(request.getRequestURI());

		if (m.find()) {
			String serviceName = m.group("serviceName");
			String operationName = m.group("operationName");

			Object handler = customServiceHandlers.get(serviceName);
			if (handler != null) {
				Optional<Method> handlerMethod = Arrays.stream(handler.getClass().getMethods())
						.filter(method -> method.getName().equals(operationName)).findAny();
				if (handlerMethod.isPresent()) {
					return new HandlerMethod(handler, handlerMethod.get());
				}
			}

		}
		return null;
	}

}
