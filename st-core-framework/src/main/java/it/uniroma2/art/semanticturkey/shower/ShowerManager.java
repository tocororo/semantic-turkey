package it.uniroma2.art.semanticturkey.shower;

import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.plugin.extpts.ShowerInterface;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

public class ShowerManager {
	
	private ShowerInterface showerImpl;


	public void setShowerImpl(ShowerInterface showerInterface){
		showerImpl = showerInterface;
	}
	
	
	
	public ShowerInterface getShowerImpl(String showerId){
		ShowerInterface shower = null;
		
		//get the installed Shower and return it
		
		//at the moment I don't know where to put the shower, maybe just as a simple OSGi (Felix) bundle
		shower = showerImpl;
		
		return shower;
	}
	
	
	/*public Response getResponseFromObject(Object object){
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse("testMethod",
				RepliesStatus.ok);
		if(object instanceof STRDFResource){
			STRDFResource strdfResource = (STRDFResource) object;
			RDFXMLHelp.addRDFNode(response, strdfResource);
		}
		
		return response;
	}*/
	
}
