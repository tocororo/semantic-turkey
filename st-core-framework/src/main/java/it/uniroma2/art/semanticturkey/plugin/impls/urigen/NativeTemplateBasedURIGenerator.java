package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Objects;
import com.google.common.net.UrlEscapers;


/**
 * Implementation of the {@link URIGenerator} extension point based on templates. 
 * 
 */
public class NativeTemplateBasedURIGenerator implements URIGenerator {

	public enum RandCode {
		DATETIMEMS, UUID, TRUNCUUID4, TRUNCUUID8, TRUNCUUID12;
	}

//	private static final String VALUE_REGEX = "[a-zA-Z0-9-_]*"; // regex for
//																// admitted
//																// value of
//																// placeholder
	private static final String RAND_REGEX = "rand\\((" + RandCode.DATETIMEMS
			+ "|" + RandCode.UUID + "|" + RandCode.TRUNCUUID4 + "|"
			+ RandCode.TRUNCUUID8 + "|" + RandCode.TRUNCUUID12 + ")?\\)";
	/*
	 * this regex matches every string that contains one ${rand()} with an
	 * optional argument (datetimems, uuid, truncuuid4, truncuuid8,
	 * truncuuid12). Before and after this part, eventually there could be some
	 * placeholders (${...}) or alphanumeric characters and _ character
	 */
//	private static final String TEMPLATE_REGEX = "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*"
//			+ "\\$\\{"
//			+ RAND_REGEX
//			+ "\\}"
//			+ "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*";

	private static final String XROLE = "xRole";
	
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("([a-zA-Z]+)(?:\\.(([a-zA-Z]+)))?");
	private static final Pattern PLACEHOLDER_START_PATTERN = Pattern.compile("\\$(\\$)?\\{");

	private NativeTemplateBasedURIGeneratorConfiguration conf;

