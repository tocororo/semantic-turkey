package it.uniroma2.art.semanticturkey.customviews;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
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
public class ProjectCustomViewsManager implements ApplicationListener<ApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCustomViewsManager.class);

    @Autowired
    protected ExtensionPointManager exptManager;

    //Event handlers used to listens for project events, such as opening and closing
    private ProjectManager.ProjectEventHandler prjEventHandler;

    private ConcurrentHashMap<String, CustomViewsManager> projCvMgrMap;

    public ProjectCustomViewsManager() {
        projCvMgrMap = new ConcurrentHashMap<>();
    }

    public CustomViewsManager getCustomViewManager(Project project) {
        CustomViewsManager wm = projCvMgrMap.get(project.getName());
        if (wm == null) {
            try {
                wm = new CustomViewsManager(project, exptManager);
                projCvMgrMap.put(project.getName(), wm);
            } catch (STPropertyAccessException | NoSuchConfigurationManager e) {
                logger.error("Unable to initialize CustomViewsManager for project: " + project.getName(), e);
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
                    /* Do nothing. Here I could initialize the CustomViewsManager for the given project,
                    but I prefer to initialize it on demand when requested (see getCustomViewManager) */
                }
            };
            ProjectManager.registerProjectEventHandler(prjEventHandler);
        }
    }

    private void unregisterProject(Project project) {
        projCvMgrMap.remove(project.getName());
    }

}
