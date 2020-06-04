package it.uniroma2.art.semanticturkey.services.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Null;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.CustomContext;

import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporter;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporterStore;
import it.uniroma2.art.semanticturkey.config.invokablereporter.ServiceInvocation;
import it.uniroma2.art.semanticturkey.customservice.CustomServiceHandlerMapping;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;
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

	private static final String TEXT_HTML = "text/html";

	private static final String APPLICATION_PDF = "application/pdf";

	private static Logger logger = LoggerFactory.getLogger(InvokableReporters.class);

	@Autowired
	private CustomServiceHandlerMapping customServiceHandlerMapping;

	private Mustache.Compiler mustacheCompiler;

	private ObjectMapper objectMapper;

	public InvokableReporters() {
		mustacheCompiler = Mustache.compiler();
		objectMapper = RequestMappingHandlerAdapterPostProcessor.createObjectMapper();
	}

	@STServiceOperation
	public Report compileReport(String reporterReference,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "true") boolean render,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "true") boolean includeTemplate)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Reference ref = parseReference(reporterReference);

		InvokableReporter reporter = (InvokableReporter) exptManager
				.getConfiguration(InvokableReporterStore.class.getName(), ref);

		Report report = new Report();
		report.label = reporter.label;
		report.description = reporter.description;
		if (includeTemplate) {
			report.template = reporter.template;
		}
		ObjectMapper objectMapper = STPropertiesManager.createObjectMapper();

		if (reporter.sections != null && reporter.sections.size() > 0) {
			report.sections = new ArrayList<>(reporter.sections.size());
			for (ServiceInvocation serviceInvocation : reporter.sections) {
				Section section = new Section();
				section.service = serviceInvocation.service;
				section.operation = serviceInvocation.operation;
				section.arguments = serviceInvocation.arguments;
				section.label = serviceInvocation.label;
				section.description = serviceInvocation.description;

				if (includeTemplate) {
					section.template = serviceInvocation.template;
				}
				report.sections.add(section);

				Object handler = customServiceHandlerMapping.getHandler(serviceInvocation.service);

				Optional<Method> operation = Arrays.stream(handler.getClass().getMethods())
						.filter(m -> m.getName().equals(serviceInvocation.operation)).findAny();

				Object result = null;

				try {

					if (operation.isPresent()) {
						Method m = operation.get();
						if (serviceInvocation.arguments != null && serviceInvocation.arguments.size() > 0) {
							Map<String, String> actualParameters = Optional
									.ofNullable(serviceInvocation.arguments).orElse(Collections.emptyMap());

							String[] parameterNames = Arrays.stream(m.getParameters()).map(Parameter::getName)
									.collect(Collectors.toList()).toArray(new String[0]);
							Type[] formalParameters = m.getGenericParameterTypes();
							Object[] convertedArgs = new Object[formalParameters.length];

							if (actualParameters.size() != formalParameters.length) {
								throw new IllegalArgumentException(
										"actual parameters count doesn't match the number of formal parameters");
							}

							for (int i = 0; i < formalParameters.length; i++) {
								String paramName = parameterNames[i];
								Type formalParam = formalParameters[i];
								if (!actualParameters.containsKey(paramName)) {
									throw new IllegalArgumentException(
											"Missing argument for parameter '" + paramName + "'");
								}
								String actualParam = actualParameters.get(paramName);

								Object convertedParam = objectMapper.readValue(new StringReader(actualParam),
										objectMapper.constructType(formalParam));
								convertedArgs[i++] = convertedParam;

							}

							result = m.invoke(handler, convertedArgs);
						} else {
							result = m.invoke(handler);
						}

					} else {
						throw new IllegalArgumentException("Operation not found");
					}

					section.result = ((Response<?>) result).getResult();
				} catch (Exception e) {
					section.exception = e.getClass().getSimpleName() + ":" + e.getMessage();
				}
			}

			if (render) {
				Iterator<Section> reportSectionIt = report.sections.iterator();
				Iterator<ServiceInvocation> reporterSectionIt = reporter.sections.iterator();

				while (reportSectionIt.hasNext()) {
					Section reportSection = reportSectionIt.next();
					ServiceInvocation reporterSection = reporterSectionIt.next();

					@Nullable
					String template = reporterSection.template;
					if (render && template != null) {
						Object adaptedSection = JacksonCustomContext
								.adaptJsonNode(objectMapper.valueToTree(reportSection));
						String rendering = mustacheCompiler.compile(template).execute(adaptedSection);
						reportSection.rendering = rendering;
					}
				}

				String template = reporter.template;
				Object adaptedReport = JacksonCustomContext.adaptJsonNode(objectMapper.valueToTree(report));

				String rendering = mustacheCompiler.compile(template).execute(adaptedReport);
				report.rendering = rendering;
				report.mimeType = reporter.mimeType;
			}
		}

		return report;
	}

	@STServiceOperation
	public void compileAndDownloadReport(HttpServletResponse response, String reporterReference,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional String targetMimeType)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Report report = compileReport(reporterReference, true, true);
		String reportMimeType = report.mimeType;
		String rendering = report.rendering != null ? report.rendering : "";

		byte[] bytes;
		String contentType;
		if (targetMimeType != null) {
			if (Objects.equals(targetMimeType, APPLICATION_PDF)) {
				if (Objects.equals(reportMimeType, TEXT_HTML)) {
					Document jsoupDocument = Jsoup.parse(rendering);
					org.w3c.dom.Document doc = W3CDom.convert(jsoupDocument);

					try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
						PdfRendererBuilder builder = new PdfRendererBuilder();
						builder.useFastMode();
						builder.withW3cDocument(doc, "http://example.org/");
						builder.toStream(os);
						builder.run();

						bytes = os.toByteArray();
						contentType = APPLICATION_PDF;
					}

				} else {
					throw new IllegalArgumentException("Unsupported conversion from '" + reportMimeType
							+ "' to '" + targetMimeType + "'");
				}
			} else {
				throw new IllegalArgumentException("Unsupported target MIME type: " + targetMimeType);
			}

			contentType = APPLICATION_PDF;
		} else {
			bytes = rendering.getBytes(StandardCharsets.UTF_8);
			contentType = ContentType.getByMimeType(reportMimeType).withCharset(StandardCharsets.UTF_8)
					.toString();
		}

		response.setContentType(contentType);
		response.setContentLength(bytes.length);

		try (OutputStream os = response.getOutputStream()) {
			IOUtils.write(bytes, os);
		}
	}

	public static class Report {
		public String label;
		@Nullable
		public String description;
		public List<Section> sections;
		@Nullable
		public String template;
		@Nullable
		public String rendering;
		@Nullable
		public String mimeType;
	}

	public static class Section {
		public String service;
		public String operation;
		@Nullable
		public Map<String, String> arguments;
		@Nullable
		public String label;
		@Nullable
		public String description;
		@Nullable
		public Object result;
		@Nullable
		public Object exception;
		@Null
		public String template;
		@Nullable
		public String rendering;
	}

	public static class JacksonCustomContext {

		private static class ObjectNodeCustomContext implements CustomContext {

			private ObjectNode node;

			public ObjectNodeCustomContext(ObjectNode node) {
				this.node = node;
			}

			@Override
			public Object get(String name) throws Exception {
				return JacksonCustomContext.adaptJsonNode(node.get(name));
			}

		}

		private static class ArrayAdaptor implements Iterable<Object> {

			private ArrayNode node;

			public ArrayAdaptor(ArrayNode node) {
				this.node = node;
			}

			@Override
			public Iterator<Object> iterator() {
				return Iterators.transform(node.elements(), JacksonCustomContext::adaptJsonNode);
			}

		}

		private static class NodeAdaptor {

			private JsonNode node;

			public NodeAdaptor(JsonNode node) {
				this.node = node;
			}

			@Override
			public String toString() {
				return node.asText();
			}
		}

		public static Object adaptJsonNode(JsonNode node) {
			if (node == null) {
				return null;
			} else if (node.isArray()) {
				return new ArrayAdaptor((ArrayNode) node);
			} else if (node.isObject()) {
				return new ObjectNodeCustomContext((ObjectNode) node);
			} else {
				return new NodeAdaptor(node);
			}
		}

	}
}
