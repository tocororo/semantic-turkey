package it.uniroma2.art.semanticturkey.services.core;

import org.openrdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

/**
 * This class provides services for manipulating SKOS constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@GenerateSTServiceController
@Validated
@Component
public class SKOS {
	
	private static Logger logger = LoggerFactory.getLogger(SKOS.class);
	
	@GenerateSTServiceController	
	public Response getNarrowerConcepts(@LocallyDefined Resource resource) {
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("getNarrowerConcept", RepliesStatus.ok);
				
		return response;
	}
}
