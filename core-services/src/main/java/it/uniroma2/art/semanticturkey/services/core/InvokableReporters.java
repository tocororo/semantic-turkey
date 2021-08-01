package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.Closer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.CustomContext;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporter;
import it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporterStore;
import it.uniroma2.art.semanticturkey.config.invokablereporter.ServiceInvocation;
import it.uniroma2.art.semanticturkey.customservice.CustomServiceHandlerMapping;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.FormattedResourceSource;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.font.FontClass;
import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.Response;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.tracker.OperationDescription;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
import it.uniroma2.art.semanticturkey.storage.StorageManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.math3.util.MathUtils;
import org.apache.http.entity.ContentType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.http.HttpEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Null;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@STService
public class InvokableReporters extends STServiceAdapter {

	private static final ContentType TEXT_HTML = ContentType.create("text/html");

	private static final ContentType APPLICATION_PDF = ContentType.create("application/pdf");

	private final String FONT_FILE_NAME = "arial-unicode-ms.ttf";

	private static Logger logger = LoggerFactory.getLogger(InvokableReporters.class);

	@Autowired
	private CustomServiceHandlerMapping customServiceHandlerMapping;

	@Autowired
	private STServiceTracker stServiceTracker;

	private Mustache.Compiler mustacheCompiler;

	private	LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer;

	public InvokableReporters() {
		mustacheCompiler = Mustache.compiler().emptyStringIsFalse(true).zeroIsFalse(true);
		parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
	}

	protected InvokableReporterStore getInvokableReporterStore() throws NoSuchConfigurationManager {
		InvokableReporterStore cm = (InvokableReporterStore) exptManager
				.getConfigurationManager(InvokableReporterStore.class.getName());
		return cm;
	}

