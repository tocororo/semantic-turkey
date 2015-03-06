package it.uniroma2.art.semanticturkey.services.core;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;

import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.PRParserException;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.conf.ModelConfiguration;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeCODAManager;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntry;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;

import org.apache.uima.UIMAException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@GenerateSTServiceController
@Validated
@Component
public class CustomRanges extends STServiceAdapter {
	
	@Autowired
	private ObjectFactory<CustomRangeCODAManager> crCODAMgrProvider;
	
	@Autowired
	private ServletRequest request;
	
	/**
	 * This service get as parameters a custom range id and a set of userPrompt key-value pairs
	 * (userPrompt are unknown a priori, so pairs are dynamic and have to be get from the request),
	 * then run CODA on the pearl specified in the CustomRangeEntry (with the given id 
	 * <code>crEntryId</code>) and the features filled following the userPrompt parameters.
	 *  
	 * @param crEntryId
	 * @return
	 */
	@GenerateSTServiceController
	public Response runCoda(String crEntryId) { //TODO: gestione eccezioni? catturare o propagare?
		//get the parameters to put in the userPromptMap from the request
		Map<String, String[]> parMap = request.getParameterMap();//the others params (form key and values) are dynamic, get it directly from request
		Map<String, String> userPromptMap = new HashMap<String, String>();
		for (Entry<String, String[]> par : parMap.entrySet()){
//			System.out.println("param: " + par.getKey() + ", value: " + par.getValue()[0]);
			userPromptMap.put(par.getKey(), par.getValue()[0]);
		}
		//Remove useless parameters for fillMapAndAddTriples method (parameters not belonging to userPrompt feature)
		/* N.B. if some other parameters will be put in this map, there should be no problem since
		 * when this map will be used to valorize the CAS, the value will be get based on the feature in the TSD. */
		userPromptMap.remove("ctx_project");
		userPromptMap.remove("crEntryId");
		for (Entry<String, String> e : userPromptMap.entrySet()){
			System.out.println("userPrompt: " + e.getKey() + " = " + e.getValue());
		}
		try {
			CustomRangeCODAManager crCodaMgr = crCODAMgrProvider.getObject();
			CustomRangeEntry crEntry = new CustomRangeEntry(crEntryId);
			RDFModel rdfModel = getOWLModel();
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
			crCodaMgr.fillMapAndAddTriples(rdfModel, ontFact, userPromptMap, crEntry);//par map has more entry than necessary (ctx_project at least, but it should be ignored in fillMap...)
		} catch (FileNotFoundException | UIMAException | PRParserException | ComponentProvisioningException | 
				ConverterException | UnsupportedQueryLanguageException | ModelAccessException |
				MalformedQueryException | QueryEvaluationException | DependencyException | 
				UnavailableResourceException | ProjectInconsistentException e) {
			e.printStackTrace();
		}
		
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("test",RepliesStatus.ok);
		return response;
	}

}
