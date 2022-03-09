package it.uniroma2.art.semanticturkey.services.core.projects;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Collection;

public class ProjectStarter {

    protected static Logger logger = LoggerFactory.getLogger(ProjectStarter.class);

    @PostConstruct
    public void openProjects() throws RBACException, ProjectAccessException {

        Collection<AbstractProject> abstractProjectCollection = ProjectManager.listProjects(ProjectConsumer.SYSTEM);


        // first loop for the loadRBACProcessor
        for (AbstractProject abstractProject : abstractProjectCollection) {
            if (abstractProject instanceof Project) {
                Project project = (Project) abstractProject;
                /* initialize the RBAC processor of the project. This operation needs to be performed at
                system startup and not when the projects are accessed so that the auth evaluation can be performed
                also on closed project */
                logger.info("Initializing project " + project.getName() + " ...");
                RBACManager.loadRBACProcessor(project);
            }
        }

        //second loop to open projects that should be opened at startup
        for (AbstractProject abstractProject : abstractProjectCollection) {
            if (abstractProject instanceof Project) {
                Project project = (Project) abstractProject;
                boolean openAtStart = project.isOpenAtStartupEnabled();
                if (openAtStart) {
                    try {
                        logger.info("Opening project " + project.getName() + " ...");
                        ProjectManager.accessProject(ProjectConsumer.SYSTEM, project.getName(), ProjectACL.AccessLevel.R, ProjectACL.LockLevel.NO);
                    } catch (InvalidProjectNameException | ProjectInexistentException | ProjectAccessException | ForbiddenProjectAccessException e) {
                        // do nothing, so just skip this project and continue opening the other projects
                    }
                }
            }
        }
    }
}