	/**
	 * Returns a form for the definition of a new invokable reporter.
	 * 
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'R')")
	@STServiceOperation
	public InvokableReporter getInvokableReporterForm() {
		return new InvokableReporter();
	}

	/**
	 * Returns the <em>scope</em>s on which invokable reporters can be defined.
	 * 
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'R')")
	@STServiceOperation
	public Collection<Scope> getConfigurationScopes() throws NoSuchConfigurationManager {
		InvokableReporterStore cm = getInvokableReporterStore();
		return cm.getConfigurationScopes();
	}

	/**
	 * Returns the <em>references</em> to already defined reporters.
	 * 
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'R')")
	@STServiceOperation
	public Collection<Reference> getInvokableReporterIdentifiers() throws NoSuchConfigurationManager {
		InvokableReporterStore cm = getInvokableReporterStore();
		return cm.getConfigurationReferences(getProject(), UsersManager.getLoggedUser());
	}

	/**
	 * Returns the description of the invokable reporter identified by the supplied <em>reference</em>.
	 * 
	 * @param reference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'R')")
	@STServiceOperation
	public InvokableReporter getInvokableReporter(String reference)
			throws NoSuchConfigurationManager, STPropertyAccessException {
		InvokableReporterStore cm = getInvokableReporterStore();
		return cm.getConfiguration(parseReference(reference));
	}

	/**
	 * Deletes the invokable reporter identified by the supplied <em>reference</em>.
	 * 
	 * @param reference
	 * @throws ConfigurationNotFoundException
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'D')")
	@STServiceOperation(method = RequestMethod.POST)
	public void deleteInvokableReporter(String reference)
			throws ConfigurationNotFoundException, NoSuchConfigurationManager {
		Reference ref = parseReference(reference);
		getInvokableReporterStore().deleteConfiguration(ref);
	}

	/**
	 * Creates an invokable reporter given its <em>definition</em> and a <em>reference</em> telling where its
	 * configuration will be stored.
	 * 
	 * @param reference
	 * @param definition
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'C')")
	@STServiceOperation(method = RequestMethod.POST)
	public void createInvokableReporter(String reference, @JsonSerialized ObjectNode definition)
			throws IOException, WrongPropertiesException, STPropertyUpdateException,
			NoSuchConfigurationManager {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		InvokableReporter defObj = mapper.treeToValue(definition, InvokableReporter.class);

		Reference ref = parseReference(reference);
		getInvokableReporterStore().storeConfiguration(ref, defObj);
	}

	/**
	 * Updates the invokable reporter identified by the supplied <em>reference</em>.
	 * 
	 * @param reference
	 * @param definition
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter)', 'U')")
	@STServiceOperation(method = RequestMethod.POST)
	public void updateInvokableReporter(String reference, @JsonSerialized ObjectNode definition)
			throws IOException, WrongPropertiesException, STPropertyUpdateException,
			NoSuchConfigurationManager {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		InvokableReporter defObj = mapper.treeToValue(definition, InvokableReporter.class);
		Reference ref = parseReference(reference);
		getInvokableReporterStore().storeConfiguration(ref, defObj);
	}

	/**
	 * Adds a <em>section</em> to the invokable reporter identified by the supplied <em>reference</em>. If the
	 * insertion index is negative (by default), then the section is appened to the end of the sections list.
	 * 
	 * @param reference
	 * @param section
	 * @param index
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter, section)', 'C')")
	@STServiceOperation(method = RequestMethod.POST)
	public void addSectionToReporter(String reference, @JsonSerialized ObjectNode section,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "-1") int index)
			throws NoSuchConfigurationManager, IOException, WrongPropertiesException,
			STPropertyAccessException, STPropertyUpdateException {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		ServiceInvocation sectObj = mapper.treeToValue(section, ServiceInvocation.class);

		InvokableReporterStore cm = getInvokableReporterStore();

		Reference ref = parseReference(reference);

		InvokableReporter invokableReporter = cm.getConfiguration(parseReference(reference));

		List<ServiceInvocation> sections = invokableReporter.sections;
		ArrayList<ServiceInvocation> newSections;
		if (sections == null) {
			newSections = new ArrayList<>(1);
		} else {
			newSections = new ArrayList<>(sections.size() + 1);
			newSections.addAll(sections);
		}

		if (index < 0) {
			index = newSections.size(); // this is currently a non-null copy of the original sections array
		}

		newSections.add(index, sectObj);

		invokableReporter.sections = newSections;

		cm.storeConfiguration(ref, invokableReporter);
	}

	/**
	 * Updates the <em>section</em> at the provided <em>index</em> inside the invokable reporter identified by
	 * the supplied <em>reference</em>.
	 * 
	 * @param reference
	 * @param section
	 * @param index
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter, section)', 'U')")
	@STServiceOperation(method = RequestMethod.POST)
	public void updateSectionInReporter(String reference, @JsonSerialized ObjectNode section, int index)
			throws NoSuchConfigurationManager, IOException, WrongPropertiesException,
			STPropertyAccessException, STPropertyUpdateException {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		ServiceInvocation sectObj = mapper.treeToValue(section, ServiceInvocation.class);

		InvokableReporterStore cm = getInvokableReporterStore();

		Reference ref = parseReference(reference);

		InvokableReporter invokableReporter = cm.getConfiguration(parseReference(reference));
		if (invokableReporter.sections == null) {
			throw new IndexOutOfBoundsException();
		}

		invokableReporter.sections.set(index, sectObj);

		cm.storeConfiguration(ref, invokableReporter);
	}

	/**
	 * Removes the <em>section</em> at the provided <em>index</em> inside the invokable reporter identified by
	 * the supplied <em>reference</em>.
	 * 
	 * @param reference
	 * @param index
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException
	 */
	@PreAuthorize("@auth.isAuthorized('invokableReporter(reporter,section)', 'D')")
	@STServiceOperation(method = RequestMethod.POST)
	public void removeSectionFromReporter(String reference, int index) throws NoSuchConfigurationManager,
			IOException, WrongPropertiesException, STPropertyAccessException, STPropertyUpdateException {
		InvokableReporterStore cm = getInvokableReporterStore();

		Reference ref = parseReference(reference);

		InvokableReporter invokableReporter = cm.getConfiguration(parseReference(reference));
		if (invokableReporter.sections == null) {
			throw new IndexOutOfBoundsException();
		}

		invokableReporter.sections.remove(index);

		cm.storeConfiguration(ref, invokableReporter);
	}

