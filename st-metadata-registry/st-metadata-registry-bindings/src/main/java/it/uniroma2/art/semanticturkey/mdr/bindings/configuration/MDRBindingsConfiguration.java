package it.uniroma2.art.semanticturkey.mdr.bindings.configuration;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.osgi.extensions.annotation.ServiceReference;

import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.bindings.impl.STMetadataRegistryBackendImpl;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryCreationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Config;

@Configuration
public class MDRBindingsConfiguration
		implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private MediationFramework mediationFramework;
	private ExtensionPointManager exptManager;
	private ProjectManager.ProjectEventHandler prjEventHandler;

	@ServiceReference
	public void setMediationFramework(MediationFramework mediationFramework) {
		this.mediationFramework = mediationFramework;
	}

	@ServiceReference
	public void setExptManager(ExtensionPointManager exptManager) {
		this.exptManager = exptManager;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean(name = "stMetadataRegistryBackend")
	public STMetadataRegistryBackend createSTMetadataRegistryBackend()
			throws MetadataRegistryCreationException {
		return new STMetadataRegistryBackendImpl(Config.getDataDir(), mediationFramework, exptManager);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent
				|| event instanceof ContextRefreshedEvent) {
			if (prjEventHandler != null) {
				ProjectManager.unregisterProjectEventHandler(prjEventHandler);
				prjEventHandler = null;
			}
		}

		if (event instanceof ContextStartedEvent || event instanceof ContextRefreshedEvent) {
			STMetadataRegistryBackend mdr = applicationContext.getBean(STMetadataRegistryBackend.class);
			prjEventHandler = new ProjectManager.ProjectEventHandler() {

				@Override
				public void beforeProjectTearDown(Project project) {
					mdr.unregisterProject(project);
				}

				@Override
				public void afterProjectInitialization(Project project) {
					mdr.registerProject(project);
				}
			};
			ProjectManager.registerProjectEventHandler(prjEventHandler);
		}
	}

}
