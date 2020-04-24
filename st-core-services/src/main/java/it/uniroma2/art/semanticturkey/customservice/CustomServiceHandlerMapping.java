package it.uniroma2.art.semanticturkey.customservice;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.customservice.CustomService;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.config.customservice.Parameter;
import it.uniroma2.art.semanticturkey.config.customservice.Type;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
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

	private static final Logger logger = LoggerFactory.getLogger(CustomServiceHandlerMapping.class);

	public static final String CUSTOM_SERVICES_URL_PREFIX = "it.uniroma2.art.semanticturkey/st-custom-services/";

	@Autowired
	private ExtensionPointManager exptManager;

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private ConfigurableListableBeanFactory context;

	static class DelegateRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

		private List<Object> handlers;

		public DelegateRequestMappingHandlerMapping(List<Object> handlers) {
			this.handlers = handlers;
		}

		protected void initHandlerMethods() {

			for (Object h : this.handlers) {
				detectHandlerMethods(h);
			}

			handlerMethodsInitialized(getHandlerMethods());
		};

		@Override
		public HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
			return super.getHandlerInternal(request);
		}

	};

	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private DelegateRequestMappingHandlerMapping delegateMapping;
	private Map<String, Object> customServiceHandlers = new HashMap<>();
	private Multimap<String, String> customServiceNames2IDs = HashMultimap.create();

	@PostConstruct
	public void init() throws NoSuchConfigurationManager {
		initializeFromStoredCustomServices();
	}

	public void initializeFromStoredCustomServices() throws NoSuchConfigurationManager {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {
			CustomServiceDefinitionStore cs = (CustomServiceDefinitionStore) exptManager
					.getConfigurationManager(CustomServiceDefinitionStore.class.getName());

			// temporary multimap to spot conflicting services
			customServiceNames2IDs = HashMultimap.create();

			for (String customServiceCfgID : cs.getSystemConfigurationIdentifiers()) {
				try {
					CustomService customServiceCfg = cs.getSystemConfiguration(customServiceCfgID);
					Object handler = buildCustomServiceHandler(customServiceCfgID, customServiceCfg);
					customServiceHandlers.put(customServiceCfgID, handler);
					customServiceNames2IDs.put(customServiceCfg.name, customServiceCfgID);
				} catch (Exception e) {
					logger.error(" exception occurred when parsing the custom service: " + customServiceCfgID,
							e);
				}
			}

			Iterator<Entry<String, Collection<String>>> it = customServiceNames2IDs.asMap().entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<String, Collection<String>> serviceName2IDs = it.next();
				String serviceName = serviceName2IDs.getKey();
				Collection<String> serviceIDs = serviceName2IDs.getValue();

				if (serviceIDs.size() > 1) {
					logger.error("Custom services {} use the same service name {}. Not loaded.", serviceIDs,
							serviceName);
				}
			}

			setDelegateMappingInternal();
		} finally {
			wlock.unlock();
		}
	}

	public Collection<String> getCustomServiceIdentifiers() {
		CustomServiceDefinitionStore cs;
		try {
			cs = (CustomServiceDefinitionStore) exptManager
					.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		} catch (NoSuchConfigurationManager e) {
			throw new IllegalStateException("Unable to find the configuration manager for custom services",
					e);
		}
		return cs.getSystemConfigurationIdentifiers();
	}

	public CustomService getCustomService(String id) throws IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException {
		CustomServiceDefinitionStore cs;
		try {
			cs = (CustomServiceDefinitionStore) exptManager
					.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		} catch (NoSuchConfigurationManager e) {
			throw new IllegalStateException("Unable to find the configuration manager for custom services",
					e);
		}
		return cs.getSystemConfiguration(id);
	}

	public void registerCustomService(String customServiceCfgID)
			throws DuplicateIdException, STPropertyAccessException, IOException, WrongPropertiesException,
			STPropertyUpdateException, InstantiationException, IllegalAccessException, SchemaException,
			IllegalArgumentException, NoSuchExtensionException, InvalidConfigurationException,
			ConfigurationNotFoundException, DuplicateName {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {
			CustomServiceDefinitionStore cs = (CustomServiceDefinitionStore) exptManager
					.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
			CustomService customServiceCfg = cs.getSystemConfiguration(customServiceCfgID);

			// build
			Object handler = buildCustomServiceHandler(customServiceCfgID, customServiceCfg);

			// checks uniqueness of custom service name
			Collection<String> conflictingCustomServiceIDs = customServiceNames2IDs.get(customServiceCfg.name)
					.stream().filter(id -> !id.equals(customServiceCfgID)).collect(Collectors.toList());
			if (!conflictingCustomServiceIDs.isEmpty()) {
				throw new DuplicateName("the custom service '" + customServiceCfgID
						+ "' uses the service name '" + customServiceCfg.name
						+ "' already used by the custom service " + conflictingCustomServiceIDs);
			}

			// then, finalize the registration (which is unlikely to fail)
			customServiceHandlers.put(customServiceCfgID, handler);
			setDelegateMappingInternal();
		} catch (NoSuchConfigurationManager e) {
			throw new IllegalStateException("Unable to find the configuration manager for custom services",
					e);
		} finally {
			wlock.unlock();
		}
	}

	public void registerCustomService(String customServiceCfgID, ObjectNode customServiceDefinitiondefinition,
			boolean overwrite) throws STPropertyAccessException, IOException, WrongPropertiesException,
			STPropertyUpdateException, InstantiationException, IllegalAccessException, SchemaException,
			IllegalArgumentException, NoSuchExtensionException, InvalidConfigurationException,
			CustomServiceException {

		// parses the JSON object into an actual Configuration object
		CustomService customServiceCfg = STPropertiesManager
				.loadSTPropertiesFromObjectNode(CustomService.class, true, customServiceDefinitiondefinition);

		registerCustomService(customServiceCfgID, customServiceCfg, overwrite);
	}

	public void registerCustomService(String customServiceCfgID, CustomService customServiceCfg,
			boolean overwrite) throws DuplicateIdException, STPropertyAccessException, IOException,
			WrongPropertiesException, STPropertyUpdateException, InstantiationException,
			IllegalAccessException, SchemaException, IllegalArgumentException, NoSuchExtensionException,
			InvalidConfigurationException, CustomServiceException {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {
			CustomServiceDefinitionStore cs = (CustomServiceDefinitionStore) exptManager
					.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
			// raises an exception if a custom service with the same configuration id exists and overwriting
			// is disabled
			if (!overwrite && cs.getSystemConfigurationIdentifiers().contains(customServiceCfgID)) {
				throw new DuplicateIdException(
						"A CustomService definition with id '" + customServiceCfgID + "' already exists");
			}

			// build
			Object handler = buildCustomServiceHandler(customServiceCfgID, customServiceCfg);

			// checks uniqueness of custom service name
			Collection<String> conflictingCustomServiceIDs = customServiceNames2IDs.get(customServiceCfg.name)
					.stream().filter(id -> !id.equals(customServiceCfgID)).collect(Collectors.toList());
			if (!conflictingCustomServiceIDs.isEmpty()) {
				throw new DuplicateName("the custom service '" + customServiceCfgID
						+ "' uses the service name '" + customServiceCfg.name
						+ "' already used by the custom service " + conflictingCustomServiceIDs);
			}

			// first, try to store the configuration
			cs.storeSystemConfiguration(customServiceCfgID, customServiceCfg);

			// then, finalize the registration (which is unlikely to fail)
			customServiceHandlers.put(customServiceCfgID, handler);
			customServiceNames2IDs.put(customServiceCfg.name, customServiceCfgID);

			setDelegateMappingInternal();
		} catch (NoSuchConfigurationManager e) {
			throw new IllegalStateException("Unable to find the configuration manager for custom services",
					e);
		} finally {
			wlock.unlock();
		}
	}

	public void unregisterCustomService(String customServiceCfgID) throws ConfigurationNotFoundException {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {
			CustomServiceDefinitionStore cs;
			try {
				cs = (CustomServiceDefinitionStore) exptManager
						.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
			} catch (NoSuchConfigurationManager e) {
				throw new IllegalStateException(
						"Unable to find the configuration manager for custom services", e);
			}
			cs.deleteSystemConfiguration(customServiceCfgID);

			Object removedMapping = customServiceHandlers.remove(customServiceCfgID);
			customServiceNames2IDs.values().remove(customServiceCfgID);

			if (removedMapping != null) {
				setDelegateMappingInternal();
			}
		} finally {
			wlock.unlock();
		}
	}

	public void addOperationToCustomeService(String id, ObjectNode operationDefinition)
			throws STPropertyAccessException, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, InstantiationException, IllegalAccessException, SchemaException,
			IllegalArgumentException, NoSuchExtensionException, STPropertyUpdateException,
			InvalidConfigurationException, CustomServiceException {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {

			Operation op = STPropertiesManager.loadSTPropertiesFromObjectNode(Operation.class, true,
					operationDefinition);

			CustomServiceDefinitionStore cs;
			try {
				cs = (CustomServiceDefinitionStore) exptManager
						.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
			} catch (NoSuchConfigurationManager e) {
				throw new IllegalStateException(
						"Unable to find the configuration manager for custom services", e);
			}

			CustomService customService = cs.getSystemConfiguration(id);

			List<Operation> oldOps = customService.operations;
			if (oldOps != null) {
				customService.operations = new ArrayList<>(oldOps.size() + 1);
				customService.operations.addAll(oldOps);
				customService.operations.add(op);
			} else {
				customService.operations = Lists.newArrayList(op);
			}

			registerCustomService(id, customService, true);
		} finally {
			wlock.unlock();
		}
	}

	public void udpateOperationInCustomeService(String id, ObjectNode operationDefinition,
			String oldOperationName) throws STPropertyAccessException, IOException,
			ConfigurationNotFoundException, WrongPropertiesException, InstantiationException,
			IllegalAccessException, SchemaException, IllegalArgumentException, NoSuchExtensionException,
			STPropertyUpdateException, InvalidConfigurationException, CustomServiceException {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {

			Operation op = STPropertiesManager.loadSTPropertiesFromObjectNode(Operation.class, true,
					operationDefinition);

			CustomServiceDefinitionStore cs;
			try {
				cs = (CustomServiceDefinitionStore) exptManager
						.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
			} catch (NoSuchConfigurationManager e) {
				throw new IllegalStateException(
						"Unable to find the configuration manager for custom services", e);
			}

			CustomService customService = cs.getSystemConfiguration(id);
			String op2replace = oldOperationName != null ? oldOperationName : op.name;

			if (customService.operations != null) {
				customService.operations = customService.operations.stream()
						.map(eop -> Objects.equals(eop.name, op2replace) ? op : eop)
						.collect(Collectors.toList());
			}

			registerCustomService(id, customService, true);
		} finally {
			wlock.unlock();
		}
	}

	public void removeOperationFromCustomeService(String id, String operationName)
			throws STPropertyAccessException, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, InstantiationException, IllegalAccessException, SchemaException,
			IllegalArgumentException, NoSuchExtensionException, STPropertyUpdateException,
			InvalidConfigurationException, CustomServiceException {
		Lock wlock = lock.writeLock();
		wlock.lock();
		try {
			CustomServiceDefinitionStore cs;
			try {
				cs = (CustomServiceDefinitionStore) exptManager
						.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
			} catch (NoSuchConfigurationManager e) {
				throw new IllegalStateException(
						"Unable to find the configuration manager for custom services", e);
			}

			CustomService customService = cs.getSystemConfiguration(id);

			List<Operation> oldOps = customService.operations;
			if (oldOps != null) {
				customService.operations = customService.operations.stream()
						.filter(eop -> !Objects.equals(eop.name, operationName)).collect(Collectors.toList());

			}

			registerCustomService(id, customService, true);
		} finally {
			wlock.unlock();
		}
	}

	// this method should be invoked after acquiring the write lock
	protected void setDelegateMappingInternal() {
		// defensive copy of the argument list
		delegateMapping = new DelegateRequestMappingHandlerMapping(
				Lists.newArrayList(customServiceHandlers.values()));
		delegateMapping.afterPropertiesSet();
	}

	/*
	 * @formatter:off
	 * Pattern for the recognition of authorizations.
	 * These are the named capturing group:
	 * - "area": matches the area (restricted to letters)
	 * - "subjfun": optionally, matches the subject as a function invocation; "subjfunpar" is the parameter
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
			"^\\s*(?<area>[a-z]+)(?<subj>\\(((?<subjfun>@typeof\\(\\s*#(?<subjfunpar>[a-z]+)\\s*\\))|(?<subjlit>[a-z]+))(\\s*,\\s*(?<scope>\\w+))?\\))?(\\s*,\\s*\\{\\s*(?<userkey>[a-z]+)\\s*:\\s*((?<userlit>\\w+)|(?<userfun>(@langof\\(\\s*#(?<userfunpar>[a-z]+)\\s*\\))))\\s*\\})?\\s*,\\s*(?<crudv>[CRUDV])\\s*$",
			Pattern.CASE_INSENSITIVE);

	private Object buildCustomServiceHandler(String cfgID, CustomService customServiceCfg)
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

		List<Pair<Operation, CustomServiceBackend>> backends = new ArrayList<>(operationDefinitions.size());
		for (Operation operationDefinition : operationDefinitions) {

			CustomServiceBackend customServiceBackend = buildExtension(operationDefinition);
			backends.add(ImmutablePair.of(operationDefinition, customServiceBackend));

			InvocationHandler invocationHandler = customServiceBackend.createInvocationHandler();
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

			List<Parameter> parameters = Optional.ofNullable(operationDefinition.parameters)
					.orElse(Collections.emptyList());
			for (Parameter parameterDefinition : parameters) {
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

		for (Pair<Operation, CustomServiceBackend> confAndBackend : backends) {

			Operation operationDefinition = confAndBackend.getLeft();
			CustomServiceBackend backend = confAndBackend.getRight();

			boolean isWrite = backend.isWrite();

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

			List<Parameter> parameters = Optional.ofNullable(operationDefinition.parameters)
					.orElse(Collections.emptyList());
			for (Parameter parameterDefinition : parameters) {
				parameterBuilder = MoreObjects.firstNonNull(parameterBuilder, methodBuilder)
						.withParameter(generateTypeDefinitionFromSchema(parameterDefinition.type),
								parameterDefinition.name)
						.annotateParameter(AnnotationDescription.Builder.ofType(RequestParam.class)
								.define("value", parameterDefinition.name)
								.define("required", parameterDefinition.required).build());
			}

			String preauthorizeValue;

			if (StringUtils.isNoneBlank(operationDefinition.authorization)) {
				Matcher m = AUTHORIZATION_PATTERN.matcher(operationDefinition.authorization);
				if (!m.find()) {
					throw new RuntimeException("Invalid authorization string");
				}

				StringBuilder sb = new StringBuilder();
				sb.append("@auth.isAuthorized(");
				sb.append("'");
				sb.append(m.group("area"));
				if (m.group("subj") != null) {
					sb.append("(");
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
				}
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
				String crudv = m.group("crudv");
				if (isWrite) {
					if (!crudv.chars().allMatch(c -> c == 'C' || c == 'U' || c == 'D')) {
						throw new IllegalArgumentException(
								"Invalid CRUD operations associated with a write service operation");
					}
				} else {
					if (!Objects.equals(crudv, "R")) {
						throw new IllegalArgumentException(
								"Invalid CRUD operations associated with a read service operation");
					}
				}

				sb.append(", '").append(crudv).append("'");
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
							Object out;
							try {
								out = m.invoke(service, args);
							} catch (InvocationTargetException e) {
								throw e.getCause();
							}

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

		return controller;
	}

	protected CustomServiceBackend buildExtension(Operation operationDefinition)
			throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException {
		@SuppressWarnings("unchecked")
		ConfigurableExtensionFactory<?, Operation> extensionFactory = exptManager
				.getExtensions(CustomServiceBackend.class.getName()).stream()
				.filter(ConfigurableExtensionFactory.class::isInstance)
				.map(ConfigurableExtensionFactory.class::cast)
				.filter(f -> f.getConfigurations().stream().anyMatch(
						c -> c.getClass().getName().equals(operationDefinition.getClass().getName())))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						"Unable to to find an extension for the configuration class "
								+ operationDefinition.getClass().getName()));

		return exptManager.instantiateExtension(CustomServiceBackend.class,
				new PluginSpecification(extensionFactory.getId(), null, null,
						STPropertiesManager.storeSTPropertiesToObjectNode(operationDefinition, true)));
	}

	protected TypeDefinition generateTypeDefinitionFromSchema(Type typeDescription) throws SchemaException {
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
		Lock rlock = lock.readLock();
		rlock.lock();
		try {
			return delegateMapping.getHandlerInternal(request);
		} finally {
			rlock.unlock();
		}
	}

	public boolean isMapped(String id) {
		Lock rlock = lock.readLock();
		rlock.lock();
		try {
			return customServiceHandlers.containsKey(id);
		} finally {
			rlock.unlock();
		}
	}

	public Object getHandler(String serviceName) {
		String id = Iterables.getFirst(customServiceNames2IDs.get(serviceName), null);
		return id != null ? customServiceHandlers.get(id) : null;
	}
}
