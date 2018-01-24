package it.uniroma2.art.semanticturkey.extension.impl.urigen.template;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.user.STUser;

public class NativeTemplateBasedURIGeneratorFactory implements NonConfigurableExtensionFactory<NativeTemplateBasedURIGenerator>, ProjectSettingsManager<NativeTemplateBasedURIGeneratorConfiguration> {

	@Override
	public Class<NativeTemplateBasedURIGenerator> getExtensionType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Reference> getSettingReferences(Project project, STUser user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NativeTemplateBasedURIGeneratorConfiguration getSetting(Reference reference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeSetting(Reference reference,
			NativeTemplateBasedURIGeneratorConfiguration configuration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<String> getProjectSettingsIdentifiers(Project project) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NativeTemplateBasedURIGeneratorConfiguration getProjectSettings(Project project,
			String identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeProjectSettings(Project project, String identifier,
			NativeTemplateBasedURIGeneratorConfiguration settings) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NativeTemplateBasedURIGenerator createInstance() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
