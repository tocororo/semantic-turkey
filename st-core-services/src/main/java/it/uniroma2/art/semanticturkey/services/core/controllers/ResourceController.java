package it.uniroma2.art.semanticturkey.services.core.controllers;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.services.core.Resource;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ResourceController implements ApplicationContextAware {
	ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		context = arg0;

	}

	@RequestMapping(value = "/Resource/getPropertyValues", method = RequestMethod.GET, produces = "application/xml")
	@ResponseBody
	public String getPropertyValuesPublished(
			@RequestParam(value = "subject") it.uniroma2.art.owlart.model.ARTResource subject,
			@RequestParam(value = "predicate") it.uniroma2.art.owlart.model.ARTURIResource predicate) throws ModelAccessException {
		Resource fun = (Resource) context.getBean(Resource.class);
		Collection<STRDFNode> response = fun.getPropertyValues(subject, predicate);

		//test to see the show
		for(STRDFNode strdfNode : response){
			System.out.println("&&&&&&&&& rendering = "+strdfNode.getRendering());
		}
		
		return response.toString();
	}
}