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

public class URIGenerator {
	
	public enum RandCode {
		DATETIMEMS, UUID, TRUNCUUID8, TRUNCUUID12;
	}
	
	private static final String VALUE_REGEX = "[a-zA-Z0-9-_]*"; //regex for admitted value of placeholder
	private static final String RAND_REGEX = "rand\\(("+RandCode.DATETIMEMS+"|"+RandCode.UUID+"|"
			+RandCode.TRUNCUUID8+"|"+RandCode.TRUNCUUID12+")?\\)";
	/*
	 * this regex matches every string that contains one ${rand()} with an optional argument
	 * (datetimems, uuid, trunuuid8, truncuuid12). Before and after this part, eventually there could be some
	 * placeholders (${...}) or alphanumeric characters and _ character 
	 */
	private static final String TEMPLATE_REGEX = "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*"
			+ "\\$\\{"+ RAND_REGEX +"\\}"
			+ "([A-Za-z0-9_]*(\\$\\{[A-Za-z0-9]+\\})*[A-Za-z0-9_]*)*";
	
	private RDFModel model;
	private ARTResource[] graphs;
	private String projectName;
	
	public URIGenerator(RDFModel model, ARTResource[] graphs, String projectName){
		this.model = model;
		this.graphs = graphs;
		this.projectName = projectName;
	}
	
	/**
	 * This method generates a URI base on the template and the valueMapping provided.
	 * The valueMapping must contain key-value pairs that associates the placeholder used in the template
	 * with the values to assign to them
	 * @param template a regex-like expression where every placeholder is contained between ${ and }.
	 * A special placeholder is rand() that must be present into the template and it generates a random part.
	 * It can have an optional single argument <code>RandCode</code>. If this argument is not provided, is looked
	 * into the project property <code>uriRndCodeGenerator</code>. If the property is not found in the project,
	 * then a default is assumed (truncuuid8).<br/>
	 * e.g <code>xl_${lang}_${rand()}</code> will generate a
	 * @param valueMapping
	 * @return
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 * @throws IOException 
	 * @throws ModelAccessException 
	 */
	public ARTURIResAndRandomString generateURI(String template, Map<String, String> valueMapping) throws IOException, 
			InvalidProjectNameException, ProjectInexistentException, ModelAccessException {
		
		//validate template
		if (!template.matches(TEMPLATE_REGEX)){
			throw new IllegalArgumentException("The template \"" + template + "\" is not valid");
		}
		
		ARTURIResAndRandomString resRand = new ARTURIResAndRandomString();
		
		boolean newConceptGenerated = false;
		while(!newConceptGenerated){
			String localName = "";
			while (template.length() > 0){
				if (template.startsWith("${")){
					//get placeholder
					String ph = template.substring(template.indexOf("${")+2, template.indexOf("}"));
					//retrieve the value to replace the placeholder
					String value;
					if (ph.matches(RAND_REGEX)) {
						value = getRandomPart(ph);
						resRand.setRandomValue(value);
					} else {
						value = valueMapping.get(ph);
						if (value == null)
							throw new IllegalArgumentException("The placeholder \"" + ph + "\" is not present into the valueMapping");
						if (!value.matches(VALUE_REGEX))
							throw new IllegalArgumentException("The value \"" + value + "\" for the placeholder \"" + ph + "\" is not valid");
					}
					localName = localName + value; //compose the result
					template = template.substring(template.indexOf("}")+1);//remove the parsed part
				} else {
					//concat the fixed part of the template
					localName = localName + template.substring(0, template.indexOf("${"));
					template = template.substring(template.indexOf("${"));
				}
			}
			ARTURIResource uriRes = model.createURIResource(model.getDefaultNamespace() + localName);
			resRand.setArtURIResource(uriRes);
			
			if(!model.existsResource(uriRes, graphs)){
				newConceptGenerated = true;
			};
		}

		return resRand;
	}
	
	private String getRandomPart(String placheholder) {
		String DEFAULT_VALUE = RandCode.TRUNCUUID8.name();
		String randomCode = placheholder.substring(placheholder.indexOf("(")+1, placheholder.indexOf(")"));
		//if in the template there's no rand code, try to get it from project property
		if (randomCode.length() == 0){
			try {
				randomCode = ProjectManager.getProjectProperty(projectName, "uriRndCodeGenerator");
			} catch (IOException | InvalidProjectNameException | ProjectInexistentException e) {}
		}
		//If the property is not found in the project truncuuid8 is assumed as default
		if (randomCode == null){
			randomCode = DEFAULT_VALUE;
		}
		String randomValue;
		if(randomCode.equalsIgnoreCase(RandCode.DATETIMEMS.name())){
			randomValue = new java.util.Date().getTime()+"";
		} else if(randomCode.equalsIgnoreCase(RandCode.UUID.name())){
			randomValue = UUID.randomUUID().toString();
		} else if(randomCode.equalsIgnoreCase(RandCode.TRUNCUUID12.name())){
			randomValue = UUID.randomUUID().toString().substring(0, 13);
		} else {//default value truncuuid8
			randomValue = UUID.randomUUID().toString().substring(0, 8);
		}
		return randomValue;
	}

}
