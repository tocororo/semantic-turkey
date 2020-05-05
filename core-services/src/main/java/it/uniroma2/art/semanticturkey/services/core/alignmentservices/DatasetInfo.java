package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;

public class DatasetInfo {
	private String projectName;
	private IRI datasetIRI;
	private String baseURI;
	private IRI model;
	private IRI lexicalizationModel;
	private boolean open;

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public IRI getDatasetIRI() {
		return datasetIRI;
	}

	public void setDatasetIRI(IRI datasetIRI) {
		this.datasetIRI = datasetIRI;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	public IRI getModel() {
		return model;
	}

	public void setModel(IRI model) {
		this.model = model;
	}

	public IRI getLexicalizationModel() {
		return lexicalizationModel;
	}

	public void setLexicalizationModel(IRI lexicalizationModel) {
		this.lexicalizationModel = lexicalizationModel;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public static DatasetInfo valueOf(Project project, IRI datasetIRI) {
		DatasetInfo rv = new DatasetInfo();
		rv.setProjectName(project.getName());
		rv.setBaseURI(project.getBaseURI());
		rv.setDatasetIRI(datasetIRI);
		rv.setLexicalizationModel(project.getLexicalizationModel());
		rv.setModel(project.getModel());
		rv.setOpen(ProjectManager.isOpen(project));
		return rv;
	}

}
