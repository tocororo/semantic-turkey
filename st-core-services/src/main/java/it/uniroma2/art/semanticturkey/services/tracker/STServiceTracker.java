package it.uniroma2.art.semanticturkey.services.tracker;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.rdf4j.model.IRI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * This singleton object tracks services inside Semantic Turkey.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@Component
public class STServiceTracker {

	private ServiceTracker serviceTracker;
	private Map<String, Map<String, Map<String, OperationDescription>>> extensionPath2serviceClass2operation2meta;
	private Map<IRI, OperationDescription> iri2operationDescription;

	private static final Pattern regex = Pattern.compile("^\\/+([^/]+)\\/+([^/]+)\\/+([^/]+)\\/+([^/]+)$");

	@Autowired
	public STServiceTracker(BundleContext bundleContext) throws InvalidSyntaxException {
		this.extensionPath2serviceClass2operation2meta = new ConcurrentHashMap<>();
		this.iri2operationDescription = new ConcurrentHashMap<>();

		this.serviceTracker = new ServiceTracker(bundleContext,
				bundleContext.createFilter(
						"(" + Constants.OBJECTCLASS + "=" + WebApplicationContext.class.getName() + ")"),
				new ServiceTrackerCustomizer() {

					@Override
					public void removedService(ServiceReference reference, Object service) {
						WebApplicationContext wac = (WebApplicationContext) service;

						try {
							RequestMappingHandlerMapping handlerMapping = wac
									.getBean(RequestMappingHandlerMapping.class);

							for (Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping
									.getHandlerMethods().entrySet()) {
								Set<String> patterns = entry.getKey().getPatternsCondition().getPatterns();
								if (patterns.size() != 1)
									continue;

								String pattern = patterns.iterator().next();

								Matcher matcher = regex.matcher(pattern);
								if (matcher.find()) {
									String groupId = matcher.group(1);
									String artifactId = matcher.group(2);
									String serviceClass = matcher.group(3);
									String operation = matcher.group(4);

									Map<String, Map<String, OperationDescription>> serviceClass2operation2meta = extensionPath2serviceClass2operation2meta
											.getOrDefault(groupId + "/" + artifactId, Collections.emptyMap());
									Map<String, OperationDescription> operation2meta = serviceClass2operation2meta
											.getOrDefault(serviceClass, Collections.emptyMap());
									if (operation2meta.remove(operation) != null
											&& operation2meta.isEmpty()) {
										if (serviceClass2operation2meta.remove(serviceClass) != null
												&& serviceClass2operation2meta.isEmpty()) {
											Map<String, Map<String, OperationDescription>> removedSeviceClass2Operation2Meta = extensionPath2serviceClass2operation2meta
													.remove(groupId + "/" + artifactId);

											removedSeviceClass2Operation2Meta.values().stream()
													.flatMap(m -> m.values().stream())
													.map(OperationDescription::getOperationIRI)
													.forEach(iri2operationDescription::remove);
										}
									}
								}
							}
						} catch (NoSuchBeanDefinitionException e) {
						}
					}

					@Override
					public void modifiedService(ServiceReference reference, Object service) {
						// TODO Auto-generated method stub

					}

					@Override
					public Object addingService(ServiceReference reference) {
						WebApplicationContext wac = (WebApplicationContext) bundleContext
								.getService(reference);
						try {
							RequestMappingHandlerMapping handlerMapping = wac
									.getBean(RequestMappingHandlerMapping.class);
							for (Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping
									.getHandlerMethods().entrySet()) {
								Set<String> patterns = entry.getKey().getPatternsCondition().getPatterns();
								if (patterns.size() != 1)
									continue;

								String pattern = patterns.iterator().next();

								Matcher matcher = regex.matcher(pattern);
								if (matcher.find()) {
									String groupId = matcher.group(1);
									String artifactId = matcher.group(2);
									String serviceClass = matcher.group(3);
									String operation = matcher.group(4);

									Map<String, Map<String, OperationDescription>> serviceClass2operation2meta = extensionPath2serviceClass2operation2meta
											.computeIfAbsent(groupId + "/" + artifactId,
													key -> new ConcurrentHashMap<>());
									Map<String, OperationDescription> operation2meta = serviceClass2operation2meta
											.computeIfAbsent(serviceClass, key -> new ConcurrentHashMap<>());
									OperationDescription operationDescription = OperationDescription
											.create(wac, groupId, artifactId, serviceClass, operation, entry);
									operation2meta.put(operation, operationDescription);
									iri2operationDescription.put(operationDescription.getOperationIRI(),
											operationDescription);
								}
							}

						} catch (NoSuchBeanDefinitionException e) {
						}
						return wac;
					}

				});
	}

	@PostConstruct
	public void initialize() {
		this.serviceTracker.open(false);
	}

	@PreDestroy
	public void destroy() {
		this.serviceTracker.close();
	}

	public Collection<String> getExtensionPaths() {
		return extensionPath2serviceClass2operation2meta.keySet();
	}

	public Collection<String> getServiceClasses(String extensionPath) {
		return extensionPath2serviceClass2operation2meta.getOrDefault(extensionPath, Collections.emptyMap())
				.keySet();
	}

	public Collection<String> getServiceOperations(String extensionPath, String serviceClass) {
		return extensionPath2serviceClass2operation2meta.getOrDefault(extensionPath, Collections.emptyMap())
				.getOrDefault(serviceClass, Collections.emptyMap()).keySet().stream()
				.map(operation -> "http://semanticturkey.uniroma2.it/services/" + extensionPath + "/"
						+ serviceClass + "/" + operation)
				.collect(Collectors.toSet());

	}

	public Optional<OperationDescription> getOperationDescription(IRI operationIRI) {
		return Optional.ofNullable(iri2operationDescription.get(operationIRI));
	}
}
