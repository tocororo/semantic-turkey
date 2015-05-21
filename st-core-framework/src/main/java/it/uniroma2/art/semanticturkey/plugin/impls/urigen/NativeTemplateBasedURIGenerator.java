package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.data.id.ARTURIResAndRandomString;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the {@link URIGenerator} extension point based on templates. 
 * 
 */
public class NativeTemplateBasedURIGenerator implements URIGenerator {

	public enum RandCode {
		DATETIMEMS, UUID, TRUNCUUID4, TRUNCUUID8, TRUNCUUID12;
	}

	private static final String VALUE_REGEX = "[a-zA-Z0-9-_]*"; // regex for
																// admitted
																// value of
																// placeholder
	private static final String RAND_REGEX = "rand\\((" + RandCode.DATETIMEMS
			+ "|" + RandCode.UUID + "|" + RandCode.TRUNCUUID4 + "|"
			+ RandCode.TRUNCUUID8 + "|" + RandCode.TRUNCUUID12 + ")?\\)";
	/*
	 * this regex matches every string that contains one ${rand()} with an
	 * optional argument (datetimems, uuid, truncuuid4, truncuuid8,
	 * truncuuid12). Before and after this part, eventually there could be some
	 * placeholders (${...}) or alphanumeric characters and _ character
	 */
	private static final String TEMPLATE_REGEX = "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*"
			+ "\\$\\{"
			+ RAND_REGEX
			+ "\\}"
			+ "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*";

	private NativeTemplateBasedURIGeneratorConfiguration conf;

	public NativeTemplateBasedURIGenerator(NativeTemplateBasedURIGeneratorConfiguration conf) {
		this.conf = conf;
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator#generateURI(it.uniroma2.art.semanticturkey.services.STServiceContext, java.lang.String, java.util.Map)
	 */
	@Override
	public ARTURIResAndRandomString generateURI(STServiceContext stServiceContext, String xRole,
			Map<String, String> args) throws URIGenerationException {
		
		String template = conf.fallback;
		
		if (xRole.equals("xLabel")) {
			template = conf.xLabel;
		} else if (xRole.equals("concept")) {
			template = conf.concept;
		}
				
		// validate template
		if (!template.matches(TEMPLATE_REGEX)) {
			throw new IllegalArgumentException("The template \"" + template
					+ "\" is not valid");
		}

		ARTURIResAndRandomString resRand = new ARTURIResAndRandomString();

		boolean newConceptGenerated = false;
		while (!newConceptGenerated) {
			String localName = "";
			String currentTemplate = template;
			while (currentTemplate.length() > 0) {
				if (currentTemplate.startsWith("${")) {
					// get placeholder
					String ph = currentTemplate.substring(
							currentTemplate.indexOf("${") + 2,
							currentTemplate.indexOf("}"));
					// retrieve the value to replace the placeholder
					String value;
					if (ph.matches(RAND_REGEX)) {
						value = getRandomPart(stServiceContext, ph);
						resRand.setRandomValue(value);
					} else {
						value = args.get(ph);
						if (value == null)
							throw new IllegalArgumentException(
									"The placeholder \""
											+ ph
											+ "\" is not present into the valueMapping");
						if (!value.matches(VALUE_REGEX))
							throw new IllegalArgumentException("The value \""
									+ value + "\" for the placeholder \"" + ph
									+ "\" is not valid");
					}
					localName = localName + value; // compose the result
					// remove the parsed part
					currentTemplate = currentTemplate.substring(currentTemplate
							.indexOf("}") + 1);
				} else {
					// concat the fixed part of the template
					localName = localName
							+ currentTemplate.substring(0,
									currentTemplate.indexOf("${"));
					currentTemplate = currentTemplate.substring(currentTemplate
							.indexOf("${"));
				}
			}

			RDFModel model = stServiceContext.getProject().getOntModel();
			ARTResource[] graphs = stServiceContext.getRGraphs();
			ARTURIResource uriRes = model.createURIResource(model
					.getDefaultNamespace() + localName);
			resRand.setArtURIResource(uriRes);

			try {
				if (!model.existsResource(uriRes, graphs)) {
					newConceptGenerated = true;
				}
			} catch (ModelAccessException e) {
				throw new URIGenerationException(e);
			}
		}
		
		return resRand;
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
}
