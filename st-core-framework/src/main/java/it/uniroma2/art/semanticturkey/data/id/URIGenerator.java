package it.uniroma2.art.semanticturkey.data.id;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.ProjectManager;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 
 * @deprecated use instead the extension point {@link it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator}
 */
@Deprecated
public class URIGenerator {

	public enum RandCode {
		DATETIMEMS, UUID, TRUNCUUID4, TRUNCUUID8, TRUNCUUID12;
	}

	private static final String VALUE_REGEX = "[a-zA-Z0-9-_]*"; // regex for admitted value of placeholder
	private static final String RAND_REGEX = "rand\\((" + RandCode.DATETIMEMS + "|" + RandCode.UUID + "|"
			+ RandCode.TRUNCUUID4 + "|" + RandCode.TRUNCUUID8 + "|" + RandCode.TRUNCUUID12 + ")?\\)";
	/*
	 * this regex matches every string that contains one ${rand()} with an optional argument (datetimems,
	 * uuid, truncuuid4, truncuuid8, truncuuid12). Before and after this part, eventually there could be some
	 * placeholders (${...}) or alphanumeric characters and _ character
	 */
	private static final String TEMPLATE_REGEX = "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*"
			+ "\\$\\{" + RAND_REGEX + "\\}" + "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*";

	private RDFModel model;
	private ARTResource[] graphs;
	private String projectName;

	public URIGenerator(RDFModel model, ARTResource[] graphs, String projectName) {
		this.model = model;
		this.graphs = graphs;
		this.projectName = projectName;
	}

	/**
	 * This method generates a URI base on the template and the valueMapping provided.<br/>
	 * The valueMapping must contain key-value pairs that associate the placeholder used in the template with
	 * the values to assign to them
	 * 
	 * @param template
	 *            a regex expression with optional (defineable) placeholders contained between ${ and }. The
	 *            only mandatory (and provided by default) placeholder is rand(), which generates a random
	 *            code for the URI local name. rand() can have an optional single argument
	 *            <code>RandCode</code>.<br/>
	 *            RandCode can be one of:
	 *            <ul>
	 *            <li>DATETIMEMS: uses the current time in MS for generating the ID</li>
	 *            <li>UUID: generates a random UUID</li>
	 *            <li>TRUNCUUID4: generates a random UUID and then truncates up to the first 4 chars</li>
	 *            <li>TRUNCUUID8: generates a random UUID and then truncates up to the first 8 chars (first
	 *            section of the UUID before the hyphen)</li>
	 *            <li>TRUNCUUID12: generates a random UUID and then truncates up to the first 12 chars
	 *            (including the hyphen)</li>
	 *            </ul>
	 *            If this argument is not provided explcitly between the round brackets of rand(), it is
	 *            looked up on the project property <code>uriRndCodeGenerator</code>.<br/>
	 *            If that property is not found in the project, then a default is assumed (TRUNCUUID8).<br/>
	 *            e.g. <code>c_${rand(TRUNCUUID4)}</code> will generate resource localnames such as c_47d3
	 * @param valueMapping
	 *            can be used to define new placeholders, by associating them to values computed outside of
	 *            the regexp. For instance, in the case of SKOSXL labels, one might want to add a lang placeholder filled with the value of the literalform of the xlabel<br/>
	 *            e.g <code>xl_${lang}_${rand()}</code> will generate skosxl labels such as: xl_en_4f56ed21
	 * @return
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws IOException
	 * @throws ModelAccessException
	 */
	public ARTURIResAndRandomString generateURI(String template, Map<String, String> valueMapping)
			throws ModelAccessException {

		// validate template
		if (!template.matches(TEMPLATE_REGEX)) {
			throw new IllegalArgumentException("The template \"" + template + "\" is not valid");
		}

		ARTURIResAndRandomString resRand = new ARTURIResAndRandomString();

		boolean newConceptGenerated = false;
		while (!newConceptGenerated) {
			String localName = "";
			String currentTemplate = template;
			while (currentTemplate.length() > 0) {
				if (currentTemplate.startsWith("${")) {
					// get placeholder
					String ph = currentTemplate.substring(currentTemplate.indexOf("${") + 2,
							currentTemplate.indexOf("}"));
					// retrieve the value to replace the placeholder
					String value;
					if (ph.matches(RAND_REGEX)) {
						value = getRandomPart(ph);
						resRand.setRandomValue(value);
					} else {
						value = valueMapping.get(ph);
						if (value == null)
							throw new IllegalArgumentException("The placeholder \"" + ph
									+ "\" is not present into the valueMapping");
						if (!value.matches(VALUE_REGEX))
							throw new IllegalArgumentException("The value \"" + value
									+ "\" for the placeholder \"" + ph + "\" is not valid");
					}
					localName = localName + value; // compose the result
					// remove the parsed part
					currentTemplate = currentTemplate.substring(currentTemplate.indexOf("}") + 1);
				} else {
					// concat the fixed part of the template
					localName = localName + currentTemplate.substring(0, currentTemplate.indexOf("${"));
					currentTemplate = currentTemplate.substring(currentTemplate.indexOf("${"));
				}
			}
			ARTURIResource uriRes = model.createURIResource(model.getDefaultNamespace() + localName);
			resRand.setArtURIResource(uriRes);

			if (!model.existsResource(uriRes, graphs)) {
				newConceptGenerated = true;
			}
			;
		}

		return resRand;
	}

	private String getRandomPart(String placheholder) {
		String DEFAULT_VALUE = RandCode.TRUNCUUID8.name();
		String randomCode = placheholder.substring(placheholder.indexOf("(") + 1, placheholder.indexOf(")"));
		// if in the template there's no rand code, try to get it from project property
		if (randomCode.length() == 0) {
			try {
				randomCode = ProjectManager.getProjectProperty(projectName, "uriRndCodeGenerator");
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