	/**
	 * Invokes the referenced reporter and compile a report, which is returned as a structured object.
	 * Optionally, it is possible to generate a textual rendering of the report and, indipendently, include
	 * the associated templates (e.g. for client-side rendering).
	 * 
	 * @param reporterReference
	 * @param render
	 * @param includeTemplate
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws ConfigurationNotFoundException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws InvokableReporterException
	 */
	@PreAuthorize("true") // actual authorization is done when invoking the underlying custom services
	@STServiceOperation
	public Report compileReport(String reporterReference,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "true") boolean render,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "true") boolean includeTemplate)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException, IllegalArgumentException,
			InvokableReporterException {
		Reference ref = parseReference(reporterReference);

		InvokableReporter reporter = (InvokableReporter) exptManager
				.getConfiguration(InvokableReporterStore.class.getName(), ref);

		Report report = new Report();
		report.label = reporter.label;
		report.description = reporter.description;
		if (includeTemplate) {
			report.template = reporter.template;
		}
		report.filename = reporter.filename;
		report.additionalFiles = CollectionUtils.emptyIfNull(reporter.additionalFiles).stream().map(af -> {
			AdditionalFile af2 = new AdditionalFile();
			af2.sourcePath = af.sourcePath;
			af2.destinationPath = af.destinationPath;
			af2.required = af.required;
			af2.sourceExists = StorageManager.exists(parseReference(af.sourcePath));
			return af2;
		}).collect(Collectors.toList());

		ObjectMapper argumentObjectMapper = STPropertiesManager.createObjectMapper();

		if (reporter.sections != null && reporter.sections.size() > 0) {
			report.sections = new ArrayList<>(reporter.sections.size());
			int sectionNumber = -1;
			for (ServiceInvocation serviceInvocation : reporter.sections) {
				sectionNumber++;
				Section section = new Section();
				section.extensionPath = Optional.ofNullable(serviceInvocation.extensionPath).orElse(CustomServiceHandlerMapping.CUSTOM_SERVICES_EXTENSION_PATH);
				section.service = serviceInvocation.service;
				section.operation = serviceInvocation.operation;
				section.arguments = serviceInvocation.arguments;
				section.label = serviceInvocation.label;
				section.description = serviceInvocation.description;

				if (includeTemplate) {
					section.template = serviceInvocation.template;
				}
				report.sections.add(section);

				Object handler;
				Optional<Method> operation;
				if (Objects.equals(section.extensionPath, CustomServiceHandlerMapping.CUSTOM_SERVICES_EXTENSION_PATH)) {
					handler = customServiceHandlerMapping.getHandler(serviceInvocation.service);
					if (handler == null) {
						throw new IllegalArgumentException("Service not found: " + serviceInvocation.service);
					}
					operation = Arrays.stream(handler.getClass().getMethods())
							.filter(m -> m.getName().equals(serviceInvocation.operation)).findAny();

				} else {
					IRI operationIRI = Values.iri("http://semanticturkey.uniroma2.it/services/" + section.extensionPath + "/"
							+ section.service + "/" + section.operation);
					OperationDescription operationDescription = stServiceTracker.getOperationDescription(operationIRI).orElseThrow(() -> new IllegalArgumentException("Operation not found: " + operationIRI));
					HandlerMethod springHandler = (HandlerMethod) operationDescription.getSpringEntry().getValue();
					handler = springHandler.createWithResolvedBean().getBean();
					operation = Optional.of(springHandler.getMethod());
				}

				Object response = null;

				try {

					if (operation.isPresent()) {
						Method m = operation.get();
						if (serviceInvocation.arguments != null && serviceInvocation.arguments.size() > 0) {
							Map<String, String> actualParameters = Optional
									.ofNullable(serviceInvocation.arguments).orElse(Collections.emptyMap());
							Set<String> actualParameterNameSet = actualParameters.keySet();
							String[] parameterNames = parameterNameDiscoverer.getParameterNames(m);

							Set<String> parameterNameSet = new HashSet<>();
							Arrays.stream(parameterNames).forEach(parameterNameSet::add);

							Type[] formalParameters = m.getGenericParameterTypes();
							Object[] convertedArgs = new Object[formalParameters.length];

							SetView<String> undefinedArgs = Sets.difference(actualParameterNameSet,
									parameterNameSet);
							if (!undefinedArgs.isEmpty()) {
								throw new IllegalArgumentException(
										"Provided a value for undefined parameters: "
												+ undefinedArgs.stream().collect(Collectors.joining(",")));
							}

//							SetView<String> missingArgs = Sets.difference(parameterNameSet,
//									actualParameterNameSet);
//							if (!missingArgs.isEmpty()) {
//								throw new IllegalArgumentException("Missing a value for parameters: "
//										+ undefinedArgs.stream().collect(Collectors.joining(",")));
//							}

							for (int i = 0; i < formalParameters.length; i++) {
								String paramName = parameterNames[i];
								Type formalParam = formalParameters[i];
								String actualParam;
								if (actualParameters.get(paramName) == null) {
									RequestParam requestParamAnnot = m.getParameters()[i].getAnnotation(RequestParam.class);
									if (requestParamAnnot == null) continue; // skip non request mapped arguments

									if (requestParamAnnot.required()) {
										throw new IllegalArgumentException(
												"Missing argument for parameter '" + paramName + "'");
									} else {
										if (!Objects.equals(requestParamAnnot.defaultValue(), ValueConstants.DEFAULT_NONE)) {
											actualParam = requestParamAnnot.defaultValue();
										} else {
											if (TypeUtils.isAssignable(formalParam, boolean.class) || TypeUtils.isAssignable(formalParam, Boolean.class)) {
												actualParam = "false";
											} else if (TypeUtils.isAssignable(formalParam, Number.class)
													|| TypeUtils.isAssignable(formalParam, byte.class)
													|| TypeUtils.isAssignable(formalParam, short.class)
													|| TypeUtils.isAssignable(formalParam, int.class)
													|| TypeUtils.isAssignable(formalParam, long.class)
													|| TypeUtils.isAssignable(formalParam, float.class)
													|| TypeUtils.isAssignable(formalParam, double.class)
											) {
												actualParam = "0";
											} else {
												actualParam = null;
											}
										}
									}
								} else {
									actualParam = actualParameters.get(paramName);
								}
								Object convertedParam = actualParam == null ? null : argumentObjectMapper.readValue(
										new StringReader(actualParam),
										argumentObjectMapper.constructType(formalParam));
								convertedArgs[i] = convertedParam;
							}

							try {
								response = m.invoke(handler, convertedArgs);
							} catch (InvocationTargetException e) {
								throw e.getCause();
							}
						} else {
							try {
								response = m.invoke(handler);
							} catch (InvocationTargetException e) {
								throw e.getCause();

							}
						}

					} else {
						throw new IllegalArgumentException("Operation not found: " + operation);
					}

					// unwrap HttpEntities
					if (response instanceof HttpEntity) {
						response = ((HttpEntity<?>) response).getBody();
					}
					section.result = ((Response<?>) response).getResult();
				} catch (Throwable e) {
					if (e instanceof Error) {
						throw (Error) e;
					} else {
						throw new InvokableReporterException(
								"An exception was raised on execution of sections[" + sectionNumber + "]: "
										+ e.getClass().getSimpleName() + ":" + e.getMessage(),
								e);
					}
				}
			}

			if (render) {
				ObjectMapper responseObjectMapper = RequestMappingHandlerAdapterPostProcessor
						.createObjectMapper(exptManager);

				Iterator<Section> reportSectionIt = report.sections.iterator();
				Iterator<ServiceInvocation> reporterSectionIt = reporter.sections.iterator();

				while (reportSectionIt.hasNext()) {
					Section reportSection = reportSectionIt.next();
					ServiceInvocation reporterSection = reporterSectionIt.next();

					@Nullable
					String template = reporterSection.template;
					if (render && template != null) {
						Object adaptedSection = JacksonCustomContext
								.adaptJsonNode(responseObjectMapper.valueToTree(reportSection));
						String rendering = mustacheCompiler.compile(template).execute(adaptedSection);
						reportSection.rendering = rendering;
					}
				}

				String template = reporter.template;
				Object adaptedReport = JacksonCustomContext
						.adaptJsonNode(responseObjectMapper.valueToTree(report));

				String rendering = mustacheCompiler.compile(template).execute(adaptedReport);
				report.rendering = rendering;
				report.mimeType = reporter.mimeType;
			}
		}

		return report;
	}

	/**
	 * Invokes the referenced reporter and compile a report, which is downloaded as a rendered resource.
	 * Optionally, it is possible to convert the report to a different MIME type (e.g.
	 * <code>application/pdf</code> to convert the report into a PDF document).
	 * 
	 * @param response
	 * @param reporterReference
	 * @param targetMimeType
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws ConfigurationNotFoundException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws InvokableReporterException
	 */
	@PreAuthorize("true") // actual authorization is done when invoking the underlying custom services
	@STServiceOperation
	public void compileAndExportReport(HttpServletResponse response, String reporterReference,
									   @it.uniroma2.art.semanticturkey.services.annotations.Optional String targetMimeType,
									   @it.uniroma2.art.semanticturkey.services.annotations.Optional PluginSpecification deployerSpec)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException, IllegalArgumentException,
			InvokableReporterException, InvalidConfigurationException {
		Report report = compileReport(reporterReference, true, true);
		String reportMimeType = report.mimeType;
		String rendering = report.rendering != null ? report.rendering : "";

		byte[] bytes;
		ContentType contentType;
		if (targetMimeType != null) {
			if (Objects.equals(targetMimeType, APPLICATION_PDF.getMimeType())) {
				if (Objects.equals(reportMimeType, TEXT_HTML.getMimeType())) {
					Document jsoupDocument = Jsoup.parse(rendering);
					jsoupDocument.head().prepend(
							"<style type=\"text/css\">body { font-family:\"arial unicode ms\"; }</style>");
					org.w3c.dom.Document doc = W3CDom.convert(jsoupDocument);

					try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
						PdfRendererBuilder builder = new PdfRendererBuilder();
						builder.useFastMode();
						FontClass fontClass = new FontClass();
						try (InputStream inputStream = fontClass.getFontFile(FONT_FILE_NAME);) {
							builder.useFont(() -> inputStream, "arial unicode ms");
							builder.withW3cDocument(doc, "http://example.org/");
							builder.toStream(os);
							builder.run();
						}

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
			contentType = ContentType.create(reportMimeType, StandardCharsets.UTF_8);
		}

		String defaultFileExtension = contentType.getMimeType().equals(TEXT_HTML.getMimeType()) ? "html" : contentType.getMimeType().equals(APPLICATION_PDF.getMimeType()) ? "pdf" : "dat";
		String mimeType = contentType.getMimeType();
		Charset charset = contentType.getCharset();
		
		String mainReportFilename;
		if (report.filename != null) {
			mainReportFilename = report.filename;
		} else {
			mainReportFilename = "report-" + System.currentTimeMillis() + "." + defaultFileExtension;
		}

		String downloadFilename;

		try (Closer closer = Closer.create()) {

			FormattedResourceSource source;

			if (CollectionUtils.isEmpty(report.additionalFiles)) { // no additional files, just download the main report
				downloadFilename = mainReportFilename;
				ClosableFormattedResource res = closer.register(new ClosableFormattedResource(bytes, defaultFileExtension, mimeType, charset, downloadFilename));
				source = new FormattedResourceSource(res);
			} else { // as there are additional files, create a compressed archive
				Map<String, String> env = new HashMap<>();
				env.put("create", "true");
				// locate file system by using the syntax
				// defined in java.net.JarURLConnection
				File tempDirectory = Files.createTempDirectory("report_").toFile();
				closer.register(() -> FileUtils.deleteQuietly(tempDirectory));

				File destinationArchive = new File(tempDirectory, StringUtils.substringBeforeLast(mainReportFilename, ".") + ".zip");

				URI destinationArchiveURI = URI.create("jar:" + destinationArchive.toURI().toString());

				try (FileSystem zipfs = FileSystems.newFileSystem(destinationArchiveURI, env)) {
//					Path externalTxtFile = Paths.get("/codeSamples/zipfs/SomeTextFile.txt");
//					Path pathInZipfile = zipfs.getPath("/SomeTextFile.txt");
//					// copy a file into the zip file
//					Files.copy( externalTxtFile,pathInZipfile,
//							StandardCopyOption.REPLACE_EXISTING);

					// copy each additional files
					for (AdditionalFile af: report.additionalFiles) {
						try (InputStream is = StorageManager.getFileContent(parseReference(af.sourcePath))) {
							Path zipfsPath = zipfs.getPath(StringUtils.prependIfMissing(af.destinationPath, "/"));
							Path zipfsParentPath = zipfsPath.getParent();
							if (zipfsParentPath != null) {
								Files.createDirectories(zipfsParentPath);
							}
							Files.copy(is, zipfsPath,
									StandardCopyOption.REPLACE_EXISTING);
						} catch (FileNotFoundException e) {
							if (af.required) throw e;
						}
					}
					// copy main report file
					Files.copy(new ByteArrayInputStream(bytes), zipfs.getPath("/" + mainReportFilename),
							StandardCopyOption.REPLACE_EXISTING);
				}

				ClosableFormattedResource res = closer.register(new ClosableFormattedResource(destinationArchive, "zip", "application/zip", null, destinationArchive.getName()));
				source = new FormattedResourceSource(res);
			}

			Export.downloadOrDeploy(exptManager, stServiceContext, response, deployerSpec, source);
		}
	}

	public static class Report {
		public String label;
		@Nullable
		public String description;
		public List<AdditionalFile> additionalFiles;
		public List<Section> sections;
		@Nullable
		public String template;
		@Nullable
		public String rendering;
		@Nullable
		public String mimeType;
		@Nullable
		public String filename;
	}

	public static class AdditionalFile {
		public String sourcePath;
		public String destinationPath;
		public boolean required;
		public boolean sourceExists;
	}
	public static class Section {
		public String extensionPath;
		public String service;
		public String operation;
		@Nullable
		public Map<String, String> arguments;
		@Nullable
		public String label;
		@Nullable
		public String description;
		public Object result;
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

			@Override
			public String toString() {
				return node.toString();
			}
		}

		private static class ArrayAdaptor implements Iterable<Object>, CustomContext {

			private ArrayNode node;

			public ArrayAdaptor(ArrayNode node) {
				this.node = node;
			}

			@Override
			public Iterator<Object> iterator() {
				return Iterators.transform(node.elements(), JacksonCustomContext::adaptJsonNode);
			}

			@Override
			public String toString() {
				return node.toString();
			}

			@Override
			public Object get(String name) throws Exception {
				if (name.length() > 0 && Character.isDigit(name.charAt(0))) {
					return adaptJsonNode(node.get(Integer.parseInt(name)));
				} else {
					return null;
				}
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
			if (node == null || node.isNull()) {
				return null;
			} else if (node.isArray()) {
				return new ArrayAdaptor((ArrayNode) node);
			} else if (node.isObject()) {
				return new ObjectNodeCustomContext((ObjectNode) node);
			} else if (node.isBoolean()) {
				return node.asBoolean();
			} else if (node.isTextual()) {
				return node.textValue();
			} else if (node.isLong() || node.isInt()) {
				return node.asLong();
			} else if (node.isFloat() || node.isDouble()) {
				return node.asDouble();
			} else {
				return new NodeAdaptor(node);
			}
		}

	}
}
