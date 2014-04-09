package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.servlet.Response;

import java.io.IOException;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class ShowerAdapter implements ShowerInterface {

	@Autowired
	protected STServiceContext serviceContext;
	
	protected String id;

	public ShowerAdapter(String id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.semanticturkey.plugin.extpts.ServiceInterface#getId()
	 */
	public String getId() {
		return id;
	}

	@Override
	public void apply(Object object) {

		try {
			// get the right Shower from the right project (the current one at the moment)
			Project<? extends RDFModel> currentProject = serviceContext.getProject();
			ProjectManager.getProjectProperty(currentProject.getName(), "shower");

			// now it create a Response, later it will call the Marshaller
			if (object instanceof Iterable) {
				Iterator iter = ((Iterable) object).iterator();
				while (iter.hasNext()) {
					Object singleObj = iter.next();
					apply(singleObj);

				}
			} else if(object instanceof STRDFResource){
				renderResource((STRDFResource)object);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidProjectNameException e) {
			e.printStackTrace();
		} catch (ProjectInexistentException e) {
			e.printStackTrace();
		}

	}

	protected abstract  void renderResource(STRDFResource strdfResource);
		

}
