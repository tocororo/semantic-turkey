package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.core.convert.converter.Converter;

import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.UnknownResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;

public class StringToResourcePositionConverter implements Converter<String, ResourcePosition> {

	public static final String LOCAL_PREFIX = "local:";
	public static final String REMOTE_PREFIX = "remote:";
	public static final String UNKNOWN_PREFIX = "unknown:";

	private DatasetMetadataRepository datasetMetadataRepository;
	
	public StringToResourcePositionConverter(DatasetMetadataRepository datasetMetadataRepository) {
		this.datasetMetadataRepository = datasetMetadataRepository;
	}
	
	@Override
	public ResourcePosition convert(String resourcePositionString) {
		if (resourcePositionString.startsWith(LOCAL_PREFIX)) {
			String projectName = resourcePositionString.substring(LOCAL_PREFIX.length());
			Project project = ProjectManager.getProject(projectName);
			
			if (project == null) {
				throw new IllegalArgumentException(String.format("The project used in a local resource position does not exist: %s", projectName));
			}
			return new LocalResourcePosition(project);
		} else if (resourcePositionString.startsWith(REMOTE_PREFIX)) {
			String datasetId = resourcePositionString.substring(REMOTE_PREFIX.length());
			//DatasetMetadata meta = datasetMetadataRepository.findDatasetForResource(VocabUtilities.nodeFactory.createURIResource(datasetId));
			//DatasetMetadata meta = datasetMetadataRepository.findDatasetForResource(RDF4JMigrationUtils.convert2art(SimpleValueFactory.getInstance().createIRI(datasetId)));
			DatasetMetadata meta = datasetMetadataRepository.findDatasetForResource(SimpleValueFactory.getInstance().createIRI(datasetId));
			
			if (meta == null) {
				throw new IllegalArgumentException(String.format("The dataset mentioned in a remote resource position is not known: %s", datasetId));
			}
			return new RemoteResourcePosition(meta);
		} else if (resourcePositionString.startsWith(UNKNOWN_PREFIX)) {
			return new UnknownResourcePosition();
		} else {
			throw new IllegalArgumentException(String.format("Cannot recognize a valid prefix in the given string serialization of a resource position", resourcePositionString));
		}
	}

}
