package it.uniroma2.art.semanticturkey.history;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SESAME;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;
import it.uniroma2.art.semanticturkey.vocabulary.STCHANGELOG;

public class OperationMetadata {
	private IRI userIRI;
	private IRI operationIRI;
	private IRI macroRole;
	private String[] parameterNames;
	private String[] parameterValues;

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

	public IRI getParameterIRI(int index) {
		return getParameterIRI(index, parameterNames[index]);
	}

	public IRI getParameterIRI(String name) {
		int index = ArrayUtils.indexOf(parameterNames, name);
		if (index == -1) {
			throw new IllegalArgumentException("Unknown parameter '" + name + "'");
		}
		return getParameterIRI(index, name);
	}

	private IRI getParameterIRI(int index, String name) {
		return SimpleValueFactory.getInstance()
				.createIRI(operationIRI.stringValue() + "/param-" + index + "-" + name);
	}

	public void setOperation(IRI operationIRI, String[] parameterNames, String[] parameterValues) {
		this.operationIRI = operationIRI;
		this.parameterNames = parameterNames;
		this.parameterValues = parameterValues;
	}

	public Model toRDF() {
		Model model = new LinkedHashModel();
		ValueFactory vf = SimpleValueFactory.getInstance();

		if (operationIRI != null) {
			model.add(CHANGETRACKER.COMMIT_METADATA, PROV.USED, operationIRI);

			BNode parametersResource = vf.createBNode();

			if (parameterNames != null) {
				model.add(CHANGETRACKER.COMMIT_METADATA, STCHANGELOG.PARAMETERS, parametersResource);
				model.add(parametersResource, RDF.TYPE, PROV.ENTITY);

				for (int i = 0; i < parameterNames.length; i++) {
					String pv = parameterValues[i];

					Value rdfPv;

					if (pv == null) {
						rdfPv = CHANGELOG.NULL;
					} else {
						rdfPv = vf.createLiteral(pv);
					}
					model.add(parametersResource, getParameterIRI(i), rdfPv);
				}
			}
		}

		if (userIRI != null) {
			BNode association = vf.createBNode();

			model.add(CHANGETRACKER.COMMIT_METADATA, PROV.QUALIFIED_ASSOCIATION, association);
			model.add(association, RDF.TYPE, PROV.ASSOCIATION);
			model.add(association, PROV.HAS_AGENT, userIRI);
			model.add(association, PROV.HAD_ROLE, macroRole);
		}

		return model;
	}

}
