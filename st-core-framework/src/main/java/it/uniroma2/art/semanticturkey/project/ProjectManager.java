/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.project;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerConfig;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectDeletionException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectIncompatibleException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedModelException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.ontology.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectGroupBindingsManager;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

/**
 * <p>
 * a manager/factory class for creating new projects, for retrieving existing ones or for accessing the loaded
 * projects
 * <p/>
 * 
 * <p>
 * Apart from project factoring, The ProjectManager allows to specify the ACL (Access Control List) and to
 * administer the online status of each project.
 * </p>
 * <p>
 * The ACL is described by the {@link ProjectACL} class, and provides information about which
 * {@link ProjectConsumer}s can access to this project, and which grants they have. Specifically, for each
 * project, it contains the following descriptions:
 * <ul>
 * <li>a list of {@link ProjectConsumer}s, together with their access permissions {@link AccessLevel}.</li>
 * <li>a "lockable" property, telling if the project associated to this ACL, can be locked for use by a
 * {@link ProjectConsumer}, and by which modality</li>
 * </ul>
 * </p>
 * <p>
 * the online status of each project reports:
 * <ul>
 * <li>the list of its consumers, together with the {@link AccessLevel for each of them}</li>
 * <li>the {@link LockStatus}, which is represented by a &lt;{@link LockLevel},
 * {@link ProjectConsumer}&rt;</li>
 * </ul>
 * </p>
 * 
 * TODO We should split ProjectManager into two classes: the real ProjectManager, and a ProjectFactory.
 * ProjectManager should not deal with things like having "hands-in-the-details" of how a project is
 * structured. ProjectFactory should do this.
 * 
 * @author Armando Stellato
 */
public class ProjectManager {

	public static enum ProjectType {
		continousEditing, saveToStore
	}

	public static final String triples_exchange_FileName = "ontology.nt";

	protected static Logger logger = LoggerFactory.getLogger(ProjectManager.class);

	private static OpenProjectsHolder openProjects = new OpenProjectsHolder();

	private static volatile ExtensionPointManager exptManager;
	private static volatile MetadataRegistryBackend metadataRegistryBackend;

	private static Set<Project> projectsLockedForAccess = ConcurrentHashMap.newKeySet();

	public static void setExtensionPointManager(ExtensionPointManager exptManager) {
		ProjectManager.exptManager = exptManager;
	}

	public static void setMetadataRegistryBackend(MetadataRegistryBackend metadataRegistryBackend) {
		ProjectManager.metadataRegistryBackend = metadataRegistryBackend;
	}

	/**
	 * lists the projects available (stored in the projects directory of Semantic Turkey). If
	 * <code>consumer</code> is not null, filters the list by reporting only the projects which contain
	 * <code>consumer</code> in their ACL
	 * 
	 * @param consumer
	 *            if <code>null</code>, all the projects in the projects folder are listed. Corrupted projects
	 *            are also listed in this case.
	 * @return
	 * @throws ProjectAccessException
	 */
	public static Collection<AbstractProject> listProjects(ProjectConsumer consumer)
			throws ProjectAccessException {
		ArrayList<AbstractProject> projects = new ArrayList<AbstractProject>();
		List<File> projectDirs = null;
		try {
			projectDirs = Utilities.listDirectoryContentAsFiles(Resources.getProjectsDir(), false, true,
					false);
		} catch (IOException e) {
			throw new ProjectAccessException(e);
		}
		for (File projDir : projectDirs) {
			// an AbstractProject is being declared as it can be both a description of a project, or a
			// CorruptedProject
			AbstractProject proj = null;

			if (consumer == null) { // case of no consumer
				try {
					proj = getProjectDescription(projDir.getName());
				} catch (Exception e) {
					proj = new CorruptedProject(projDir.getName(), projDir, e);
				}
				projects.add(proj);
			} else { // case of using a consumer
				try {
					proj = getProjectDescription(projDir.getName());
					if (((Project) proj).getACL().hasInACL(consumer))
						projects.add(proj);
				} catch (Exception e) {
					// if a project is corrupted, it is simply cut from the list of available projects
				}

			}

		}

		return projects;
	}

	public static Collection<AbstractProject> listProjects() throws ProjectAccessException {
		return listProjects(null);
	}

	/**
	 * 
	 * 
	 * @param projectName
	 * @throws ProjectDeletionException
	 */
	public static void deleteProject(String projectName) throws ProjectDeletionException {
		File projectDir;
		try {
			projectDir = getProjectDir(projectName);
		} catch (InvalidProjectNameException e) {
			throw new ProjectDeletionException(
					"project name: " + projectName + " is not a valid name; cannot delete that project");
		} catch (ProjectInexistentException e) {
			throw new ProjectDeletionException(
					"project: " + projectName + " does not exist; cannot delete it");
		}

		if (isOpen(projectName))
			throw new ProjectDeletionException("cannot delete a project while it is open");

		// delete the folders about project-user and project-group bindings
		try {
			ProjectUserBindingsManager.deletePUBindingsOfProject(projectName);
			ProjectGroupBindingsManager.deletePGBindingsOfProject(projectName);
		} catch (IOException e) {
			throw new ProjectDeletionException(e);
		}

		if (!Utilities.deleteDir(projectDir))
			throw new ProjectDeletionException("unable to delete project: " + projectName);
	}

	private static Project activateProject(String projectName)
			throws ProjectCreationException, ProjectInconsistentException, ProjectUpdateException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		Project proj = getProjectDescription(projectName);

