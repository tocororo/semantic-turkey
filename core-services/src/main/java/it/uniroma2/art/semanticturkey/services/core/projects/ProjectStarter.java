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

import javax.annotation.PostConstruct;
import java.util.Collection;

public class ProjectStarter {

	@PostConstruct
	public void openProjects()  {

		try {
			Collection<AbstractProject> abstractProjectCollection = ProjectManager
					.listProjects(ProjectConsumer.SYSTEM);
			for (AbstractProject abstractProject : abstractProjectCollection) {
				if (abstractProject instanceof Project) {
					Project project = (Project) abstractProject;
					boolean openAtStart = project.isOpenAtStartupEnabled();
					if (openAtStart) {
						ProjectManager.accessProject(ProjectConsumer.SYSTEM, project.getName(), ProjectACL.AccessLevel.R, ProjectACL.LockLevel.NO);
					}
				}
			}
		} catch (ProjectAccessException | InvalidProjectNameException | ProjectInexistentException | ForbiddenProjectAccessException | RBACException e) {
			// TODO decide what to do in this case, as it is now, if there is any problem in opening a project, then the other sequential projects
			// are not opened as well
		}
	}
}
