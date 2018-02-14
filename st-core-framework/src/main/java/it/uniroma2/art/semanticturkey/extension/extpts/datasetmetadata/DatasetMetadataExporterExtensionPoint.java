package it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class DatasetMetadataExporterExtensionPoint
		implements ExtensionPoint, ProjectSettingsManager<DatasetMetadataExporterSettings> {

	@Override
	public Class<?> getInterface() {
		return DatasetMetadataExporter.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}

}
