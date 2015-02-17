package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.converters.impl.STSpecificNodeChecks;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

public class StringToARTURIResourceConverter implements Converter<String, ARTURIResource> {

	@Autowired
	private STServiceContext serviceContext;
	
	@Override
	public ARTURIResource convert(String NTTerm) {
		RDFModel model = serviceContext.getProject().getOntModel();
		try {
			ARTURIResource uriResource = RDFNodeSerializer.createURI(model, NTTerm);
			STSpecificNodeChecks.checkURIResourceConstraints(uriResource);
			return uriResource;
		} catch (ModelAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
