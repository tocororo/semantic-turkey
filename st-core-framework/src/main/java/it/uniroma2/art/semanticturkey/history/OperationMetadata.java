package it.uniroma2.art.semanticturkey.history;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;

public class OperationMetadata {
	private IRI userIRI;
	private IRI operationIRI;
	private IRI macroRole;
	private Resource subject;

	public IRI getUserIRI() {
		return userIRI;
	}

	public void setUserIRI(IRI userIRI, IRI macroRole) {
		this.userIRI = userIRI;
		this.macroRole = macroRole;
	}

	public IRI getOperationIRI() {
		return operationIRI;
	}

	public void setOperationIRI(IRI operationIRI) {
		this.operationIRI = operationIRI;
	}

	public void setSubject(Resource subject) {
		this.subject = subject;
	}
	
	public Model toRDF() {
		Model model = new LinkedHashModel();
		if (operationIRI != null) {
			model.add(CHANGETRACKER.COMMIT_METADATA, PROV.USED, operationIRI);
		}
		
		if (userIRI != null) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			BNode association = vf.createBNode();
			
			model.add(CHANGETRACKER.COMMIT_METADATA, PROV.QUALIFIED_ASSOCIATION, association);
			model.add(association, RDF.TYPE, PROV.ASSOCIATION);
			model.add(association, PROV.HAS_AGENT, userIRI);
			model.add(association, PROV.HAD_ROLE, macroRole);
		}
		
		if (subject != null) {
			model.add(CHANGETRACKER.COMMIT_METADATA, DCTERMS.SUBJECT, subject);
		}
		return model;
	}

}