	public NativeTemplateBasedURIGenerator(NativeTemplateBasedURIGeneratorConfiguration conf) {
		this.conf = conf;
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator#generateURI(it.uniroma2.art.semanticturkey.services.STServiceContext, java.lang.String, java.util.Map)
	 */
	@Override
	public ARTURIResource generateURI(STServiceContext stServiceContext, String xRole,
			Map<String, ARTNode> args) throws URIGenerationException {
		
		String template = conf.fallback;
		
		if (xRole.equals("xLabel")) {
			template = conf.xLabel;
		} else if (xRole.equals("concept")) {
			template = conf.concept;
		} else if(xRole.equals("xDefinition")){
			template = conf.xDefinition;
		}
				
//		// validate template
//		if (!template.matches(TEMPLATE_REGEX)) {
//			throw new IllegalArgumentException("The template \"" + template
//					+ "\" is not valid");
//		}

		ARTURIResource uriRes = null;

		boolean newConceptGenerated = false;
		while (!newConceptGenerated) {
			String localName = "";
			String currentTemplate = template;
			while (currentTemplate.length() > 0) {
				// S{ is an escaped placeholder, $${ is a not escaped placeholder
				if (currentTemplate.startsWith("${") || currentTemplate.startsWith("$${")) {
					// get placeholder
					// If ${, then escaped with 2 characters to be skipped
					boolean phEscaped = true;
					int phBegin = 2;
					
					// If $${, then not escaped with 3 characters to be skipped
					if (currentTemplate.startsWith("$${")) {
						phEscaped = false;
						phBegin = 3;
					}
					
					int phEnd = currentTemplate.indexOf("}");
					
					if (phEnd == -1) {
						throw new IllegalArgumentException("Missing closing brace");
					}
					
					String ph = currentTemplate.substring(phBegin, phEnd);
					
					// retrieve the value to replace the placeholder
					String value;
					if (ph.matches(RAND_REGEX)) {
						value = getRandomPart(stServiceContext, ph);
					} else {
						if(ph.equals(XROLE)){
							value = xRole;
						}
						else {
							value = getPlaceholderValue(ph, args);
						}
						if (value == null)
							throw new IllegalArgumentException(
									"The placeholder \""
											+ ph
											+ "\" is not present into the valueMapping");
//						if (!value.matches(VALUE_REGEX))
//							throw new IllegalArgumentException("The value \""
//									+ value + "\" for the placeholder \"" + ph
//									+ "\" is not valid");
					}
					
					if (phEscaped) {
						value = escapeValue(value);
					}
					localName = localName + value; // compose the result
					// remove the parsed part
					currentTemplate = currentTemplate.substring(phEnd + 1);
				} else {
					// concat the fixed part of the template
					Matcher m = PLACEHOLDER_START_PATTERN.matcher(currentTemplate);
					
					int literalEnd;
					
					if (m.find()) {
						literalEnd = m.start();
					} else {
						literalEnd = currentTemplate.length();
					}
					
					localName = localName
							+ currentTemplate.substring(0,literalEnd);
					currentTemplate = currentTemplate.substring(literalEnd);
				}
			}

			RDFModel model = stServiceContext.getProject().getOntModel();
			ARTResource[] graphs = stServiceContext.getRGraphs();
			uriRes = model.createURIResource(model
					.getDefaultNamespace() + localName);

			try {
				if (!model.existsResource(uriRes, graphs)) {
					newConceptGenerated = true;
				}
			} catch (ModelAccessException e) {
				throw new URIGenerationException(e);
			}
		}
		
		return uriRes;
	}

	private String escapeValue(String rawString) {
		return UrlEscapers.urlPathSegmentEscaper().escape(rawString.trim().replaceAll("\\s+", "_"));
	}

	private String getRandomPart(STServiceContext stServiceContext, String placheholder) {
		String DEFAULT_VALUE = RandCode.TRUNCUUID8.name();

		String randomCode = placheholder.substring(
				placheholder.indexOf("(") + 1, placheholder.indexOf(")"));
		// if in the template there's no rand code, try to get it from project
		// property
		if (randomCode.length() == 0) {
			try {
				randomCode = ProjectManager.getProjectProperty(stServiceContext.getProject().getName(), "uriRndCodeGenerator");
			} catch (IOException | InvalidProjectNameException | ProjectInexistentException e) {
			}
		}
		// If the property is not found in the project truncuuid8 is assumed as default
		if (randomCode == null) {
			randomCode = DEFAULT_VALUE;
		}

		String randomValue;
		if (randomCode.equalsIgnoreCase(RandCode.DATETIMEMS.name())) {
			randomValue = new java.util.Date().getTime() + "";
		} else if (randomCode.equalsIgnoreCase(RandCode.UUID.name())) {
			randomValue = UUID.randomUUID().toString();
		} else if (randomCode.equalsIgnoreCase(RandCode.TRUNCUUID4.name())) {
			randomValue = UUID.randomUUID().toString().substring(0, 4);
		} else if (randomCode.equalsIgnoreCase(RandCode.TRUNCUUID12.name())) {
			randomValue = UUID.randomUUID().toString().substring(0, 13);
		} else {// default value TRUNCUUID8
			randomValue = UUID.randomUUID().toString().substring(0, 8);
		}
		return randomValue;
	}
	
	private String getPlaceholderValue(String ph, Map<String, ARTNode> args) {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(ph);
		if (!matcher.matches())
			return null;

		String firstLevel = matcher.group(1);
		String secondLevel = matcher.group(2);

		ARTNode firstLevelObj = args.get(firstLevel);
		
		if (firstLevelObj == null)
			return null;

		if (secondLevel == null)
			return object2string(firstLevelObj);

		try {
			Method[] methods = firstLevelObj.getClass().getMethods();
			
			String nameWithHas = "get" + Character.toUpperCase(secondLevel.charAt(0)) + secondLevel.substring(1);
			
			Method methodWithLiteralName = null;
			Method methodWithHasName = null;

			for (Method m : methods) {
				if (m.getParameterTypes().length != 0) continue;
				
				String methodName = m.getName();
				if (Objects.equal(methodName, secondLevel)) {
					methodWithLiteralName = m;
					break;
				} else if (Objects.equal(methodName, nameWithHas)) {
					methodWithHasName = m;
				}
			}
			
			Method m = methodWithLiteralName != null ? methodWithLiteralName : methodWithHasName;
			
			if (m == null) return null;
			
			Object secondLevelObject = m.invoke(firstLevelObj);

			return object2string(secondLevelObject);
		} catch (SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			return null;
		}
	}

	private String object2string(Object obj) {
		String rawString = null;
		if (obj instanceof ARTNode) {
			ARTNode artNode = (ARTNode)obj;
			if (artNode.isURIResource()) {
				rawString = artNode.asURIResource().getLocalName();
			} else if (artNode.isLiteral()) {
				rawString = artNode.asLiteral().getLabel();
			} else {
				rawString =  artNode.getNominalValue();
			}
		} else if (obj != null) {
			rawString = obj.toString();
		} else {
			return null;
		}
		
		return rawString;
	}
}
