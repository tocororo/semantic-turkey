package it.uniroma2.art.semanticturkey.generation.annotation.processor;

import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
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
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;


@SupportedAnnotationTypes("it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class STServiceProcessor extends AbstractProcessor {

	private static final String CLASSPATH_RESOURCES_BASE = "/" +STServiceProcessor.class.getPackage().getName()
			.replace('.', '/');
	private static final String VELOCITY_PROPERTIES_CLASSPATH_NAME = CLASSPATH_RESOURCES_BASE
			+ "/velocity.properties";
	private static final String TEMPLATE_CLASSPATH_CLASSPATH_NAME = CLASSPATH_RESOURCES_BASE
			+ "/spring_controller.vm";

	public STServiceProcessor() {
		super();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		System.out.println("generating:");
		for (Element i : roundEnv.getElementsAnnotatedWith(GenerateSTServiceController.class)) {
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

			for (Element e : roundEnv.getElementsAnnotatedWith(GenerateSTServiceController.class)) {
				if (e.getKind() == ElementKind.FIELD) {

					System.out.println(fqClassName + " " + (TypeElement) e.getEnclosingElement());

					if (!((TypeElement) e.getEnclosingElement()).getQualifiedName().toString()
							.equals(fqClassName))
						continue;

					VariableElement varElement = (VariableElement) e;

					System.out.println("annotated field: " + varElement.getSimpleName());
					fields.put(varElement.getSimpleName().toString(), varElement);

				} else if (e.getKind() == ElementKind.METHOD) {

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

					vc.put("generatedPackageName", generatedPackageName);
					vc.put("generatedClassSimpleName", generatedClassSimpleName);
					
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