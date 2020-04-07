package it.uniroma2.art.semanticturkey.customservice;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.google.common.base.MoreObjects;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.customservice.CustomService;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.config.customservice.Parameter;
import it.uniroma2.art.semanticturkey.config.customservice.Type;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql.SPARQLCustomServiceBackend;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
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
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
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

	class DelegateRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

		protected void initHandlerMethods() {

			for (Object h : customServiceHandlers.values()) {
				detectHandlerMethods(h);
			}

			handlerMethodsInitialized(getHandlerMethods());
		};

		@Override
		public HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
			return super.getHandlerInternal(request);
		}

	};

	private DelegateRequestMappingHandlerMapping delegateMapping = new DelegateRequestMappingHandlerMapping();

	@PostConstruct
	public void init() throws NoSuchConfigurationManager {
		registerStoredCustomServices();
		delegateMapping.afterPropertiesSet();
	}

	protected void registerStoredCustomServices() throws NoSuchConfigurationManager {
		CustomServiceDefinitionStore cs = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());

		for (String customServiceCfgID : cs.getSystemConfigurationIdentifiers()) {
			try {
				CustomService customServiceCfg = cs.getSystemConfiguration(customServiceCfgID);
				registerCustomService(customServiceCfg);
			} catch (Exception e) {
				e.printStackTrace(); // store the exception somewhere to enable inspection
			}
		}

	}

	/*
	 * @formatter:off
	 * Pattern for the recognition of authorizations.
	 * These are the named capturing group:
	 * - "area": matches the area (restricted to letters)
	 * - "subjfun": matches the subject as a function invocation; "subjfunpar" is the parameter
	 * - "subjlit": matches the subject as a a literal (exclusive with the previous)
	 * - "scope": optionally, matches the scope as a literal (restricted to letters)
	 * - "userkey": optionally, matches a key in the userResponsibility structure (restricted to letters)
	 * - "userlit": optionally, the value associated with the key (as word characters)
	 * - "userfun": optionally, the value associated with the key as a function invocation (exclusive with the previous); "usefunpar" is the parameter
	 * - "crudv": the operation in the CRUDV model
	 * 
	 * The pattern matches unquoted authorizations:
	 *  rdf(@typeof(#root),lexicalizations), {lang: @langOf}, R
	 *  
	 *  quotes should be added:
	 *  
	 *  'rdf(' + @auth.typeof(#root) + ',lexicalizations)', '{lang: ''' + @auth.langOf(#label) + '''}', 'R'
	 * @formatter:on
	 */
	private Pattern AUTHORIZATION_PATTERN = Pattern.compile(
			"^\\s*(?<area>[a-z]+)\\(((?<subjfun>@typeof\\(#(?<subjfunpar>[a-z]+)\\))|(?<subjlit>[a-z]+))(\\s*,\\s*(?<scope>\\w+))?\\)(\\s*,\\s*\\{\\s*(?<userkey>[a-z]+)\\s*:\\s*((?<userlit>\\w+)|(?<userfun>(@langOf\\(#(?<userfunpar>[a-z]+)\\))))\\})?\\s*,\\s*(?<crudv>[CRUDV])\\s*$",
			Pattern.CASE_INSENSITIVE);

	private void registerCustomService(CustomService customServiceCfg)
			throws InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, WrongPropertiesException, STPropertyAccessException,
			InvalidConfigurationException {
		// mimicking ordinary ST service, we have to build i) a Spring MVC controller, and ii) an @STService
		// object. The controller binds the request parameters and delegates the actual implementation to the
		// service

		List<Operation> operationDefinitions = Optional.ofNullable(customServiceCfg.operations)
				.orElse(Collections.emptyList());

		// build the service class
		Builder<Object> serviceClassBuilder = new ByteBuddy().subclass(Object.class);
		serviceClassBuilder = serviceClassBuilder
				.annotateType(AnnotationDescription.Builder.ofType(STService.class).build())
				.defineField("stServiceContext", STServiceContext.class, Modifier.PUBLIC)
				.annotateField(AnnotationDescription.Builder.ofType(Autowired.class).build());

		for (Operation operationDefinition : operationDefinitions) {

			CustomServiceBackend customServiceBackend = exptManager.instantiateExtension(
					CustomServiceBackend.class, new PluginSpecification(SPARQLCustomServiceBackend.class.getName(),
							null, null, STPropertiesManager.storeSTPropertiesToObjectNode(operationDefinition, true)));
			InvocationHandler invocationHandler = customServiceBackend
					.createInvocationHandler();
			boolean isWrite = customServiceBackend.isWrite();

			TypeDefinition returnTypeDefinition = generateTypeDefinitionFromSchema(
					operationDefinition.returns);

			if (isWrite) {
				if (!returnTypeDefinition.equals(net.bytebuddy.description.type.TypeDescription.VOID)) {
					throw new RuntimeException("Mutation operations may not return a value");
				}
			}

			Initial<Object> methodBuilder = serviceClassBuilder.defineMethod(operationDefinition.name,
					returnTypeDefinition, Modifier.PUBLIC);

			Annotatable<Object> parameterBuilder = null;

			for (Parameter parameterDefinition : operationDefinition.parameters) {
				parameterBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder).withParameter(
						generateTypeDefinitionFromSchema(parameterDefinition.type), parameterDefinition.name);
			}
			serviceClassBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
					.intercept(InvocationHandlerAdapter.of(invocationHandler))
					.annotateMethod(AnnotationDescription.Builder.ofType(STServiceOperation.class)
							.define("method", isWrite ? RequestMethod.POST : RequestMethod.GET).build(),
							AnnotationDescription.Builder.ofType(isWrite ? Write.class : Read.class).build());
		}

		Class<?> serviceClass = serviceClassBuilder.make().load(getClass().getClassLoader()).getLoaded();

		// build a service object using the Spring context to enable dependency injection (e.g. of the service
		// context), aspects, etc.
		Object service = context.createBean(serviceClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

		// build the controller class
		Builder<Object> controllerClassBuilder = new ByteBuddy().subclass(Object.class);
		controllerClassBuilder = controllerClassBuilder
				.annotateType(AnnotationDescription.Builder.ofType(Controller.class).build());

		for (Operation operationDefinition : operationDefinitions) {

			CustomServiceBackend customServiceBackend = exptManager.instantiateExtension(
					CustomServiceBackend.class, new PluginSpecification(SPARQLCustomServiceBackend.class.getName(),
							null, null, STPropertiesManager.storeSTPropertiesToObjectNode(operationDefinition, true)));
			boolean isWrite = customServiceBackend.isWrite();

			TypeDefinition returnTypeDefinition = generateTypeDefinitionFromSchema(
					operationDefinition.returns);

			Initial<Object> methodBuilder = controllerClassBuilder.defineMethod(operationDefinition.name,
					isWrite ? net.bytebuddy.description.type.TypeDescription.Generic.Builder
							.parameterizedType(HttpEntity.class, String.class).build()
							: net.bytebuddy.description.type.TypeDescription.Generic.Builder
									.parameterizedType(
											net.bytebuddy.description.type.TypeDescription.Generic.Builder
													.rawType(Response.class).build().asErasure(),
											returnTypeDefinition)
									.build(),
					Modifier.PUBLIC);

			Annotatable<Object> parameterBuilder = null;

			for (Parameter parameterDefinition : operationDefinition.parameters) {
				parameterBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
						.withParameter(generateTypeDefinitionFromSchema(parameterDefinition.type),
								parameterDefinition.name)
						.annotateParameter(AnnotationDescription.Builder.ofType(RequestParam.class)
								.define("value", parameterDefinition.name)
								.define("required", parameterDefinition.required).build());
			}

			String preauthorizeValue;

			if (operationDefinition.authorization != null) {
				Matcher m = AUTHORIZATION_PATTERN.matcher(operationDefinition.authorization);
				if (!m.find()) {
					throw new RuntimeException("Invalid authorization string");
				}

				StringBuilder sb = new StringBuilder();
				sb.append("@auth.isAuthorized(");
				sb.append("'");
				sb.append(m.group("area"));
				if (m.group("subjlit") != null) {
					sb.append(m.group("subjlit"));
				} else {
					sb.append("' +");
					sb.append("@auth." + m.group("subjfun").substring(1)); // removes leading @
					sb.append("+ '");
				}
				if (m.group("scope") != null) {
					sb.append(", ").append(m.group("scope"));
				}
				sb.append(")");
				sb.append("'");
				if (m.group("userkey") != null) {
					sb.append(", '{" + m.group("userkey") + ": ");
					if (m.group("userlit") != null) {
						sb.append("''").append(m.group("userlit")).append("''");
					} else {
						sb.append("'''").append(" + @auth." + m.group("userfun").substring(1))
								.append(" + '''");
					}
					sb.append("}'");
				}
				sb.append(", '").append(m.group("crudv")).append("'");
				sb.append(")");
				preauthorizeValue = sb.toString();
			} else {
				preauthorizeValue = "@auth.isAuthorized(true)";
			}
			controllerClassBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
					.intercept(InvocationHandlerAdapter.of(new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Method m = Arrays.stream(service.getClass().getMethods())
									.filter(method2 -> method2.getName().equals(operationDefinition.name))
									.findAny().get();
							Object out = m.invoke(service, args);
							if (isWrite) {
								return new HttpEntity<>(ServletUtilities.getService()
										.createReplyResponse(operationDefinition.name, RepliesStatus.ok,
												SerializationType.json)
										.getResponseContent(), new HttpHeaders());
							} else {
								return new Response<>(out);
							}
						}
					}))
					.annotateMethod(AnnotationDescription.Builder.ofType(RequestMapping.class)
							.defineArray("value",
									CUSTOM_SERVICES_URL_PREFIX + customServiceCfg.name + "/"
											+ operationDefinition.name)
							.defineEnumerationArray("method",
									org.springframework.web.bind.annotation.RequestMethod.class,
									isWrite ? org.springframework.web.bind.annotation.RequestMethod.POST
											: org.springframework.web.bind.annotation.RequestMethod.GET)
							.defineArray("produces", "application/json;charset=UTF-8").build())
					.annotateMethod(AnnotationDescription.Builder.ofType(ResponseBody.class).build(),
							AnnotationDescription.Builder.ofType(PreAuthorize.class)
									.define("value", preauthorizeValue).build());
		}

		Class<? extends Object> controllerClass = controllerClassBuilder.make()
				.load(getClass().getClassLoader()).getLoaded();

		// build a controller object
		Object controller = controllerClass.newInstance();

		customServiceHandlers.put(customServiceCfg.name, controller);
	}

	protected TypeDefinition generateTypeDefinitionFromSchema(Type typeDescription)
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
		} else if ("void".equals(typeDescription.getName())) {
			return net.bytebuddy.description.type.TypeDescription.VOID;
		} else {
			throw new SchemaException("Unknown type '" + typeDescription.getName());
		}
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		return delegateMapping.getHandlerInternal(request);
	}
}
