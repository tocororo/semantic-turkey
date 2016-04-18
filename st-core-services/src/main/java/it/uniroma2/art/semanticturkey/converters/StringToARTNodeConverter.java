package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.converters.impl.STSpecificNodeChecks;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

@Deprecated
public class StringToARTNodeConverter implements Converter<String, ARTNode> {

	@Autowired
	private STServiceContext serviceContext;
	
	@Override
	public ARTNode convert(String NTTerm) {
		RDFModel model = serviceContext.getProject().getOntModel();
		try {
			ARTNode node = RDFNodeSerializer.createNode(model, NTTerm);
			STSpecificNodeChecks.checkURIResourceConstraints(node);
			return node;
		} catch (ModelAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
