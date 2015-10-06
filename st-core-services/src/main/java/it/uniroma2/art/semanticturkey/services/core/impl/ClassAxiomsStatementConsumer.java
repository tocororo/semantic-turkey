package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFS;

public class ClassAxiomsStatementConsumer extends MultiPropertyMatchingAbstractStatementConsumer {

	public static final ARTURIResource[] relevantProperties = {
		OWL.Res.EQUIVALENTCLASS,
		RDFS.Res.SUBCLASSOF,
		OWL.Res.DISJOINTWITH,
		// OWL.Res.HASKEY,
		OWL.Res.COMPLEMENTOF,
		OWL.Res.INTERSECTIONOF,
		//OWL.Res.DISJOINTUNIONOF,
		OWL.Res.ONEOF,
		OWL.Res.UNIONOF
	};
	
	public ClassAxiomsStatementConsumer() {
		super("classaxioms", relevantProperties);
	}

}
