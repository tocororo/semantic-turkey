package it.uniroma2.art.semanticturkey.generation.annotation.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import it.uniroma2.art.semanticturkey.generation.annotation.DoNotGenerateController;
import it.uniroma2.art.semanticturkey.generation.annotation.processor.internal.VelocitySupportTools;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@SupportedAnnotationTypes("it.uniroma2.art.semanticturkey.services.annotations.STService")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class STServiceProcessor extends AbstractProcessor {

	private static final String CLASSPATH_RESOURCES_BASE = "/" +STServiceProcessor.class.getPackage().getName()
			.replace('.', '/');
	private static final String VELOCITY_PROPERTIES_CLASSPATH_NAME = CLASSPATH_RESOURCES_BASE
			+ "/velocity.properties";
	private static final String TEMPLATE_CLASSPATH_CLASSPATH_NAME = CLASSPATH_RESOURCES_BASE
			+ "/spring_controller.vm";

	private static class Options {
		public static final String CONTROLLER_PATH_PREFIX = "it.uniroma2.art.semanticturkey.generation.controller.path.prefix";
	}
	
	private static final Map<String, String> optionsWithDefaults;
	
	static {
		optionsWithDefaults = new HashMap<String, String>();
		optionsWithDefaults.put(Options.CONTROLLER_PATH_PREFIX, "");
	}
	
	private Map<String, String> options;
	private Types typeUtils;
	private Elements elementUtils;
	
	@Override
	public Set<String> getSupportedOptions() {
		return optionsWithDefaults.keySet();
	}
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		
		typeUtils = processingEnv.getTypeUtils();
		elementUtils = processingEnv.getElementUtils();
		
		Map<String, String> providedOptions = processingEnv.getOptions();
		
		options = new HashMap<String, String>();
		
		for (Entry<String, String> entry : optionsWithDefaults.entrySet()) {
			String providedOption = providedOptions.get(entry.getKey());
			
			if (providedOption == null) {
				options.put(entry.getKey(), entry.getValue());
			} else {
				options.put(entry.getKey(), providedOption);
			}
		}
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		System.out.println("generating:");
		for (Element i : roundEnv.getElementsAnnotatedWith(STService.class)) {
			if (i.getAnnotation(DoNotGenerateController.class) != null) {
				continue;
			}
			
			if (i.getKind() != ElementKind.CLASS)
				continue;

			String fqClassName = null;
			String classSimpleName = null;
			String packageName = null;
			Map<String, VariableElement> fields = new HashMap<String, VariableElement>();
			Map<String, ExecutableElement> methods = new HashMap<String, ExecutableElement>();
			TypeElement classElement = (TypeElement) i;
			PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();

			System.out.println("annotated class: " + classElement.getQualifiedName());

			fqClassName = classElement.getQualifiedName().toString();
			classSimpleName = classElement.getSimpleName().toString();
			packageName = packageElement.getQualifiedName().toString();

			for (Element e : roundEnv.getElementsAnnotatedWith(STServiceOperation.class)) {
				if (e.getKind() == ElementKind.METHOD) {

					System.out.println(fqClassName + " " + (TypeElement) e.getEnclosingElement());

					if (!((TypeElement) e.getEnclosingElement()).getQualifiedName().toString()
							.equals(fqClassName))
						continue;

					ExecutableElement exeElement = (ExecutableElement) e;

					System.out.println("annotated method: " + exeElement.getSimpleName());
					
					methods.put(exeElement.getSimpleName().toString(), exeElement);
				}

			}
			if (fqClassName != null) {
				try {
					Properties props = new Properties();
					InputStream is = STServiceProcessor.class
							.getResourceAsStream(VELOCITY_PROPERTIES_CLASSPATH_NAME);
					try {
						props.load(is);
					} finally {
						is.close();
					}
					VelocityEngine ve = new VelocityEngine(props);
					ve.init();

					VelocityContext vc = new VelocityContext();

					Template vt = ve.getTemplate(TEMPLATE_CLASSPATH_CLASSPATH_NAME);

					JavaFileObject jfo;
					StringBuilder sb = new StringBuilder(packageName).append(".controllers");

					String generatedPackageName = sb.toString();
					String generatedClassSimpleName = classSimpleName + "Controller";

					vc.put("classSimpleName", classSimpleName);
					vc.put("packageName", packageName);
					vc.put("fields", fields);
					vc.put("methods", methods);
					vc.put("tools", new VelocitySupportTools(typeUtils, elementUtils));

					vc.put("generatedPackageName", generatedPackageName);
					vc.put("generatedClassSimpleName", generatedClassSimpleName);
					
					vc.put("controllerPathPrefix", options.get(Options.CONTROLLER_PATH_PREFIX));
					
					jfo = processingEnv.getFiler().createSourceFile(
							generatedPackageName + "." + generatedClassSimpleName);
					System.out.println("creating source file: " + jfo.toUri());

					Writer writer = jfo.openWriter();

					System.out.println("applying velocity template: " + vt.getName());

					vt.merge(vc, writer);

					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
					processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
				}
			}
		}

		return true;
	}
}