package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

public class StringToARTResourceConverter implements Converter<String, ARTResource> {

	@Autowired
	private STServiceContext serviceContext;
	
	@Override
	public ARTResource convert(String NTTerm) {
		RDFModel model = serviceContext.getProject().getOntModel();
		try {
			return RDFNodeSerializer.createResource(model, NTTerm);
		} catch (ModelAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
