package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;

import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporter;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporterStore;
import it.uniroma2.art.semanticturkey.config.invokablereporter.ServiceInvocation;
import it.uniroma2.art.semanticturkey.customservice.CustomServiceHandlerMapping;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.Response;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@STService
public class InvokableReporters extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(InvokableReporters.class);

	@Autowired
	private CustomServiceHandlerMapping customServiceHandlerMapping;

	/**
	 * Reloads all custom services.
	 * 
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * 
	 */
	@STServiceOperation
	public Report compileReport(String reporterReference) throws NoSuchConfigurationManager, IOException,
			ConfigurationNotFoundException, WrongPropertiesException, STPropertyAccessException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Reference ref = parseReference(reporterReference);

		InvokableReporter reporter = (InvokableReporter) exptManager
				.getConfiguration(InvokableReporterStore.class.getName(), ref);

		Report report = new Report();
		report.label = reporter.label;
		report.description = reporter.description;

		ObjectMapper objectMapper = STPropertiesManager.createObjectMapper();

		if (reporter.serviceInvocations != null && reporter.serviceInvocations.size() > 0) {
			report.sections = new ArrayList<>(reporter.serviceInvocations.size());
			for (ServiceInvocation serviceInvocation : reporter.serviceInvocations) {
				Section section = new Section();
				report.sections.add(section);

				Object handler = customServiceHandlerMapping.getHandler(serviceInvocation.service);

				Optional<Method> operation = Arrays.stream(handler.getClass().getMethods())
						.filter(m -> m.getName().equals(serviceInvocation.operation)).findAny();

				Object result = null;

				if (operation.isPresent()) {
					Method m = operation.get();
					if (serviceInvocation.arguments != null && serviceInvocation.arguments.size() > 0) {
						List<String> actualParameters = serviceInvocation.arguments;
						Type[] formalParameters = m.getGenericParameterTypes();

						Object[] convertedArgs = new Object[formalParameters.length];

						if (actualParameters.size() != formalParameters.length) {
							throw new IllegalArgumentException(
									"actual parameters count doesn't match the number of formal parameters");
						}

						Iterator<String> actIt = actualParameters.iterator();
						Iterator<Type> formIt = Iterators.forArray(formalParameters);

						int i = 0;
						while (actIt.hasNext()) {
							String actP = actIt.next();
							Type formP = formIt.next();

							Object convertedP = objectMapper.readValue(new StringReader(actP),
									objectMapper.constructType(formP));

							convertedArgs[i++] = convertedP;
						}
						result = m.invoke(handler, convertedArgs);
					} else {
						result = m.invoke(handler);
					}

					section.result = ((Response<?>)result).getResult();
				}
			}
		}

		return report;
	}

	public static class Report {
		public String label;
		public String description;
		public List<Section> sections;
	}

	public static class Section {
		public Object result;
	}
}
