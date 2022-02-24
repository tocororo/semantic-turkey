package it.uniroma2.art.semanticturkey.services.core.resourceview;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.widgets.WidgetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProjectWidgetsManager implements ApplicationListener<ApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProjectWidgetsManager.class);

    @Autowired
    protected ExtensionPointManager exptManager;

    //Event handlers used to listens for project events, such as opening and closing
    private ProjectManager.ProjectEventHandler prjEventHandler;

    private ConcurrentHashMap<String, WidgetsManager> projWidgetMgrMap;

    public ProjectWidgetsManager() {
        projWidgetMgrMap = new ConcurrentHashMap<>();
    }

    public WidgetsManager getWidgetManager(Project project) {
        WidgetsManager wm = projWidgetMgrMap.get(project.getName());
        if (wm == null) {
            try {
                wm = new WidgetsManager(project, exptManager);
                projWidgetMgrMap.put(project.getName(), wm);
            } catch (STPropertyAccessException | NoSuchConfigurationManager e) {
                logger.error("Unable to initialize WidgetsManager for project: " + project.getName(), e);
            }
        }
        return wm;
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
            prjEventHandler = new ProjectManager.ProjectEventHandler() {

                @Override
                public void beforeProjectTearDown(Project project) {
                    unregisterProject(project);
                }

                @Override
                public void afterProjectInitialization(Project project) {
                    /* Do nothing. Here I could initialize the WidgetsManager for the given project, but since I need
                    to be sure that it is already initialized when it is required in StatementConsumerProvider,
                    I prefer to initialize it directly when requested (see getWidgetManager) */
                }
            };
            ProjectManager.registerProjectEventHandler(prjEventHandler);
        }
    }

    private void unregisterProject(Project project) {
        projWidgetMgrMap.remove(project.getName());
    }

}
