package it.uniroma2.art.semanticturkey.shower.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.plugin.extpts.ShowerAdapter;

public class ShowerLabels extends ShowerAdapter {

	@Autowired
	public ShowerLabels(@Value("id") String id) {
		super(id);
	}

	@Override
	public void renderResource(STRDFResource strdfResource) {
		String show="Test Show for : "+strdfResource.getARTNode().getNominalValue();
		ARTResource a;
		strdfResource.setRendering(show);
	}

	

}
