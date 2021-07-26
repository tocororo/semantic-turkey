package it.uniroma2.art.semanticturkey.services.core;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import it.uniroma2.art.semanticturkey.config.customservice.CustomService;
import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.config.customservice.Type;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties;
import it.uniroma2.art.semanticturkey.services.tracker.OperationDescription;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This class provides services for obtain information on the available services.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Services extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Services.class);

	@Autowired
	private STServiceTracker stServiceTracker;

	private LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	/**
	 * Returns the extension paths associated with active extension bundles. Usually, they are in the form
	 * <code>groupId/artifactId</code>.
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getExtensionPaths() {
		return stServiceTracker.getExtensionPaths();
	}

	/**
	 * Returns the services classes utilizing a given extension path.
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getServiceClasses(String extensionPath) {
		return stServiceTracker.getServiceClasses(extensionPath);
	}

	/**
	 * Returns the operations of a service class bound to an extension path.
	 * 
	 * @param extensionPath
	 * @param serviceClass
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getServiceOperations(String extensionPath, String serviceClass) {
		return stServiceTracker.getServiceOperations(extensionPath, serviceClass);
	}

	/**
	 * Returns the description of an operation. The <em>operationIRI</em> is structured as follows: <code>http://semanticturkey.uniroma2.it/services/{extensionPath}/{serviceClass}/{operation}</code>
	 *
	 * @param operationIRI
	 * @return
	 */
	@STServiceOperation
	public Optional<OperationDescription> getServiceOperation(IRI operationIRI) {
		return stServiceTracker.getOperationDescription(operationIRI);
	}

	/**
	 * Returns the description of an operation. The <em>operationIRI</em> is structured as follows: <code>http://semanticturkey.uniroma2.it/services/{extensionPath}/{serviceClass}/{operation}</code>
	 *
	 * @param operationIRI
	 * @return
	 */
	@STServiceOperation
	public Operation getServiceOperationAsCustomService(IRI operationIRI) {
		OperationDescription operationDescription = stServiceTracker.getOperationDescription(operationIRI).orElseThrow(() -> new IllegalArgumentException("Operation not found: " + RenderUtils.toSPARQL(operationIRI)));
		Method m = operationDescription.getSpringEntry().getValue().getMethod();
		LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] parameterNames = parameterNameDiscoverer.getParameterNames(m);
		AnnotatedType[] parameterTypes = m.getAnnotatedParameterTypes();
		Parameter[] parameters = m.getParameters();

		Operation invocationForm = new Operation();
		invocationForm.name = StringUtils.removeEnd(m.getName(), "Published");

		List<it.uniroma2.art.semanticturkey.config.customservice.Parameter> parameterDefs = new ArrayList<>();

		for (int i = 0 ; i < parameterNames.length ; i++) {
			it.uniroma2.art.semanticturkey.config.customservice.Parameter param = new it.uniroma2.art.semanticturkey.config.customservice.Parameter();
			param.name = parameterNames[i];
			param.required = Optional.of(parameters[i].getAnnotation(RequestParam.class))
					.map(RequestParam::required)
					.orElse(false);
			param.type = Type.fromJavaType(parameterTypes[i].getType());

			parameterDefs.add(param);
		}

		invocationForm.parameters = parameterDefs;

		return invocationForm;
	}
	/**
	 * Returns the description of an operation. The <em>operationIRI</em> is structured as follows: <code>http://semanticturkey.uniroma2.it/services/{extensionPath}/{serviceClass}/{operation}</code>
	 *
	 * @param operationIRI
	 * @return
	 */
	@STServiceOperation
	public STProperties getServiceInvocationForm(IRI operationIRI) {
		OperationDescription operationDescription = stServiceTracker.getOperationDescription(operationIRI).orElseThrow(() -> new IllegalArgumentException("Operation not found: " + RenderUtils.toSPARQL(operationIRI)));
		DynamicSTProperties invocationForm = new DynamicSTProperties("serviceInvocation");
		Method m = operationDescription.getSpringEntry().getValue().getMethod();
		LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] parameterNames = parameterNameDiscoverer.getParameterNames(m);
		AnnotatedType[] parameterTypes = m.getAnnotatedParameterTypes();
		Parameter[] parameters = m.getParameters();

		for (int i = 0 ; i < parameterNames.length ; i++) {
			DynamicSTProperties.PropertyDefinition def = new DynamicSTProperties.PropertyDefinition(Arrays.asList(Values.literal(parameterNames[i])),
					Collections.emptyList(),
					Optional.of(parameters[i].getAnnotation(RequestParam.class))
							.map(RequestParam::required)
							.orElse(false),
					parameterTypes[i]);
			invocationForm.addProperty(parameterNames[i], def);
		}
		return invocationForm;
	}
};