		try {
			logger.debug("project " + projectName + " initialized");

			proj.activate(exptManager);

			CustomFormManager.getInstance().registerCustomFormModelOfProject(proj);

			logger.debug("project " + projectName + " activated");

			logger.debug("project : " + projectName + " created");

			return proj;

		} catch (RDF4JException e) {
			// Utilities.deleteDir(resolveProjectNameToDir(projectName));
			// I moved the delete to where I'm sure I'm creating the project
			throw new ProjectCreationException(e);
		} catch (ProjectIncompatibleException e) {
			// Utilities.deleteDir(resolveProjectNameToDir(projectName));
			// surely good to not delete here. The lack of a proper OntologyManager is not something that
			// would normally happen when creating a project. It usually happens on old projects which have
			// been created with an ontology manager which is not present in the current installation
			throw new ProjectCreationException("it is not possible to create a project with OntologyManager: "
					+ "---" + "because no bundle with such ID has been loaded by OSGi");
		}
	}

	/**
	 * returns the {@link Project} with name = <code>projectName</code>. In case no project with that name is
	 * open, <code>null</code> is returned.
	 * 
	 * @param projectName
	 * @return
	 */
	public static Project getProject(String projectName) {
		return openProjects.getProject(projectName);
		// TODO should check for existing project and valid project name
	}

	public static Project getProject(String projectName, boolean descriptionAllowed)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		Project proj = openProjects.getProject(projectName);
		if (proj == null && descriptionAllowed) {
			proj = getProjectDescription(projectName);
		}
		return proj;
	}

	public static boolean existsProject(String projectName) throws InvalidProjectNameException {
		File projectDir = resolveProjectNameToDir(projectName);
		return (projectDir.exists());
	}

	/**
	 * returns a project consumer from its string description. A valid project consumer is either
	 * {@link ProjectConsumer#SYSTEM} or an open project. If <code>consumerName</code>
	 * 
	 * @param consumerName
	 * @return
	 */
	public static ProjectConsumer getProjectConsumer(String consumerName) {

		if (consumerName.equals(ProjectConsumer.SYSTEM.getName())) {
			return ProjectConsumer.SYSTEM;
		}

		Project project = openProjects.getProject(consumerName);
		if (project != null)
			return project;
		else
			throw new IllegalProjectStatusException(
					consumerName + " is not an open project, so cannot be a consumer");

	}

	/**
	 * this method copies a project to another location. The project identified by <code>projectName</code>
	 * must not be open.
	 * 
	 * @param projectName
	 * @param newProjectName
	 * @throws InvalidProjectNameException
	 * @throws DuplicatedResourceException
	 * @throws IOException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	public static void cloneProjectToNewProject(String projectName, String newProjectName)
			throws InvalidProjectNameException, DuplicatedResourceException, IOException,
			ProjectInexistentException, ProjectAccessException {

		logger.debug("cloning project: " + projectName + " to project: " + newProjectName);

		Project.checkProjectName(newProjectName);

		if (isOpen(projectName)) {
			throw new ProjectAccessException(
					"project: " + projectName + " is currently open, thus it cannot be cloned");
		}

		File oldProjectDir = getProjectDir(projectName);
		File newProjectDir = resolveProjectNameToDir(newProjectName);

		if (newProjectDir.exists())
			throw new DuplicatedResourceException("project: " + projectName
					+ " already exists; choose a different project name for a new project");

		Utilities.recursiveCopy(oldProjectDir, newProjectDir);
		setProjectProperty(newProjectName, Project.PROJECT_NAME_PROP, newProjectName);
	}

	/**
	 * This method:
	 * <ul>
	 * <li>invokes {@link #resolveProjectNameToDir(String)} and gets the project dir associated to that
	 * name</li>
	 * <li>returns the directory if the project exists, otherwise throws a
	 * {@link ProjectInexistentException}</li>
	 * </ul>
	 * 
	 * @param projectName
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static File getProjectDir(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException {
		File projectDir = resolveProjectNameToDir(projectName);
		if (!projectDir.exists())
			throw new ProjectInexistentException("Project: " + " does not exist");
		return projectDir;
	}

	/**
	 * returns the directory of a project given its name. This is based on the assumption that the project
	 * directory is assigned on the basis of project's name (as done by the ProjectManager).<br/>
	 * <em>Note that this method does not guarantees that the project dir (and thus the project) exists</em>
	 * 
	 * @param projectName
	 * @return
	 * @throws InvalidProjectNameException
	 */
	public static File resolveProjectNameToDir(String projectName) throws InvalidProjectNameException {
		Project.checkProjectName(projectName);
		return new File(Resources.getProjectsDir(), projectName);
	}

	public static void exportProject(String projectName, File semTurkeyProjectFile)
			throws IOException, ProjectAccessException {

		if (!isOpen(projectName)) {
			throw new ProjectAccessException(
					"project " + projectName + " is not open, and thus cannot be exported");
		}

		Project project = openProjects.getProject(projectName);

		File tempDir = Resources.createTempDir();
		Utilities.copy(project.infoSTPFile, new File(tempDir, project.infoSTPFile.getName()));
		Utilities.copy(project.nsPrefixMappingsPersistence.getFile(),
				new File(tempDir, project.nsPrefixMappingsPersistence.getFile().getName()));
		Utilities.copy(project.uriGenConfigFile, new File(tempDir, project.uriGenConfigFile.getName()));
		Utilities.copy(project.renderingConfigFile, new File(tempDir, project.renderingConfigFile.getName()));
		// project.ontManager.writeRDFOnFile(new File(tempDir, triples_exchange_FileName),
		// RDFFormat.NTRIPLES);
		Utilities.createZipFile(tempDir, semTurkeyProjectFile, false, true,
				"Semantic Turkey Project Archive");
		tempDir.delete();
		tempDir.deleteOnExit();
	}

	public static void importProject(File semTurkeyProjectFile, String name)
			throws IOException, ProjectCreationException, DuplicatedResourceException,
			ProjectInconsistentException, ProjectUpdateException, InvalidProjectNameException {
		File tempDir = Resources.createTempDir();
		Utilities.unZip(semTurkeyProjectFile.getPath(), tempDir);

		// change imported project name to newly created one
		File infoSTPFile = new File(tempDir, Project.INFOFILENAME);
		Properties stp_properties = new Properties();
		FileInputStream fis = new FileInputStream(infoSTPFile);
		stp_properties.load(fis);
		fis.close();
		if (name == null)
			name = stp_properties.getProperty(Project.PROJECT_NAME_PROP);

		// copy temporary unzipped project to new import position
		File newProjDir = new File(Resources.getProjectsDir(), name);
		if (newProjDir.exists())
			throw new DuplicatedResourceException("project with name: " + name
					+ " already exists, please choose another name for the imported project");
		else if (!newProjDir.mkdirs())
			throw new ProjectCreationException("unable to create project with name: " + name);

		// copying separate files from imported project into new one
		// TODO why not first copying the whole temp directory and then make changes?
		Utilities.copy(infoSTPFile, new File(newProjDir, infoSTPFile.getName()));
		Utilities.copy(new File(tempDir, NSPrefixMappings.prefixMappingFileName),
				new File(newProjDir, NSPrefixMappings.prefixMappingFileName));
		Utilities.copy(new File(tempDir, Project.URI_GENERATOR_CONFIG_FILENAME),
				new File(newProjDir, Project.URI_GENERATOR_CONFIG_FILENAME));
		Utilities.copy(new File(tempDir, Project.RENDERING_ENGINE_CONFIG_FILENAME),
				new File(newProjDir, Project.RENDERING_ENGINE_CONFIG_FILENAME));

		// ProjectType projectType = ProjectType.valueOf(stp_properties.getProperty(Project.PROJECT_TYPE));
		// String ontManagerID = stp_properties.getProperty(Project.ONTOLOGY_MANAGER_ID_PROP);
		// logger.debug("type: " + projectType);

		Project newProj;
		try {
			newProj = activateProject(name);
			try {
				newProj.setName(name);
				// newProj.getOntologyManager().loadOntologyData(new File(tempDir, triples_exchange_FileName),
				// newProj.getBaseURI(), RDFFormat.NTRIPLES);

				tempDir.delete();
				tempDir.deleteOnExit();
			} finally {
				tearDownProject(newProj);
			}
		} catch (ProjectInexistentException e) {
			throw new ProjectCreationException("Error while importing project from file: "
					+ semTurkeyProjectFile
					+ ". New project data should have been created while it is not; unable to load the it");
		} catch (ProjectAccessException e) {
			throw new ProjectCreationException("unable to access data from project: " + name
					+ " imported from file: " + semTurkeyProjectFile);
		}

	}

	/**
	 * returns a instance of a concrete implementation of class {@link Project}, with the sole exception that
	 * the project is not activated (no data is loaded)
	 * 
	 * @param projectName
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	public static Project getProjectDescription(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		logger.debug("opening project: " + projectName);

		File projectDir = getProjectDir(projectName);

		logger.debug("project dir: " + projectDir);

		Project proj;

		try {
			proj = new PersistentStoreProject(projectName, projectDir);
			logger.debug("created project description for: " + proj);
			return proj;
		} catch (Exception e) {
			throw new ProjectAccessException(e);
		}
	}

	/**
	 * sets the value of the given property <code>property</code> for project with name
	 * <code>projectName</code>
	 * <p>
	 * use specific get methods for standard project properties; this method is left public to support
	 * customized properties defined over projects
	 * </p>
	 * 
	 * @param projectName
	 * @param property
	 * @param propValue
	 * @return
	 * @throws IOException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static void setProjectProperty(String projectName, String property, String propValue)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {

		logger.debug(
				"setting property: " + property + " of project: " + projectName + " to value: " + propValue);
		File projectDir = getProjectDir(projectName);
		logger.debug("projectDir: " + projectDir);
		File infoSTPFile = new File(projectDir, Project.INFOFILENAME);
		logger.debug("infoSTPFile: " + infoSTPFile);
		Properties stp_properties = new Properties();
		FileInputStream propFileInputStream = new FileInputStream(infoSTPFile);
		stp_properties.load(propFileInputStream);
		propFileInputStream.close();
		stp_properties.setProperty(property, propValue);
		BufferedOutputStream propFileWriteStream = new BufferedOutputStream(
				new FileOutputStream(infoSTPFile));
		stp_properties.store(propFileWriteStream, "");
		propFileWriteStream.close();
	}

	/**
	 * gets the value of the given property <code>property</code> for project with name
	 * <code>projectName</code>
	 * <p>
	 * use specific get methods for standard project properties; this method is left public to support
	 * customized properties defined over projects
	 * </p>
	 * 
	 * @param projectName
	 * @param property
	 * @return
	 * @throws IOException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static String getProjectProperty(String projectName, String property)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		File projectDir = getProjectDir(projectName);
		File infoSTPFile = new File(projectDir, Project.INFOFILENAME);
		Properties stp_properties = new Properties();
		FileInputStream fis = new FileInputStream(infoSTPFile);
		stp_properties.load(fis);
		fis.close();
		return stp_properties.getProperty(property);
	}

	/**
	 * gets name-value pairs for each property of project with name <code>projectName</code>
	 * 
	 * @param projectName
	 * @return
	 * @throws IOException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static Map<String, String> getProjectPropertyMap(String projectName)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		File projectDir = getProjectDir(projectName);
		File infoSTPFile = new File(projectDir, Project.INFOFILENAME);
		Properties stp_properties = new Properties();
		FileInputStream fis = new FileInputStream(infoSTPFile);
		stp_properties.load(fis);
		fis.close();
		Map<String, String> map = new HashMap<String, String>();
		Set<String> propList = stp_properties.stringPropertyNames();
		for (String p : propList) {
			map.put(p, stp_properties.getProperty(p));
		}
		return map;
	}

	/**
	 * gets the project.info file content for project with name <code>projectName</code>
	 * 
	 * @param projectName
	 * @return
	 * @throws IOException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static String getProjectPropertyFileContent(String projectName)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		File projectDir = getProjectDir(projectName);
		File infoSTPFile = new File(projectDir, Project.INFOFILENAME);

		String content = "";
		BufferedReader input = new BufferedReader(new FileReader(infoSTPFile));
		StringBuffer buffer = new StringBuffer();
		while ((content = input.readLine()) != null)
			buffer.append(content + "\n");
		input.close();
		content = buffer.toString();
		return content;
	}

	/**
	 * saves the project.info file content for project with name <code>projectName</code>
	 * 
	 * @param projectName
	 * @param content
	 * @return
	 * @throws IOException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static void saveProjectPropertyFileContent(String projectName, String content)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		File projectDir = getProjectDir(projectName);
		File infoSTPFile = new File(projectDir, Project.INFOFILENAME);
		PrintWriter pw = new PrintWriter(infoSTPFile);
		pw.print(content);
		pw.close();
	}

	/**
	 * as for {@link #getProjectProperty(String, String) but throws a {@link ProjectInconsistentException} if
	 * the property has a null value}
	 * 
	 * @param projectName
	 * @param property
	 * @return
	 * @throws ProjectInconsistentException
	 * @throws IOException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	public static String getRequiredProjectProperty(String projectName, String property)
			throws ProjectInconsistentException, IOException, InvalidProjectNameException,
			ProjectInexistentException {
		String propValue = getProjectProperty(projectName, property);
		if (propValue != null)
			return propValue;
		else
			throw new ProjectInconsistentException(
					"missing required " + property + " value from description of project: " + projectName);
	}

	/**
	 * gets the baseuri of the project with name <code>projectName</code>.
	 * 
	 * @param projectName
	 * @return
	 * @throws IOException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	public static String getProjectBaseURI(String projectName)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		return getProjectProperty(projectName, Project.BASEURI_PROP);
	}

	/**
	 * gets the default namespace of the project with name <code>projectName</code>.
	 * 
	 * @param projectName
	 * @return
	 * @throws IOException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	public static String getProjectDefaultNamespace(String projectName)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		return getProjectProperty(projectName, Project.DEF_NS_PROP);
	}

	/**
	 * gets the timestamp of the project with name <code>projectName</code>.
	 * 
	 * @param projectName
	 * @return
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws IOException
	 * @throws IOException
	 */
	public static long getProjectTimeStamp(String projectName)
			throws IOException, InvalidProjectNameException, ProjectInexistentException {
		String propValue = getProjectProperty(projectName, Project.TIMESTAMP_PROP);
		return Long.parseLong(propValue);
	}

	public static String escape(String in) {
		return saveConvert(in, false, false);
	}

	// the methods below come from the Property class in standard java distribution

	/*
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters with a preceding slash
	 */
	private static String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}

	/**
	 * Convert a nibble to a hex character
	 * 
	 * @param nibble
	 *            the nibble to convert.
	 */
	private static char toHex(int nibble) {
		return hexDigit[(nibble & 0xF)];
	}

	/** A table of hex digits */
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
			'D', 'E', 'F' };

	// MULTI PROJECT MANAGEMENT ADDITIONS

	public static class AccessResponse {
		private String msg;
		private boolean affirmative;

		AccessResponse(boolean affirmative, String msg) {
			this.affirmative = affirmative;
			this.msg = msg;
		}

		AccessResponse(boolean affirmative) {
			this.affirmative = affirmative;
		}

		public String getMsg() {
			return msg;
		}

		public boolean isAffirmative() {
			return affirmative;
		}
	}

	public static AccessResponse checkAccessibility(ProjectConsumer consumer, Project project,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel) {

		ProjectACL acl = project.getACL();

		// statically checking accessibility to the project through the projects' ACL
		if (!acl.isAccessibleFrom(consumer, requestedAccessLevel, requestedLockLevel))
			return new AccessResponse(false,
					"the Access Control List of project " + project.getName()
							+ " forbids access from consumer " + consumer.getName() + " with access level: "
							+ requestedAccessLevel + " and lock level: " + requestedLockLevel);

		// only if project is already open, dynamically checks its runtime status and its accessibility
		if (openProjects.isOpen(project)) {
			ProjectACL.AccessLevel accessStatus = openProjects.getAccessStatus(project);
			ProjectACL.LockLevel lockStatus = openProjects.getLockLevel(project);

			// if already locked, it cannot be locked again
			if (lockStatus != ProjectACL.LockLevel.NO && requestedLockLevel != ProjectACL.LockLevel.NO)
				return new AccessResponse(false,
						"there is already a lock on project " + project + " so it cannot be locked again");

			// requestedAccess vs lock status
			if (lockStatus == ProjectACL.LockLevel.R)
				return new AccessResponse(false,
						"LockLevel " + ProjectACL.LockLevel.R + " forbids any access to project " + project);

			if ((lockStatus == ProjectACL.LockLevel.W) && (requestedAccessLevel == ProjectACL.AccessLevel.RW))
				return new AccessResponse(false,
						"LockLevel " + ProjectACL.LockLevel.W + " forbids RW access to project " + project);

			// requestedLock vs accessStatus
			if (accessStatus == ProjectACL.AccessLevel.RW && requestedLockLevel != ProjectACL.LockLevel.NO)
				return new AccessResponse(false, "AccessLevel " + ProjectACL.AccessLevel.RW
						+ " forbids request for any lock on project " + project);

			if (accessStatus == ProjectACL.AccessLevel.R && requestedLockLevel == ProjectACL.LockLevel.R)
				return new AccessResponse(false, "AccessLevel " + ProjectACL.AccessLevel.R
						+ " forbids request for an R lock on project " + project);
		}

		return new AccessResponse(true);
	}

	/**
	 * {@link ProjectConsumer}s may request access to a project through this method. The
	 * {@link ProjectManager} verifies that the access can be granted to the requesting consumer, and in
	 * affirmative case handles all the necessary operations (verifying that the project is open, or opening
	 * it in negative case and adding it to the list of open projects, making its data available) to make the
	 * project available to the consumer.
	 * 
	 * @param consumer
	 * @param projectName
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ForbiddenProjectAccessException
	 * @throws ProjectBindingException
	 * @throws RBACException
	 */
	public static Project accessProject(ProjectConsumer consumer, String projectName,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, ProjectBindingException, RBACException {

		Project project = getProjectDescription(projectName);

		if (projectsLockedForAccess.contains(project)) {
			throw new ProjectAccessException("Project locked for access: " + project.getName());
		}

		AccessResponse accessResponse = checkAccessibility(consumer, project, requestedAccessLevel,
				requestedLockLevel);

		try {
			if (accessResponse.isAffirmative()) {
				if (openProjects.isOpen(project))
					openProjects.addConsumer(project, consumer, requestedAccessLevel, requestedLockLevel);
				else {
					project = activateProject(projectName);
					openProjects.addProject(project, consumer, requestedAccessLevel, requestedLockLevel);
				}

				/*
				 * if there aren't the folders for the project-user bindings of the current project, create
				 * them. this scenario could happen when the project is imported (by means the import function
				 * or the copy of a project folder in SemanticTurkeyData/projects)
				 */
				if (!ProjectUserBindingsManager.existsPUBindingsOfProject(project)) {
					ProjectUserBindingsManager.createPUBindingsOfProject(project);
				}
				if (!ProjectGroupBindingsManager.existsPGBindingsOfProject(project)) {
					ProjectGroupBindingsManager.createPGBindingsOfProject(project);
				}
				RBACManager.loadRBACProcessor(project);
				metadataRegistryBackend.registerProject(project);

				return project;
			} else {
				throw new ForbiddenProjectAccessException(accessResponse.getMsg());
			}
		} catch (ProjectCreationException e) {
			throw new ProjectAccessException(e);
		} catch (ProjectInconsistentException e) {
			throw new ProjectAccessException(e);
		} catch (ProjectUpdateException e) {
			throw new ProjectAccessException(e);
		} catch (InvalidProjectNameException e) {
			throw new ProjectAccessException(e);
		}

	}

	/**
	 * this method allows {@link ProjectConsumer} <code>consumer</code> to be disconnected from the
	 * {@link Project} identified by <code>projectName</code>.<br/>
	 * If the consumer is {@link ProjectConsumer#SYSTEM}, then <code>projectName</code> is disconnected in
	 * turn from the projects it consumes.
	 * 
	 * @param consumer
	 * @param projectName
	 */
	public static void disconnectFromProject(ProjectConsumer consumer, String projectName) {

		Project project = openProjects.getProject(projectName);

		// only in case the consumer is SYSTEM, the projects consumed by the given project are disconnected in
		// turn
		if (consumer == ProjectConsumer.SYSTEM) {
			Set<Project> accessedProjects = openProjects.listAccessedProjects(project);
			System.out.println("accessed projects set: " + accessedProjects);
			for (Project accessedProject : accessedProjects) {
				System.out.println("accessed project: " + accessedProject);
				disconnectFromProject(project, accessedProject.getName());
			}
		}

		openProjects.removeConsumer(project, consumer);

		if (openProjects.isNotConsumed(project)) {
			tearDownProject(project);
		}

	}

	public static boolean isOpen(Project project) {
		return openProjects.isOpen(project);
	}

	public static boolean isOpen(String project) {
		return openProjects.isOpen(project);
	}

	private static void tearDownProject(Project project) {
		logger.debug("closing project: " + project);
		project.deactivate();
		CustomFormManager.getInstance().unregisterCustomFormModelOfProject(project);
		RBACManager.unloadRBACProcessor(project);
		metadataRegistryBackend.unregisterProject(project);

		openProjects.removeProject(project);
	}

	/**
	 * Return the access level with which the consumer is accessing the project. Null if the consumer does not
	 * access the given project.
	 * 
	 * @param project
	 * @param consumer
	 * @return
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	public static AccessLevel getAccessedLevel(String projectName, ProjectConsumer consumer)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		Project project = getProjectDescription(projectName);
		if (isOpen(project)) {// look for the consumer only if the project is open
			AccessLevel accessLevel = openProjects.getAccessStatusMap(project).get(consumer);
			if (accessLevel != null) { // accessLevel could be null because project could be open but not from
										// the given consumer
				return accessLevel;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the ProjectConsumer that is locking the given project. <code>null</code> if the project is not
	 * currently locked
	 * 
	 * @param projectName
	 * @param consumer
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	public static ProjectConsumer getLockingConsumer(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		if (isOpen(projectName)) {
			return openProjects.getLockingConsumer(getProjectDescription(projectName));
		} else {
			return null;
		}
	}

	/**
	 * Returns the LockLevel which with the project is locked by the consumer. <code>null</code> if the
	 * project is not locked by the given consumer.
	 * 
	 * @param projectName
	 * @param consumer
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	public static LockLevel getLockingLevel(String projectName, ProjectConsumer consumer)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		Project project = getProjectDescription(projectName);
		if (openProjects.isLockedBy(project, consumer)) {
			return openProjects.getLockLevel(project);
		} else {
			return null;
		}
	}

	/**
	 * <p>
	 * This private class holds the information related to projects open at runtime <br/>
	 * the methods in this class should be available from the outer class {@link ProjectManager}<br/>
	 * here we just factorize the code and assure that the data structures necessary for holding this
	 * information remain consistent.
	 * </p>
	 * <br/>
	 * <p>
	 * the online status of each project reports:
	 * <ul>
	 * <li>the list of its consumers, together with the {@link AccessLevel for each of them}</li>
	 * <li>the {@link LockStatus}, which is represented by a &lt;{@link LockLevel}, {@link ProjectConsumer}
	 * &rt;</li>
	 * </ul>
	 * </p>
	 * 
	 * 
	 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
	 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
	 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
	 * 
	 */
	private static class OpenProjectsHolder {

		// if a project is open, it will always appear in each of the three maps!
		// the first map resolves the project name into a project
		// the second map returns all the consumers for that project (there is always a consumer if the
		// project is open)
		// the third map returns the lock status; note that a "no lock" is represented by a LockStatus pair
		// with <null, LockLevel.NO>
		private Map<String, Project> projects = new HashMap<String, Project>();
		private Map<Project, Map<ProjectConsumer, ProjectACL.AccessLevel>> projectsAccessStatus = new HashMap<Project, Map<ProjectConsumer, ProjectACL.AccessLevel>>();
		private Map<Project, LockStatus> projectsLockStatus = new HashMap<Project, LockStatus>();

		public Project getProject(String projectName) {
			return projects.get(projectName);
		}

		public Map<ProjectConsumer, ProjectACL.AccessLevel> getAccessStatusMap(Project project) {
			return projectsAccessStatus.get(project);
		}

		/**
		 * {@link AccessLevel#RW} if there is one project accessing it in {@link AccessLevel#RW}, otherwise
		 * {@link AccessLevel#R}
		 * 
		 * @param project
		 * @return
		 */
		public ProjectACL.AccessLevel getAccessStatus(Project project) {
			for (ProjectACL.AccessLevel accessLevel : getAccessStatusMap(project).values()) {
				if (accessLevel == ProjectACL.AccessLevel.RW)
					return ProjectACL.AccessLevel.RW;
			}
			return ProjectACL.AccessLevel.R;
		}

		public ProjectACL.LockLevel getLockLevel(Project project) {
			return projectsLockStatus.get(project).getLockLevel();
		}

		public ProjectConsumer getLockingConsumer(Project project) {
			return projectsLockStatus.get(project).getConsumer();
		}

		public boolean isLockedBy(Project project, ProjectConsumer consumer) {
			return (consumer.equals(getLockingConsumer(project)));
		}

		/**
		 * only for use internal to {@link ProjectManager}, as normally an open project should have at least
		 * one consumer, so an open project with no consumer represents a transient which is to be resolved
		 * inside {@link ProjectManager}'s methods
		 * 
		 * @param project
		 * @return
		 */
		public boolean isNotConsumed(Project project) {
			return projectsAccessStatus.get(project).isEmpty();
		}

		/**
		 * this setter method changes the value of the lock for project <code>projectName</code>. Note that no
		 * check for lockability is performed, as this method only updates {@link OpenProjectsHolder} internal
		 * data structures
		 * 
		 * @param projectName
		 * @param consumer
		 * @param lockLevel
		 */
		public void setLock(Project project, ProjectConsumer consumer, ProjectACL.LockLevel lockLevel) {
			// this check ensures that the written consumer is null in case of a NO lock
			if (lockLevel == LockLevel.NO)
				unlock(project);
			else
				projectsLockStatus.put(project, new LockStatus(consumer, lockLevel));
		}

		public void unlock(Project project) {
			projectsLockStatus.put(project, new LockStatus(null, LockLevel.NO));
		}

		/**
		 * adds a newly open {@link Project} to the list of open projects
		 * 
		 * @param project
		 * @param consumer
		 * @param accessLevel
		 * @param lockLevel
		 */
		public void addProject(Project project, ProjectConsumer consumer, ProjectACL.AccessLevel accessLevel,
				ProjectACL.LockLevel lockLevel) {
			// TODO foresee a check for existing project (it should not exist, so in case throw an exception)
			projects.put(project.getName(), project);
			Map<ProjectConsumer, ProjectACL.AccessLevel> accessStatusMap = new HashMap<ProjectConsumer, ProjectACL.AccessLevel>();
			accessStatusMap.put(consumer, accessLevel);
			projectsAccessStatus.put(project, accessStatusMap);
			setLock(project, consumer, lockLevel);
		}

		public void removeProject(Project project) {
			projects.remove(project.getName());
			projectsAccessStatus.remove(project);
			projectsLockStatus.remove(project);
			// TODO consistency check, to verify that the project is not a consumer for any other project here
		}

		/**
		 * adds a {@link ProjectConsumer} to an already open {@link Project}
		 * 
		 * @param project
		 * @param consumer
		 * @param accessLevel
		 * @param lockLevel
		 */
		public void addConsumer(Project project, ProjectConsumer consumer, ProjectACL.AccessLevel accessLevel,
				ProjectACL.LockLevel lockLevel) {
			if (!projects.containsKey(project.getName()))
				throw new IllegalProjectStatusException("project " + project
						+ " does not seem to be open, thus a new consumer cannot be added to it");

			Map<ProjectConsumer, ProjectACL.AccessLevel> accessStatusMap = projectsAccessStatus.get(project);
			accessStatusMap.put(consumer, accessLevel);
			if (lockLevel != ProjectACL.LockLevel.NO)
				setLock(project, consumer, lockLevel);
			// TODO add a consistency check here to verify that there is not already another lock
		}

		public void removeConsumer(Project project, ProjectConsumer consumer) {
			if (!projects.containsKey(project.getName()))
				throw new IllegalProjectStatusException("project " + project
						+ " does not seem to be open, thus a consumer cannot be removed from it. Actually, there should be no consumer for it!");

			if (isLockedBy(project, consumer))
				unlock(project);

			AccessLevel removedLevel = projectsAccessStatus.get(project).remove(consumer);

			// consistency check: not possible to invoke a consumer remove if the project was not consumed by
			// the given consumer
			if (removedLevel == null)
				throw new IllegalProjectStatusException(
						"project " + project + " was not accessed by consumer " + consumer
								+ ". Inconsistent request to remove this consumer");

		}

		/**
		 * lists all projects accessed by a given consumer
		 * 
		 * @param consumer
		 * @return
		 */
		public Set<Project> listAccessedProjects(ProjectConsumer consumer) {
			HashSet<Project> accessedProjects = new HashSet<Project>();
			for (Entry<Project, Map<ProjectConsumer, ProjectACL.AccessLevel>> mapEntry : projectsAccessStatus
					.entrySet()) {
				if (mapEntry.getValue().keySet().contains(consumer))
					accessedProjects.add(mapEntry.getKey());
			}
			return accessedProjects;
		}

		public boolean isOpen(String projectName) {
			return projects.containsKey(projectName);
		}

		public boolean isOpen(Project project) {
			return isOpen(project.getName());
		}

		/**
		 * This class implements pairs of elements of type: &lt;{@link ProjectConsumer},
		 * {@link ProjectACL.LockLevel}&gt; and is used to state which {@link ProjectConsumer} is locking
		 * (with a certain {@link ProjectACL.LockLevel}) the project holding this status
		 * 
		 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
		 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
		 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
		 * 
		 */
		private static class LockStatus {

			/**
			 * this is null if lockLevel is {@link ProjectACL.LockLevel#NO}
			 */
			private ProjectConsumer consumer;
			private ProjectACL.LockLevel lockLevel;

			public LockStatus(ProjectConsumer consumer, LockLevel lockLevel) {
				this.consumer = consumer;
				this.lockLevel = lockLevel;
			}

			public ProjectConsumer getConsumer() {
				return consumer;
			}

			public ProjectACL.LockLevel getLockLevel() {
				return lockLevel;
			}

			// probably the lock status will always be created from scratch
			// public void setLockLevel(ProjectACL.LockLevel lockLevel) {
			// this.lockLevel = lockLevel;
			// }
			//
			// public void setConsumer(ProjectConsumer consumer) {
			// this.consumer = consumer;
			// }

		}

	}

	public static Project createProject(ProjectConsumer consumer, String projectName, IRI model,
			IRI lexicalizationModel, String baseURI, boolean historyEnabled, boolean validationEnabled,
			boolean blacklistingEnabled, RepositoryAccess repositoryAccess, String coreRepoID,
			PluginSpecification coreRepoSailConfigurerSpecification, String coreBackendType,
			String supportRepoID, PluginSpecification supportRepoSailConfigurerSpecification,
			String supportBackendType, PluginSpecification uriGeneratorSpecification,
			PluginSpecification renderingEngineSpecification, IRI creationDateProperty,
			IRI modificationDateProperty, String[] updateForRoles, File preloadedDataFile,
			RDFFormat preloadedDataFormat, TransitiveImportMethodAllowance transitiveImportAllowance,
			Set<IRI> failedImports, String leftDataset, String rightDataset)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, DuplicatedResourceException, ProjectCreationException,
			ClassNotFoundException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, WrongPropertiesException, ProjectBindingException,
			RBACException, UnsupportedModelException, UnsupportedLexicalizationModelException,
			ProjectInconsistentException, InvalidConfigurationException, STPropertyAccessException,
			IOException, ReservedPropertyUpdateException, ProjectUpdateException {

		if (!validationEnabled && blacklistingEnabled) {
			throw new IllegalArgumentException(
					"Term rejection logging can't be enabled in a project without validation");
		}

		// Currently, only continuous editing projects
		ProjectType projType = ProjectType.continousEditing;

		// Currently, only projects in the default location
		File projectDir = resolveProjectNameToDir(projectName);

		// Checks the suitability of the project name
		Project.checkProjectName(projectName);

		// Checks the provided models
		Project.checkModels(model, lexicalizationModel);

		if (projectDir.exists()) {
			throw new DuplicatedResourceException("Project: " + projectName
					+ " already exists; choose a different project name for a new project");
		}

		// Creates the directory for the project, checking whether it already existed
		if (!projectDir.mkdir()) {
			throw new ProjectCreationException("Creation of project directory failed");
		}

		if (!Arrays.stream(updateForRoles)
				.allMatch(s -> "resource".equals(s) || EnumUtils.isValidEnum(RDFResourceRole.class, s))) {
			throw new ProjectCreationException("One or more roles to be updated are illegal");
		}

		boolean shouldDeleteRemoteRepositoriesOnRollback = false;

		try {

			// Currently, only guess the namespace from the base uri
			String defaultNamespace = ModelUtilities.createDefaultNamespaceFromBaseURI(baseURI);

			RepositoryConfig coreRepositoryConfig = new RepositoryConfig("core",
					"Core repository for project " + projectName);
			RepositoryConfig supportRepositoryConfig;

			if (historyEnabled || validationEnabled) {
				supportRepositoryConfig = new RepositoryConfig("support",
						"Support repository for project " + projectName);
			} else {
				supportRepositoryConfig = null;
			}

			if (repositoryAccess.isLocal()) { // Local repositories
				RepositoryImplConfigurer coreRepoSailConfigurer = instantiateRepositoryImplConfigurer(
						coreRepoSailConfigurerSpecification);
				RepositoryImplConfig coreRepositoryImplConfig = coreRepoSailConfigurer
						.buildRepositoryImplConfig(backendSailImplConfig -> {
							if (supportRepositoryConfig == null)
								return backendSailImplConfig;

							ChangeTrackerConfig changeTrackerSailConfig = new ChangeTrackerConfig(
									backendSailImplConfig);
							changeTrackerSailConfig.setSupportRepositoryID("support");
							changeTrackerSailConfig.setMetadataNS(defaultNamespace + "metadata#");
							changeTrackerSailConfig.setHistoryEnabled(historyEnabled);
							if (historyEnabled) {
								changeTrackerSailConfig.setHistoryGraph(SimpleValueFactory.getInstance()
										.createIRI(defaultNamespace + "history"));
							}
							changeTrackerSailConfig.setValidationEnabled(validationEnabled);
							if (validationEnabled) {
								changeTrackerSailConfig.setValidationGraph(SimpleValueFactory.getInstance()
										.createIRI(defaultNamespace + "validation"));
							}
							changeTrackerSailConfig.setBlacklistingEnabled(blacklistingEnabled);
							if (blacklistingEnabled) {
								changeTrackerSailConfig.setBlacklistGraph(SimpleValueFactory.getInstance()
										.createIRI(defaultNamespace + "blacklist"));
							}
							return changeTrackerSailConfig;
						});
				coreRepositoryConfig.setRepositoryImplConfig(coreRepositoryImplConfig);

				if (supportRepositoryConfig != null) {
					RepositoryImplConfigurer supportRepoSailConfigurer = instantiateRepositoryImplConfigurer(
							supportRepoSailConfigurerSpecification);
					RepositoryImplConfig supportRepositoryImplConfig = supportRepoSailConfigurer
							.buildRepositoryImplConfig(null);
					supportRepositoryConfig.setRepositoryImplConfig(supportRepositoryImplConfig);
				}
			} else { // Remote repositories
				RemoteRepositoryAccess remoteRepositoryAccess = (RemoteRepositoryAccess) repositoryAccess;

				if (remoteRepositoryAccess instanceof CreateRemote) {
					RepositoryConfig newCoreRepositoryConfig = new RepositoryConfig(coreRepoID,
							"Core repository for project " + projectName);
					RepositoryImplConfigurer coreRepoSailConfigurer = instantiateRepositoryImplConfigurer(
							coreRepoSailConfigurerSpecification);
					RepositoryImplConfig coreRepositoryImplConfig = coreRepoSailConfigurer
							.buildRepositoryImplConfig(backendSailImplConfig -> {
								if (supportRepositoryConfig == null)
									return backendSailImplConfig;

								ChangeTrackerConfig changeTrackerSailConfig = new ChangeTrackerConfig(
										backendSailImplConfig);
								changeTrackerSailConfig.setSupportRepositoryID(supportRepoID);
								changeTrackerSailConfig.setMetadataNS(defaultNamespace + "metadata#");
								changeTrackerSailConfig.setHistoryEnabled(historyEnabled);
								if (historyEnabled) {
									changeTrackerSailConfig.setHistoryGraph(SimpleValueFactory.getInstance()
											.createIRI(defaultNamespace + "history"));
								}
								changeTrackerSailConfig.setValidationEnabled(validationEnabled);
								if (validationEnabled) {
									changeTrackerSailConfig.setValidationGraph(SimpleValueFactory
											.getInstance().createIRI(defaultNamespace + "validation"));
								}
								changeTrackerSailConfig.setBlacklistingEnabled(blacklistingEnabled);
								if (blacklistingEnabled) {
									changeTrackerSailConfig.setBlacklistGraph(SimpleValueFactory.getInstance()
											.createIRI(defaultNamespace + "blacklist"));
								}

								return changeTrackerSailConfig;
							});
					newCoreRepositoryConfig.setRepositoryImplConfig(coreRepositoryImplConfig);

					RepositoryConfig newSupportRepositoryConfig = null;

					if (supportRepositoryConfig != null) {
						newSupportRepositoryConfig = new RepositoryConfig(supportRepoID,
								"Support repository for project " + projectName);
						RepositoryImplConfigurer supportRepoSailConfigurer = instantiateRepositoryImplConfigurer(
								supportRepoSailConfigurerSpecification);
						RepositoryImplConfig supportRepositoryImplConfig = supportRepoSailConfigurer
								.buildRepositoryImplConfig(null);
						newSupportRepositoryConfig.setRepositoryImplConfig(supportRepositoryImplConfig);
					}

					// Model model = new TreeModel();
					// newCoreRepositoryConfig.export(model);
					// Rio.write(model, System.out, RDFFormat.TURTLE);

					RepositoryManager remoteRepoManager = RemoteRepositoryManager.getInstance(
							remoteRepositoryAccess.getServerURL().toString(),
							remoteRepositoryAccess.getUsername(), remoteRepositoryAccess.getPassword());

					try {
						if (remoteRepoManager.hasRepositoryConfig(coreRepoID)) {
							throw new ProjectCreationException(
									"Remote repository already existing: " + coreRepoID);
						}

						if (supportRepositoryConfig != null
								&& remoteRepoManager.hasRepositoryConfig(supportRepoID)) {
							throw new ProjectCreationException(
									"Remote repository already existing: " + supportRepoID);
						}

						shouldDeleteRemoteRepositoriesOnRollback = true;

						if (newSupportRepositoryConfig != null) {
							remoteRepoManager.addRepositoryConfig(newSupportRepositoryConfig);
						}
						remoteRepoManager.addRepositoryConfig(newCoreRepositoryConfig);
					} finally {
						remoteRepoManager.shutDown();
					}

					/* Creating a remote repository: try to detect the backend type */
					if (coreBackendType == null) {
						coreBackendType = STLocalRepositoryManager
								.detectBackendType(newCoreRepositoryConfig.getRepositoryImplConfig());
					}
					if (supportBackendType == null && newSupportRepositoryConfig != null) {
						supportBackendType = STLocalRepositoryManager
								.detectBackendType(newSupportRepositoryConfig.getRepositoryImplConfig());
					}
				} else { // remote access & not create --> access existing remote

					// Check the existence of remote repositories

					RepositoryManager remoteRepoManager = RemoteRepositoryManager.getInstance(
							remoteRepositoryAccess.getServerURL().toString(),
							remoteRepositoryAccess.getUsername(), remoteRepositoryAccess.getPassword());

					try {
						if (!remoteRepoManager.hasRepositoryConfig(coreRepoID)) {
							throw new ProjectCreationException(
									"Remote repository not existing: " + coreRepoID);
						}

						if (supportRepositoryConfig != null
								&& !remoteRepoManager.hasRepositoryConfig(supportRepoID)) {
							throw new ProjectCreationException(
									"Remote repository not existing: " + supportRepoID);
						}
					} finally {
						remoteRepoManager.shutDown();
					}
				}

				HTTPRepositoryConfig coreRemoteRepoConfig = new HTTPRepositoryConfig();
				coreRemoteRepoConfig.setURL(Protocol
						.getRepositoryLocation(remoteRepositoryAccess.getServerURL().toString(), coreRepoID));
				String username = remoteRepositoryAccess.getUsername();
				if (username != null && !username.isEmpty()) {
					coreRemoteRepoConfig.setUsername(username);
				}

				String password = remoteRepositoryAccess.getPassword();
				if (password != null && !password.isEmpty()) {
					coreRemoteRepoConfig.setPassword(password);
				}

				coreRepositoryConfig.setRepositoryImplConfig(coreRemoteRepoConfig);

				if (supportRepositoryConfig != null) {

					HTTPRepositoryConfig supportRemoteRepoConfig = new HTTPRepositoryConfig();
					supportRemoteRepoConfig.setURL(Protocol.getRepositoryLocation(
							remoteRepositoryAccess.getServerURL().toString(), supportRepoID));

					if (username != null && !username.isEmpty()) {
						supportRemoteRepoConfig.setUsername(username);
					}

					if (password != null && !password.isEmpty()) {
						supportRemoteRepoConfig.setPassword(password);
					}

					supportRepositoryConfig.setRepositoryImplConfig(supportRemoteRepoConfig);
				}
			}

			prepareProjectFiles(consumer, projectName, model, lexicalizationModel, projType, projectDir,
					baseURI, defaultNamespace, historyEnabled, validationEnabled, blacklistingEnabled,
					repositoryAccess, coreRepoID, coreRepositoryConfig, coreBackendType, supportRepoID,
					supportRepositoryConfig, supportBackendType, uriGeneratorSpecification,
					renderingEngineSpecification, creationDateProperty, modificationDateProperty,
					updateForRoles, leftDataset, rightDataset);

			// make sure that both the left and right dataset of an EDOAL project grants read access to it
			if (Project.EDOAL_MODEL.equals(model)) {
				if (leftDataset == null) {
					throw new IllegalArgumentException(
							"The left dataset of an EDOAL project must be non null");
				}

				if (rightDataset == null) {
					throw new IllegalArgumentException(
							"The righ dataset of an EDOAL project must be non null");
				}
				Project projectBeingCreated = ProjectManager.getProjectDescription(projectName);
				Project leftDatasetProject = ProjectManager.getProjectDescription(leftDataset);
				Project rightDatasetProject = ProjectManager.getProjectDescription(rightDataset);

				leftDatasetProject.getACL().grantAccess(projectBeingCreated, AccessLevel.R);
				rightDatasetProject.getACL().grantAccess(projectBeingCreated, AccessLevel.R);
			}

			Project project = accessProject(consumer, projectName, AccessLevel.RW, LockLevel.NO);

			if (preloadedDataFile != null) {
				Repository repository = project.getNewOntologyManager().getRepository();
				RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repository);
				try {
					ValidationUtilities.executeWithoutValidation(validationEnabled, conn,
							(_conn) -> project.getNewOntologyManager().loadOntologyData(conn,
									preloadedDataFile, baseURI, preloadedDataFormat,
									SimpleValueFactory.getInstance().createIRI(baseURI),
									transitiveImportAllowance, failedImports));
				} finally {
					RDF4JRepositoryUtils.releaseConnection(conn, repository);
				}
			}

			return project;
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("directory: " + projectDir + " deleted due to project creation fail");

			try {
				try {
					try {
						if (repositoryAccess instanceof CreateRemote) {
							if (shouldDeleteRemoteRepositoriesOnRollback) {
								try {
									CreateRemote createRemoteAccess = (CreateRemote) repositoryAccess;

									RepositoryManager remoteRepoManager = RemoteRepositoryManager.getInstance(
											createRemoteAccess.getServerURL().toString(),
											createRemoteAccess.getUsername(),
											createRemoteAccess.getPassword());

									boolean removedCore = false;
									boolean removedSupport = false;

									try {
										try {
											try {
												remoteRepoManager.removeRepository(coreRepoID);
												removedCore = true;
											} finally {
												if (supportRepoID != null) {
													remoteRepoManager.removeRepository(supportRepoID);
												}
												removedSupport = true;
											}
										} catch (RDF4JException e2) {
											logger.debug("Swallowed exception", e2);
										}

										// If the removeRepository didn't succeeded (e.g. wrong repo config),
										// try
										// to manipulate the SYSTEM repository

										if (!removedCore || !removedSupport) {
											Repository systemRepository = remoteRepoManager
													.getSystemRepository();

											ArrayList<String> repoIdsToRemove = new ArrayList<>();
											if (!removedCore) {
												repoIdsToRemove.add(coreRepoID);
											}

											if (!removedSupport) {
												repoIdsToRemove.add(supportRepoID);
											}
											RepositoryConfigUtil.removeRepositoryConfigs(systemRepository,
													repoIdsToRemove
															.toArray(new String[repoIdsToRemove.size()]));

										}
									} finally {
										remoteRepoManager.shutDown();
									}
								} catch (RDF4JException e2) {
									logger.debug("Swallowed exception", e2);
								}
							}
						}
					} finally {
						ProjectUserBindingsManager.deletePUBindingsOfProject(projectName);
						ProjectGroupBindingsManager.deletePGBindingsOfProject(projectName);
					}
				} catch (IOException e1) {
					logger.debug("Swallowed exception", e1);
				}
			} finally {
				Utilities.deleteDir(projectDir); // if something fails, deletes everything
			}
			throw e;
		}
	}

	private static RepositoryImplConfigurer instantiateRepositoryImplConfigurer(PluginSpecification spec)
			throws ClassNotFoundException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, WrongPropertiesException, IllegalArgumentException,
			STPropertyAccessException, InvalidConfigurationException {
		try {
			return exptManager.instantiateExtension(RepositoryImplConfigurer.class, spec);
		} catch (NoSuchExtensionException e) {
			return (RepositoryImplConfigurer) spec.instatiatePlugin();
		}
	}

	private static void prepareProjectFiles(ProjectConsumer consumer, String projectName, IRI model,
			IRI lexicalizationModel, ProjectType type, File projectDir, String baseURI,
			String defaultNamespace, boolean historyEnabled, boolean validationEnabled,
			boolean blacklistingEnabled, RepositoryAccess repositoryAccess, String coreRepoID,
			RepositoryConfig coreRepoConfig, String coreBackendType, String supportRepoID,
			RepositoryConfig supportRepoConfig, String supportBackendType,
			PluginSpecification uriGeneratorSpecification, PluginSpecification renderingEngineSpecification,
			IRI creationDateProperty, IRI modificationDateProperty, String[] updateForRoles,
			String leftDataset, String rightDataset)
			throws DuplicatedResourceException, ProjectCreationException {
		File info_stp = new File(projectDir, Project.INFOFILENAME);

		try {
			// STP file containing properties for the loaded project
			logger.debug("creating project info file: " + info_stp);
			info_stp.createNewFile();
			logger.debug("project info file: " + info_stp + " created");
			// here we write directly on the file; once the project is loaded, it will be handled internally
			// as a property file
			BufferedWriter out = new BufferedWriter(new FileWriter(info_stp));
			// out.write(Project.ONTOLOGY_MANAGER_ID_PROP + "=" + escape(ontManagerID) + "\n");
			// out.write(Project.MODELCONFIG_ID + "=" + escape(modelConfigurationClass) + "\n");
			out.write(Project.HISTORY_ENABLED_PROP + "=" + historyEnabled + "\n");
			out.write(Project.VALIDATION_ENABLED_PROP + "=" + validationEnabled + "\n");
			out.write(Project.BLACKLISTING_ENABLED_PROP + "=" + blacklistingEnabled + "\n");
			out.write(Project.URI_GENERATOR_FACTORY_ID_PROP + "="
					+ escape(uriGeneratorSpecification.getFactoryId()) + "\n");
			out.write(Project.URI_GENERATOR_CONFIGURATION_TYPE_PROP + "="
					+ escape(uriGeneratorSpecification.getConfigType()) + "\n");
			out.write(Project.RENDERING_ENGINE_FACTORY_ID_PROP + "="
					+ escape(renderingEngineSpecification.getFactoryId()) + "\n");
			out.write(Project.RENDERING_ENGINE_CONFIGURATION_TYPE_PROP + "="
					+ escape(renderingEngineSpecification.getConfigType()) + "\n");
			out.write(Project.BASEURI_PROP + "=" + escape(baseURI) + "\n");
			out.write(Project.DEF_NS_PROP + "=" + escape(defaultNamespace) + "\n");
			out.write(Project.MODEL_PROP + "=" + escape(model.stringValue()) + "\n");
			out.write(Project.LEXICALIZATION_MODEL_PROP + "=" + escape(lexicalizationModel.stringValue())
					+ "\n");
			out.write(Project.PROJECT_NAME_PROP + "=" + projectName + "\n");
			out.write(Project.TIMESTAMP_PROP + "=" + Long.toString(new Date().getTime()) + "\n");
			out.write(ProjectACL.ACL + "="
					+ ProjectACL.serializeACL(consumer.getName(), ProjectACL.AccessLevel.RW) + "\n");
			out.write(Project.DEFAULT_REPOSITORY_LOCATION_PROP + "="
					+ escape(RepositoryLocation.fromRepositoryAccess(repositoryAccess).toString()) + "\n");

			if (creationDateProperty != null) {
				out.write(
						Project.CREATION_DATE_PROP + "=" + escape(creationDateProperty.stringValue()) + "\n");
			}

			if (modificationDateProperty != null) {
				out.write(Project.MODIFICATION_DATE_PROP + "="
						+ escape(modificationDateProperty.stringValue()) + "\n");
			}

			out.write(Project.UPDATE_FOR_ROLES_PROP + "="
					+ escape(Arrays.stream(updateForRoles).collect(Collectors.joining(","))) + "\n");

			if (leftDataset != null) {
				out.write(Project.LEFT_DATASET_PROP + "="
						+ escape(leftDataset) + "\n");
			}
			
			if (rightDataset != null) {
				out.write(Project.RIGHT_DATASET_PROP + "="
						+ escape(rightDataset) + "\n");
			}

			out.close();

			logger.debug("project creation: all project properties have been stored");

			// Prefix Mapping file creation
			File prefixMappingFile = new File(projectDir, NSPrefixMappings.prefixMappingFileName);
			prefixMappingFile.createNewFile();

			// // Core Repository Configuration file creation
			// File coreRepoConfigurationFile = new File(projectDir, Project.COREREPOCONFIG_FILENAME);
			// coreRepoConfigurationFile.createNewFile();
			// try (FileWriter fw = new FileWriter(coreRepoConfigurationFile)) {
			// Model model = new TreeModel();
			// coreRepoConfig.export(model);
			// Rio.write(model, fw, org.eclipse.rdf4j.rio.RDFFormat.TURTLE);
			// }
			//
			// // Support Repository Configuration file creation
			// File supportRepoConfigurationFile = new File(projectDir, Project.SUPPORTREPOCONFIG_FILENAME);
			// supportRepoConfigurationFile.createNewFile();
			// try (FileWriter fw = new FileWriter(supportRepoConfigurationFile)) {
			// Model model = new TreeModel();
			// supportRepoConfig.export(model);
			// Rio.write(model, fw, org.eclipse.rdf4j.rio.RDFFormat.TURTLE);
			// }

			File uriGenConfigurationFile = new File(projectDir, Project.URI_GENERATOR_CONFIG_FILENAME);
			uriGenConfigurationFile.createNewFile();
			try (FileWriter fw = new FileWriter(uriGenConfigurationFile)) {
				uriGeneratorSpecification.getProperties().store(fw,
						"uri generator configuration, initialized from project initialization");
			}

			File renderingEngineConfigurationFile = new File(projectDir,
					Project.RENDERING_ENGINE_CONFIG_FILENAME);
			renderingEngineConfigurationFile.createNewFile();
			try (FileWriter fw = new FileWriter(renderingEngineConfigurationFile)) {
				renderingEngineSpecification.getProperties().store(fw,
						"rendering engine configuration, initialized from project initialization");
			}

			logger.debug("all project info have been built");

			STLocalRepositoryManager localRepoMgr = new STLocalRepositoryManager(projectDir);
			localRepoMgr.initialize();
			try {
				localRepoMgr.addRepositoryConfig(coreRepoConfig, coreBackendType, true);

				if (supportRepoConfig != null) {
					localRepoMgr.addRepositoryConfig(supportRepoConfig, supportBackendType, false);
				}
			} finally {
				localRepoMgr.shutDown();
			}
		} catch (IOException e) {
			throw new ProjectCreationException(e);
		}
	}

	public static void handleProjectExclusively(String projectName, Consumer<Project> projectConsumer)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		Project project = getProject(projectName, true);

		boolean alreadyLocked = !projectsLockedForAccess.add(project);

		if (alreadyLocked) {
			throw new ProjectAccessException("Project \'" + projectName + "\' is already locked");
		}

		try {
			if (isOpen(project)) {
				throw new ProjectAccessException("Project \'" + projectName + "\' is open");
			}

			projectConsumer.accept(project);
		} finally {
			projectsLockedForAccess.remove(project);
		}
	}

}