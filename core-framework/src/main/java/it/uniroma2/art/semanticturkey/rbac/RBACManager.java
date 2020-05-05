package it.uniroma2.art.semanticturkey.rbac;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.Role.RoleLevel;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RBACManager {
	
	public static final class DefaultRole {
		public static final Role LEXICOGRAPHER = new Role("lexicographer", RoleLevel.system);
		public static final Role LURKER = new Role("lurker", RoleLevel.system);
		public static final Role MAPPER = new Role("mapper", RoleLevel.system);
		public static final Role ONTOLOGIST = new Role("ontologist", RoleLevel.system);
		public static final Role PROJECTMANAGER = new Role("projectmanager", RoleLevel.system);
		public static final Role RDF_GEEK = new Role("rdfgeek", RoleLevel.system);
		public static final Role THESAURUS_EDITOR = new Role("thesaurus-editor", RoleLevel.system);
		public static final Role VALIDATOR = new Role("validator", RoleLevel.system);
	}
	
	private static final String SYSTEM_PROJ_ID = "SYSTEM";
	private static final String ROLES_DIR_NAME = "roles";
	private static final String roleFilenamePattern = "^(role_)(.)+\\.pl$";
	
	private static Map<String, Map<String, RBACProcessor>> rbacMap = new HashMap<>(); //<projectName, <roleName, RBACProcessor>>
	
	public static void initRoles() throws ProjectAccessException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project proj = (Project) absProj;
				File rolesDir = getRolesDir(proj);
				if (rolesDir != null) {
					rolesDir.mkdir();
				}
				Map<String, RBACProcessor> roleRbacMap = new HashMap<>();
				for (File roleFile : rolesDir.listFiles()) {
					if (roleFile.isFile() && roleFile.getName().matches(roleFilenamePattern)) {
						String fileName = roleFile.getName();
						String role = fileName.substring(fileName.indexOf("role_") + 5, fileName.indexOf(".pl"));
						roleRbacMap.put(role, null);
					}
				}
				rbacMap.put(proj.getName(), roleRbacMap);
			}
		}
	}
	
	/**
	 * Loads the roles and the rbac processor for the given project. 
	 * @param project <code>null</code> means <code>SYSTEM</code>
	 * @throws InvalidTheoryException
	 * @throws TheoryNotFoundException
	 * @throws RBACException 
	 */
	public static void loadRBACProcessor(Project project) throws RBACException {
		try {
			File rolesDir = getRolesDir(project);
			if (rolesDir != null) {
				rolesDir.mkdir();
			}
			Map<String, RBACProcessor> roleRbacMap = new HashMap<>();
			for (File roleFile : rolesDir.listFiles()) {
				if (roleFile.isFile() && roleFile.getName().matches(roleFilenamePattern)) {
					RBACProcessor rbac = new RBACProcessor(roleFile);
					roleRbacMap.put(rbac.getRole(), rbac);
				}
			}
			String projectName = project == null ? SYSTEM_PROJ_ID : project.getName();
			rbacMap.put(projectName, roleRbacMap);
		} catch (InvalidTheoryException | TheoryNotFoundException e) {
			throw new RBACException(e);
		}
	}
	
	/**
	 * 
	 * @param project
	 */
	public static void unloadRBACProcessor(Project project) {
		Map<String, RBACProcessor> roleRBacMap = rbacMap.get(project.getName());
		roleRBacMap.replaceAll((k, v) -> null);
	}
	
	/**
	 * Returns the RBACProcessor for the given role in the given project. If project is null,
	 * searches the role at system level.
	 * @param project
	 * @param role
	 * @return
	 */
	public static RBACProcessor getRBACProcessor(Project project, String role) {
		//first looks for role at project level...
		String projectName = project == null ? SYSTEM_PROJ_ID : project.getName();
		RBACProcessor rbac = rbacMap.get(projectName).get(role);
		//then, if not found at project level, looks at system level
		if (rbac == null) {
			rbac = rbacMap.get(SYSTEM_PROJ_ID).get(role);
		}
		return rbac;
	}
	
	/**
	 * Returns the roles defined at system level and in the given project
	 * @param project
	 * @return
	 */
	public static Collection<Role> getRoles(Project project) {
		Collection<Role> roles = new ArrayList<>(); 
		for (String role: rbacMap.get(SYSTEM_PROJ_ID).keySet()) {
			roles.add(new Role(role, RoleLevel.system));
		}
		if (project != null) {
			for (String role: rbacMap.get(project.getName()).keySet()) {
				roles.add(new Role(role, RoleLevel.project));
			}
		}
		return roles;
	}
	
	/**
	 * Returns the role with the given name, <code>null</code> if no role is found with the given name.
	 * This method first searches for the role in the given project, then if no role is found, searches at system level.
	 * @param project
	 * @param roleName
	 * @return
	 */
	public static Role getRole(Project project, String roleName) {
		if (project != null) {
			if (rbacMap.get(project.getName()).containsKey(roleName)) {
				return new Role(roleName, RoleLevel.project); 
			} else if (rbacMap.get(SYSTEM_PROJ_ID).containsKey(roleName)) {
				return new Role(roleName, RoleLevel.system);
			}
		} else {
			if (rbacMap.get(SYSTEM_PROJ_ID).containsKey(roleName)) {
				return new Role(roleName, RoleLevel.system);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param project
	 * @param roleName
	 * @throws RoleCreationException
	 */
	public static void createRole(Project project, String roleName) throws RoleCreationException {
		//look if the role already exists at project or system level
		if (rbacMap.get(SYSTEM_PROJ_ID).containsKey(roleName)) {
			throw new RoleCreationException("Role '" + roleName + "' already exists");
		}
		if (rbacMap.get(project.getName()).containsKey(roleName)) {
			throw new RoleCreationException("Role '" + roleName + "' already exists in project " + project.getName());
		}
		try {
			File newRoleFile = getRoleFile(project, roleName);
			newRoleFile.createNewFile();
			rbacMap.get(project.getName()).put(roleName, new RBACProcessor(newRoleFile));
		} catch (InvalidTheoryException | TheoryNotFoundException | IOException e) {
			throw new RoleCreationException(e);
		}
	}

	/**
	 *
	 * @param roleName
	 * @param roleFile
	 * @throws RoleCreationException
	 */
	public static void addSystemRole(String roleName, File roleFile) throws RoleCreationException {
		//look if the role already exists at system level
		if (rbacMap.get(SYSTEM_PROJ_ID).containsKey(roleName)) {
			throw new RoleCreationException("Role '" + roleName + "' already exists");
		}
		try {
			rbacMap.get(SYSTEM_PROJ_ID).put(roleName, new RBACProcessor(roleFile));
		} catch (InvalidTheoryException | TheoryNotFoundException e) {
			throw new RoleCreationException(e);
		}
	}
	
	public static void deleteRole(Project project, String roleName) {
		rbacMap.get(project.getName()).remove(roleName);
		File rolesFile = getRoleFile(project, roleName);
		rolesFile.delete();
	}
	
	/**
	 * Returns the capabilities of the given role. This method looks for the role first at project level,
	 * then at system level. 
	 * @param project
	 * @param role
	 * @return
	 * @throws RBACException
	 */
	public static Collection<String> getRoleCapabilities(Project project, String role) throws RBACException {
		try {
			//check for role at project level
			RBACProcessor rbac = getRBACProcessor(project, role);
			if (rbac == null) {
				rbac = getRBACProcessor(null, role);
			}
			if (rbac == null) {
				throw new RBACException("Role '" + role + "' doesn't exist");
			}
			return rbac.getCapabilitiesAsStringList();
		} catch (MalformedGoalException | NoSolutionException | NoMoreSolutionException e) {
			throw new RBACException(e);
		}
	}
	
	public static void setCapabilities(Project project, String role, Collection<String> capabilities) throws RBACException {
		RBACProcessor rbac = getRBACProcessor(project, role);
		if (rbac == null) {
			throw new RBACException("Role '" + role + "' doesn't exist");
		} 
		try {
			File roleFile = getRoleFile(project, role);
			serializeCapabilities(roleFile, capabilities);
			rbacMap.get(project.getName()).put(role, new RBACProcessor(roleFile));
		} catch (InvalidTheoryException | TheoryNotFoundException | IOException e) {
			throw new RBACException("Failed to update the capability of role " + role, e);
		}
	}
	
	public static void addCapability(Project project, String role, String capability) throws RBACException {
		RBACProcessor rbac = getRBACProcessor(project, role);
		if (rbac == null) {
			throw new RBACException("Role '" + role + "' doesn't exist");
		} 
		try {
			//check if capability is duplicated
			capability = capability.replaceAll(" ", ""); //removes whitespaces
			Collection<String> capabilities = rbac.getCapabilitiesAsStringList();
			if (capabilities.contains(capability)) {
				throw new RBACException("Duplicated capability '" + capability + "' in role " + role);
			}
			//add capability
			capabilities.add(capability);
			//finally serialize and update the processor
			File roleFile = getRoleFile(project, role);
			serializeCapabilities(roleFile, capabilities);
			rbacMap.get(project.getName()).put(role, new RBACProcessor(roleFile));
		} catch (NoMoreSolutionException | MalformedGoalException | NoSolutionException |
				InvalidTheoryException | TheoryNotFoundException | IOException e) {
			throw new RBACException("Failed to update the capability of role " + role, e);
		}
	}
	
	public static void removeCapability(Project project, String role, String capability) throws RBACException {
		RBACProcessor rbac = getRBACProcessor(project, role);
		if (rbac == null) {
			throw new RBACException("Role '" + role + "' doesn't exist");
		} 
		try {
			Collection<String> capabilities = rbac.getCapabilitiesAsStringList();
			capabilities.remove(capability);
			File roleFile = getRoleFile(project, role);
			serializeCapabilities(roleFile, capabilities);
			rbacMap.get(project.getName()).put(role, new RBACProcessor(roleFile));
		} catch (NoMoreSolutionException | MalformedGoalException | NoSolutionException |
				InvalidTheoryException | TheoryNotFoundException | IOException e) {
			throw new RBACException("Failed to update the capability of role " + role, e);
		}
	}

	private static void serializeCapabilities(File roleFile, Collection<String> capabilities) throws IOException {
		try (Writer writer = new BufferedWriter(new FileWriter(roleFile))) {
			for (String capability : capabilities) {
				writer.append(capability).append(".").append(System.lineSeparator());
			}
		}
	}
	
	public static File getRolesDir(Project project) {
		File rolesDir;
		if (project == null) {
			rolesDir = new File(Resources.getSystemDir(), ROLES_DIR_NAME); 
		} else {
			rolesDir = new File(Resources.getProjectsDir() + File.separator + project.getName() + File.separator + ROLES_DIR_NAME);
		}
		return rolesDir;
	}
	
	public static File getRoleFile(Project project, String role) {
		return new File(getRolesDir(project), "role_" + role + ".pl");
	}
	
	@SuppressWarnings("unused")
	private static void printRbacMap() {
		for (String proj: rbacMap.keySet()) {
			System.out.println("Project: " + proj);
			for (String role: rbacMap.get(proj).keySet()) {
				System.out.print("\tRole: " + role);
				RBACProcessor rbac = rbacMap.get(proj).get(role);
				System.out.println("; RBACProcessor " + rbac);
			}
		}
	}
	
}
